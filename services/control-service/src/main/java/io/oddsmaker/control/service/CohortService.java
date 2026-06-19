package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同期群分析服务
 * 计算和管理用户同期群分析
 */
@Service
@Transactional
public class CohortService {

    private static final Logger logger = LoggerFactory.getLogger(CohortService.class);

    @Autowired
    private CohortRepo cohortRepo;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建同期群
     */
    public CohortEntity createCohort(String gameId, String environmentId, String name, String displayName,
                                    String description, CohortEntity.CohortType cohortType,
                                    LocalDate startDate, LocalDate endDate, String timeUnit,
                                    String analysisType, String metricType,
                                    Map<String, Object> behaviorDefinition,
                                    List<Integer> retentionPeriods, String createdBy) {

        // 检查名称唯一性
        var existing = cohortRepo.findByGameIdAndName(gameId, name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Cohort name already exists: " + name);
        }

        CohortEntity cohort = new CohortEntity();
        cohort.id = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        cohort.gameId = gameId;
        cohort.environmentId = environmentId;
        cohort.name = name;
        cohort.displayName = displayName;
        cohort.description = description;
        cohort.cohortType = cohortType != null ? cohortType : CohortEntity.CohortType.ACQUISITION;
        cohort.startDate = startDate;
        cohort.endDate = endDate;
        cohort.timeUnit = timeUnit != null ? timeUnit : "day";
        cohort.analysisType = analysisType != null ? analysisType : "retention";
        cohort.metricType = metricType != null ? metricType : "return_rate";
        cohort.status = CohortEntity.CohortStatus.PENDING;
        cohort.createdBy = createdBy;

        try {
            if (behaviorDefinition != null) {
                cohort.behaviorDefinition = objectMapper.writeValueAsString(behaviorDefinition);
            }
            if (retentionPeriods != null && !retentionPeriods.isEmpty()) {
                cohort.retentionPeriods = objectMapper.writeValueAsString(retentionPeriods);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize cohort config", e);
            throw new RuntimeException("Failed to serialize cohort config", e);
        }

        cohort = cohortRepo.save(cohort);

        // 记录审计日志
        auditLogService.logCreate("cohort", cohort.id, name, createdBy, null, null,
            Map.of("gameId", gameId, "cohortType", cohort.cohortType.name()));

        logger.info("Created cohort: {} for game: {}", name, gameId);
        return cohort;
    }

    /**
     * 计算同期群
     */
    public CohortEntity calculateCohort(String cohortId) {
        CohortEntity cohort = cohortRepo.findById(cohortId)
            .orElseThrow(() -> new IllegalArgumentException("Cohort not found: " + cohortId));

        if (!cohort.isPending()) {
            throw new IllegalStateException("Cohort is not in PENDING status: " + cohort.status);
        }

        cohort.markAsCalculating();
        cohortRepo.save(cohort);

        try {
            LocalDateTime startTime = LocalDateTime.now();

            // 模拟计算同期群数据
            Map<String, Object> resultData = simulateCohortCalculation(cohort);

            long cohortCount = (Long) resultData.getOrDefault("cohortCount", 0L);
            String resultSummary = buildResultSummary(resultData);

            long calculationTime = java.time.temporal.ChronoUnit.MILLIS.between(startTime, LocalDateTime.now());

            cohort.markAsCompleted(
                cohortCount,
                objectMapper.writeValueAsString(resultData),
                resultSummary,
                calculationTime
            );
            cohortRepo.save(cohort);

            logger.info("Calculated cohort: {} with {} users, took {}ms", cohort.name, cohortCount, calculationTime);

        } catch (Exception e) {
            cohort.markAsFailed();
            cohortRepo.save(cohort);
            logger.error("Failed to calculate cohort: {} - {}", cohort.name, e.getMessage(), e);
            throw new RuntimeException("Failed to calculate cohort", e);
        }

        return cohort;
    }

    /**
     * 获取同期群详情
     */
    @Transactional(readOnly = true)
    public CohortEntity getCohort(String cohortId) {
        return cohortRepo.findById(cohortId)
            .orElseThrow(() -> new IllegalArgumentException("Cohort not found: " + cohortId));
    }

    /**
     * 获取游戏的同期群列表
     */
    @Transactional(readOnly = true)
    public List<CohortEntity> getGameCohorts(String gameId) {
        return cohortRepo.findByGameId(gameId);
    }

    /**
     * 获取已完成的同期群
     */
    @Transactional(readOnly = true)
    public List<CohortEntity> getCompletedCohorts(String gameId) {
        return cohortRepo.findCompletedByGameId(gameId);
    }

    /**
     * 获取同期群结果
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCohortResults(String cohortId) {
        CohortEntity cohort = getCohort(cohortId);

        if (!cohort.hasResults()) {
            return Map.of("status", "no_results", "cohortId", cohortId);
        }

        try {
            return objectMapper.readValue(cohort.resultData, Map.class);
        } catch (Exception e) {
            logger.error("Failed to parse cohort results", e);
            return Map.of("status", "parse_error", "cohortId", cohortId);
        }
    }

    /**
     * 获取同期群统计
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCohortStats(String gameId) {
        List<CohortEntity> allCohorts = cohortRepo.findByGameId(gameId);
        List<CohortEntity> completedCohorts = cohortRepo.findCompletedByGameId(gameId);

        long totalUsers = completedCohorts.stream()
            .mapToLong(c -> c.cohortCount != null ? c.cohortCount : 0)
            .sum();

        Map<String, Long> byType = allCohorts.stream()
            .collect(Collectors.groupingBy(c -> c.cohortType.name(), Collectors.counting()));

        Map<String, Long> byStatus = allCohorts.stream()
            .collect(Collectors.groupingBy(c -> c.status.name(), Collectors.counting()));

        Map<String, Long> byAnalysisType = allCohorts.stream()
            .filter(c -> c.analysisType != null)
            .collect(Collectors.groupingBy(c -> c.analysisType, Collectors.counting()));

        return Map.of(
            "totalCohorts", allCohorts.size(),
            "completedCohorts", completedCohorts.size(),
            "pendingCohorts", allCohorts.stream().filter(CohortEntity::isPending).count(),
            "totalUsersAnalyzed", totalUsers,
            "byType", byType,
            "byStatus", byStatus,
            "byAnalysisType", byAnalysisType
        );
    }

    /**
     * 定期处理待计算的同期群
     */
    @Scheduled(fixedDelay = 300000)  // 每5分钟执行一次
    public void processPendingCohorts() {
        try {
            List<CohortEntity> pendingCohorts = cohortRepo.findPending();

            for (CohortEntity cohort : pendingCohorts) {
                try {
                    calculateCohort(cohort.id);
                } catch (Exception e) {
                    logger.error("Failed to calculate cohort {}: {}", cohort.id, e.getMessage());
                }
            }

            if (!pendingCohorts.isEmpty()) {
                logger.info("Processed {} pending cohorts", pendingCohorts.size());
            }
        } catch (Exception e) {
            logger.error("Failed to process pending cohorts", e);
        }
    }

    // 私有辅助方法

    private Map<String, Object> simulateCohortCalculation(CohortEntity cohort) {
        // 模拟同期群计算
        long cohortCount = (long) (Math.random() * 10000) + 1000;

        List<Integer> periods = cohort.getRetentionPeriods();
        Map<String, Object> retentionData = new HashMap<>();

        for (Integer period : periods) {
            double retentionRate = 1.0 - (period * 0.05);  // 模拟留存率衰减
            retentionRate = Math.max(0.0, retentionRate);

            retentionData.put("day_" + period, Map.of(
                "count", (long) (cohortCount * retentionRate),
                "rate", retentionRate
            ));
        }

        return Map.of(
            "cohortId", cohort.id,
            "cohortName", cohort.name,
            "cohortCount", cohortCount,
            "cohortType", cohort.cohortType.name(),
            "retention", retentionData,
            "calculatedAt", LocalDateTime.now().toString()
        );
    }

    private String buildResultSummary(Map<String, Object> resultData) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "cohortCount", resultData.get("cohortCount"),
                "periods", ((Map) resultData.get("retention")).size()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 搜索同期群
     */
    @Transactional(readOnly = true)
    public List<CohortEntity> searchCohorts(String gameId, String query) {
        return cohortRepo.search(gameId, query);
    }

    /**
     * 获取最近的同期群
     */
    @Transactional(readOnly = true)
    public List<CohortEntity> getRecentCohorts(String gameId) {
        return cohortRepo.findRecent(gameId).stream()
            .limit(10)
            .collect(Collectors.toList());
    }
}
