package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Flink作业管理服务
 * 管理实时风险控制作业的生命周期
 */
@Service
@Transactional
public class FlinkJobService {

    private static final Logger logger = LoggerFactory.getLogger(FlinkJobService.class);

    @Autowired
    private FlinkJobRepo flinkJobRepo;

    @Autowired
    private RiskRuleRepo riskRuleRepo;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${oddsmaker.flink.rest.url:http://localhost:8081}")
    private String flinkRestUrl;

    /**
     * 创建Flink作业
     */
    public FlinkJobEntity createJob(String gameId, String environmentId, String name, String displayName,
                                    String description, String jobType, Map<String, Object> jobConfig,
                                    Map<String, Object> sourceConfig, Map<String, Object> sinkConfig,
                                    List<String> ruleIds, Integer parallelism, String createdBy) {

        // 检查名称唯一性
        var existing = flinkJobRepo.findByGameIdAndName(gameId, name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Job name already exists: " + name);
        }

        FlinkJobEntity job = new FlinkJobEntity();
        job.id = "fj_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        job.gameId = gameId;
        job.environmentId = environmentId;
        job.name = name;
        job.displayName = displayName;
        job.description = description;
        job.jobType = jobType != null ? jobType : FlinkJobEntity.JobType.RISK_EVALUATION.name();
        job.parallelism = parallelism != null ? parallelism : 1;
        job.status = FlinkJobEntity.JobStatus.DRAFT;
        job.createdBy = createdBy;

        try {
            if (jobConfig != null) {
                job.jobConfig = objectMapper.writeValueAsString(jobConfig);
            }
            if (sourceConfig != null) {
                job.sourceConfig = objectMapper.writeValueAsString(sourceConfig);
            }
            if (sinkConfig != null) {
                job.sinkConfig = objectMapper.writeValueAsString(sinkConfig);
            }
            if (ruleIds != null && !ruleIds.isEmpty()) {
                job.ruleIds = objectMapper.writeValueAsString(ruleIds);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize job config", e);
            throw new RuntimeException("Failed to serialize job config", e);
        }

        job = flinkJobRepo.save(job);

        // 记录审计日志
        auditLogService.logCreate("flink_job", job.id, name, createdBy, null, null,
            Map.of("gameId", gameId, "jobType", job.jobType));

        logger.info("Created Flink job: {} for game: {}", name, gameId);
        return job;
    }

    /**
     * 部署作业到Flink集群
     */
    public FlinkJobEntity deployJob(String jobId, String deployedBy) {
        FlinkJobEntity job = flinkJobRepo.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (!job.canDeploy()) {
            throw new IllegalStateException("Job cannot be deployed in current status: " + job.status);
        }

        try {
            // 更新状态为部署中
            job.status = FlinkJobEntity.JobStatus.DEPLOYING;
            job.updatedBy = deployedBy;
            flinkJobRepo.save(job);

            // 构建Flink作业配置
            String jarPath = buildJobJarPath(job);
            String programArgs = buildProgramArgs(job);
            String parallelism = job.parallelism != null ? job.parallelism.toString() : "1";

            // 调用Flink REST API提交作业
            String flinkJobId = submitToFlinkCluster(jarPath, programArgs, parallelism);
            String flinkUrl = flinkRestUrl + "/#/jobs/" + flinkJobId;

            // 更新作业状态
            job.markAsDeployed(flinkJobId, flinkUrl);
            job.updatedBy = deployedBy;
            flinkJobRepo.save(job);

            // 记录审计日志
            auditLogService.log(
                AuditLogEntity.AuditAction.ACTIVATE,
                "flink_job",
                job.id,
                job.name,
                "Deployed Flink job: " + flinkJobId,
                AuditLogEntity.AuditResult.SUCCESS,
                deployedBy,
                null,
                null,
                null,
                null,
                Map.of("flinkJobId", flinkJobId, "flinkUrl", flinkUrl)
            );

            logger.info("Deployed Flink job {} with Flink Job ID: {}", job.name, flinkJobId);
            return job;

        } catch (Exception e) {
            job.markAsFailed(e.getMessage());
            flinkJobRepo.save(job);
            logger.error("Failed to deploy Flink job {}: {}", job.name, e.getMessage(), e);
            throw new RuntimeException("Failed to deploy job", e);
        }
    }

    /**
     * 停止作业
     */
    public FlinkJobEntity stopJob(String jobId, String stoppedBy) {
        FlinkJobEntity job = flinkJobRepo.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (!job.canStop()) {
            throw new IllegalStateException("Job cannot be stopped in current status: " + job.status);
        }

        try {
            // 调用Flink REST API停止作业
            if (job.flinkJobId != null) {
                stopFlinkJob(job.flinkJobId);
            }

            job.markAsStopped();
            job.updatedBy = stoppedBy;
            flinkJobRepo.save(job);

            // 记录审计日志
            auditLogService.log(
                AuditLogEntity.AuditAction.DEACTIVATE,
                "flink_job",
                job.id,
                job.name,
                "Stopped Flink job",
                AuditLogEntity.AuditResult.SUCCESS,
                stoppedBy,
                null,
                null,
                null,
                null,
                Map.of("flinkJobId", job.flinkJobId)
            );

            logger.info("Stopped Flink job: {}", job.name);
            return job;

        } catch (Exception e) {
            logger.error("Failed to stop Flink job {}: {}", job.name, e.getMessage(), e);
            throw new RuntimeException("Failed to stop job", e);
        }
    }

    /**
     * 更新作业指标
     */
    public void updateJobMetrics(String jobId, Long eventsProcessed, Long casesCreated, Long actionsExecuted) {
        FlinkJobEntity job = flinkJobRepo.findById(jobId).orElse(null);
        if (job == null) {
            logger.warn("Job not found for metrics update: {}", jobId);
            return;
        }

        job.updateMetrics(eventsProcessed, casesCreated, actionsExecuted);
        flinkJobRepo.save(job);
    }

    /**
     * 获取作业详情
     */
    @Transactional(readOnly = true)
    public FlinkJobEntity getJob(String jobId) {
        return flinkJobRepo.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }

    /**
     * 获取游戏的作业列表
     */
    @Transactional(readOnly = true)
    public List<FlinkJobEntity> getGameJobs(String gameId) {
        return flinkJobRepo.findByGameId(gameId);
    }

    /**
     * 获取运行中的作业
     */
    @Transactional(readOnly = true)
    public List<FlinkJobEntity> getRunningJobs(String gameId) {
        return flinkJobRepo.findRunningJobs(gameId);
    }

    /**
     * 获取作业统计信息
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getJobStats(String gameId) {
        List<FlinkJobEntity> allJobs = flinkJobRepo.findByGameId(gameId);
        List<FlinkJobEntity> runningJobs = flinkJobRepo.findRunningJobs(gameId);

        long totalEventsProcessed = allJobs.stream()
            .mapToLong(j -> j.totalEventsProcessed != null ? j.totalEventsProcessed : 0)
            .sum();

        long totalRiskCasesCreated = allJobs.stream()
            .mapToLong(j -> j.totalRiskCasesCreated != null ? j.totalRiskCasesCreated : 0)
            .sum();

        return Map.of(
            "totalJobs", allJobs.size(),
            "runningJobs", runningJobs.size(),
            "stoppedJobs", allJobs.stream().filter(j -> j.isStopped()).count(),
            "failedJobs", allJobs.stream().filter(j -> j.status == FlinkJobEntity.JobStatus.FAILED).count(),
            "totalEventsProcessed", totalEventsProcessed,
            "totalRiskCasesCreated", totalRiskCasesCreated,
            "overallRiskCaseRate", totalEventsProcessed > 0 ? (double) totalRiskCasesCreated / totalEventsProcessed : 0.0
        );
    }

    /**
     * 获取作业配置（用于生成Flink作业）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getJobConfig(String jobId) {
        FlinkJobEntity job = flinkJobRepo.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        Map<String, Object> config = new HashMap<>();
        config.put("jobId", job.id);
        config.put("gameId", job.gameId);
        config.put("environmentId", job.environmentId);
        config.put("jobType", job.jobType);
        config.put("parallelism", job.parallelism);

        try {
            if (job.jobConfig != null) {
                config.put("jobConfig", objectMapper.readValue(job.jobConfig, Map.class));
            }
            if (job.sourceConfig != null) {
                config.put("sourceConfig", objectMapper.readValue(job.sourceConfig, Map.class));
            }
            if (job.sinkConfig != null) {
                config.put("sinkConfig", objectMapper.readValue(job.sinkConfig, Map.class));
            }
            if (job.ruleIds != null) {
                config.put("ruleIds", objectMapper.readValue(job.ruleIds, List.class));
            }
        } catch (Exception e) {
            logger.error("Failed to parse job config", e);
        }

        return config;
    }

    // 私有辅助方法

    private String buildJobJarPath(FlinkJobEntity job) {
        // 返回作业JAR路径
        return "oddsmaker-risk-evaluation.jar";
    }

    private String buildProgramArgs(FlinkJobEntity job) {
        // 构建程序参数
        List<String> args = new ArrayList<>();
        args.add("--job-id=" + job.id);
        args.add("--game-id=" + job.gameId);
        if (job.environmentId != null) {
            args.add("--environment-id=" + job.environmentId);
        }
        args.add("--job-type=" + job.jobType);

        // 添加规则ID
        if (job.ruleIds != null) {
            try {
                List<String> rules = objectMapper.readValue(job.ruleIds, List.class);
                if (!rules.isEmpty()) {
                    args.add("--rule-ids=" + String.join(",", rules));
                }
            } catch (Exception e) {
                logger.warn("Failed to parse rule IDs", e);
            }
        }

        return String.join(" ", args);
    }

    private String submitToFlinkCluster(String jarPath, String programArgs, String parallelism) throws Exception {
        // TODO: 实现实际的Flink REST API调用
        // 这里简化实现，返回模拟的作业ID
        String simulatedJobId = "flink_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        logger.info("Submitting job to Flink cluster: jar={}, args={}, parallelism={}", jarPath, programArgs, parallelism);
        return simulatedJobId;
    }

    private void stopFlinkJob(String flinkJobId) throws Exception {
        // TODO: 实现实际的Flink REST API调用
        logger.info("Stopping Flink job: {}", flinkJobId);
    }

    /**
     * 获取作业的关联规则
     */
    @Transactional(readOnly = true)
    public List<RiskRuleEntity> getJobRules(String jobId) {
        FlinkJobEntity job = flinkJobRepo.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.ruleIds == null) {
            return List.of();
        }

        try {
            List<String> ruleIds = objectMapper.readValue(job.ruleIds, List.class);
            return ruleIds.stream()
                .map(ruleId -> riskRuleRepo.findById(ruleId).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to parse rule IDs", e);
            return List.of();
        }
    }
}
