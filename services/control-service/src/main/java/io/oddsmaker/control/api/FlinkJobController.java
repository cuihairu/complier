package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.FlinkJobEntity;
import io.oddsmaker.control.jpa.RiskRuleEntity;
import io.oddsmaker.control.service.FlinkJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Flink作业API控制器
 * 提供Flink作业管理的API接口
 */
@RestController
@RequestMapping("/api/flink-jobs")
public class FlinkJobController {

    @Autowired
    private FlinkJobService flinkJobService;

    /**
     * 创建Flink作业
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #request.gameId)")
    public ResponseEntity<FlinkJobEntity> createJob(@RequestBody FlinkJobRequest request) {
        FlinkJobEntity job = flinkJobService.createJob(
            request.gameId,
            request.environmentId,
            request.name,
            request.displayName,
            request.description,
            request.jobType,
            request.jobConfig,
            request.sourceConfig,
            request.sinkConfig,
            request.ruleIds,
            request.parallelism,
            request.createdBy
        );
        return ResponseEntity.ok(job);
    }

    /**
     * 获取作业详情
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<FlinkJobEntity> getJob(@PathVariable String jobId, @RequestParam String gameId) {
        FlinkJobEntity job = flinkJobService.getJob(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * 获取游戏的作业列表
     */
    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<FlinkJobEntity>> getGameJobs(@PathVariable String gameId) {
        List<FlinkJobEntity> jobs = flinkJobService.getGameJobs(gameId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * 获取运行中的作业
     */
    @GetMapping("/running/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<FlinkJobEntity>> getRunningJobs(@PathVariable String gameId) {
        List<FlinkJobEntity> jobs = flinkJobService.getRunningJobs(gameId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * 部署作业
     */
    @PostMapping("/{jobId}/deploy")
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #gameId)")
    public ResponseEntity<FlinkJobEntity> deployJob(
            @PathVariable String jobId,
            @RequestParam String gameId,
            @RequestBody DeployRequest request) {
        FlinkJobEntity job = flinkJobService.deployJob(jobId, request.deployedBy);
        return ResponseEntity.ok(job);
    }

    /**
     * 停止作业
     */
    @PostMapping("/{jobId}/stop")
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #gameId)")
    public ResponseEntity<FlinkJobEntity> stopJob(
            @PathVariable String jobId,
            @RequestParam String gameId,
            @RequestBody StopRequest request) {
        FlinkJobEntity job = flinkJobService.stopJob(jobId, request.stoppedBy);
        return ResponseEntity.ok(job);
    }

    /**
     * 获取作业统计
     */
    @GetMapping("/stats/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getJobStats(@PathVariable String gameId) {
        Map<String, Object> stats = flinkJobService.getJobStats(gameId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取作业配置
     */
    @GetMapping("/{jobId}/config")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getJobConfig(
            @PathVariable String jobId,
            @RequestParam String gameId) {
        Map<String, Object> config = flinkJobService.getJobConfig(jobId);
        return ResponseEntity.ok(config);
    }

    /**
     * 获取作业的关联规则
     */
    @GetMapping("/{jobId}/rules")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<RiskRuleEntity>> getJobRules(
            @PathVariable String jobId,
            @RequestParam String gameId) {
        List<RiskRuleEntity> rules = flinkJobService.getJobRules(jobId);
        return ResponseEntity.ok(rules);
    }

    /**
     * 更新作业指标
     */
    @PostMapping("/{jobId}/metrics")
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #gameId)")
    public ResponseEntity<Void> updateMetrics(
            @PathVariable String jobId,
            @RequestParam String gameId,
            @RequestBody MetricsUpdateRequest request) {
        flinkJobService.updateJobMetrics(jobId, request.eventsProcessed, request.casesCreated, request.actionsExecuted);
        return ResponseEntity.ok().build();
    }

    // Request DTOs

    public static class FlinkJobRequest {
        public String gameId;
        public String environmentId;
        public String name;
        public String displayName;
        public String description;
        public String jobType;
        public Map<String, Object> jobConfig;
        public Map<String, Object> sourceConfig;
        public Map<String, Object> sinkConfig;
        public List<String> ruleIds;
        public Integer parallelism;
        public String createdBy;
    }

    public static class DeployRequest {
        public String deployedBy;
    }

    public static class StopRequest {
        public String stoppedBy;
    }

    public static class MetricsUpdateRequest {
        public Long eventsProcessed;
        public Long casesCreated;
        public Long actionsExecuted;
    }
}
