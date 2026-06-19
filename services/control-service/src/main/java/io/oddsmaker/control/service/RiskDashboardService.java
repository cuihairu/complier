package io.oddsmaker.control.service;

import io.oddsmaker.control.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 风控仪表盘服务
 * 提供风控数据的统计和可视化支持
 */
@Service
@Transactional(readOnly = true)
public class RiskDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(RiskDashboardService.class);

    @Autowired
    private RiskCaseRepo riskCaseRepo;

    @Autowired
    private RiskRuleRepo riskRuleRepo;

    @Autowired
    private BlockListRepo blockListRepo;

    @Autowired
    private FlinkJobRepo flinkJobRepo;

    /**
     * 获取游戏的风控概览
     */
    public Map<String, Object> getOverview(String gameId, LocalDateTime since) {
        LocalDateTime now = LocalDateTime.now();
        if (since == null) {
            since = now.minusDays(7);  // 默认7天
        }

        // 统计数据
        long totalCases = riskCaseRepo.countByGameIdSince(gameId, since);
        List<RiskCaseEntity> recentCases = riskCaseRepo.findByGameId(gameId);

        // 按风险等级分组
        Map<String, Long> byRiskLevel = recentCases.stream()
            .collect(Collectors.groupingBy(c -> c.riskLevel.name(), Collectors.counting()));

        // 按状态分组
        Map<String, Long> byStatus = recentCases.stream()
            .collect(Collectors.groupingBy(c -> c.executionStatus.name(), Collectors.counting()));

        // 按动作类型分组
        Map<String, Long> byActionType = recentCases.stream()
            .collect(Collectors.groupingBy(c -> c.actionTaken.name(), Collectors.counting()));

        // 活跃封禁数
        List<BlockListEntity> activeBlocks = blockListRepo.findActiveBlocks(gameId, now);

        // 运行中的作业数
        List<FlinkJobEntity> runningJobs = flinkJobRepo.findRunningJobs(gameId);

        // 高风险案例数
        long highRiskCases = recentCases.stream()
            .filter(c -> c.isHighRisk())
            .count();

        // 待审核案例数
        long pendingReview = recentCases.stream()
            .filter(c -> c.needsReview() && c.reviewStatus == null)
            .count();

        return Map.of(
            "period", Map.of("since", since, "now", now),
            "totalCases", totalCases,
            "highRiskCases", highRiskCases,
            "pendingReview", pendingReview,
            "activeBlocks", activeBlocks.size(),
            "runningJobs", runningJobs.size(),
            "byRiskLevel", byRiskLevel,
            "byStatus", byStatus,
            "byActionType", byActionType
        );
    }

    /**
     * 获取风险趋势
     */
    public List<Map<String, Object>> getRiskTrends(String gameId, LocalDateTime since, int intervalHours) {
        if (since == null) {
            since = LocalDateTime.now().minusDays(7);
        }
        if (intervalHours <= 0) {
            intervalHours = 24;  // 默认24小时间隔
        }

        List<Map<String, Object>> trends = new ArrayList<>();
        LocalDateTime current = since;
        LocalDateTime now = LocalDateTime.now();

        while (current.isBefore(now)) {
            LocalDateTime next = current.plusHours(intervalHours);

            // 获取该时间段内的案例
            List<RiskCaseEntity> cases = riskCaseRepo.findByGameIdAndTimeRange(gameId, current, next);

            long total = cases.size();
            long highRisk = cases.stream().filter(c -> c.isHighRisk()).count();
            long blocked = cases.stream().filter(c -> c.actionTaken == RiskCaseEntity.ActionType.BLOCK).count();
            long reviewed = cases.stream().filter(c -> c.reviewStatus != null).count();

            trends.add(Map.of(
                "periodStart", current,
                "periodEnd", next,
                "totalCases", total,
                "highRiskCases", highRisk,
                "blockedCases", blocked,
                "reviewedCases", reviewed
            ));

            current = next;
        }

        return trends;
    }

    /**
     * 获取高风险目标
     */
    public List<Map<String, Object>> getHighRiskTargets(String gameId, LocalDateTime since, int limit) {
        if (since == null) {
            since = LocalDateTime.now().minusDays(7);
        }
        if (limit <= 0) {
            limit = 10;
        }

        List<Object[]> results = riskCaseRepo.findFrequentTargets(since, 2);

        return results.stream()
            .limit(limit)
            .map(row -> Map.of(
                "targetType", row[0],
                "targetId", row[1],
                "caseCount", row[2]
            ))
            .collect(Collectors.toList());
    }

    /**
     * 获取规则性能统计
     */
    public List<Map<String, Object>> getRulePerformance(String gameId) {
        List<RiskRuleEntity> rules = riskRuleRepo.findActiveByGameId(gameId);

        return rules.stream()
            .map(rule -> {
                long triggerCount = rule.totalTriggeredCount != null ? rule.totalTriggeredCount : 0;
                long blockCount = rule.totalBlockedCount != null ? rule.totalBlockedCount : 0;

                // 计算转化率
                double blockRate = triggerCount > 0 ? (double) blockCount / triggerCount : 0.0;

                // 获取最近的案例
                List<RiskCaseEntity> recentCases = riskCaseRepo.findByRiskRuleId(rule.id);

                Map<String, Object> result = new java.util.HashMap<>();
                result.put("ruleId", rule.id);
                result.put("ruleName", rule.displayName != null ? rule.displayName : rule.name);
                result.put("category", rule.category.name());
                result.put("riskLevel", rule.riskLevel.name());
                result.put("actionType", rule.actionType.name());
                result.put("triggerCount", triggerCount);
                result.put("blockCount", blockCount);
                result.put("blockRate", blockRate);
                result.put("recentCases", recentCases.size());
                result.put("lastTriggered", rule.lastTriggeredAt);
                return result;
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取封禁统计
     */
    public Map<String, Object> getBlockStats(String gameId) {
        LocalDateTime now = LocalDateTime.now();
        List<BlockListEntity> activeBlocks = blockListRepo.findActiveBlocks(gameId, now);

        // 按类型分组
        Map<String, Long> byType = activeBlocks.stream()
            .collect(Collectors.groupingBy(b -> b.targetType, Collectors.counting()));

        // 按分类分组
        Map<String, Long> byCategory = activeBlocks.stream()
            .collect(Collectors.groupingBy(b -> b.blockCategory != null ? b.blockCategory : "unknown", Collectors.counting()));

        // 按封禁类型分组
        Map<String, Long> byBlockType = activeBlocks.stream()
            .collect(Collectors.groupingBy(b -> b.blockType.name(), Collectors.counting()));

        // 永久封禁 vs 临时封禁
        long permanentBlocks = activeBlocks.stream()
            .filter(b -> Boolean.TRUE.equals(b.isPermanent))
            .count();

        long temporaryBlocks = activeBlocks.size() - permanentBlocks;

        // 命中统计
        long totalHits = activeBlocks.stream()
            .mapToLong(b -> b.hitCount != null ? b.hitCount : 0)
            .sum();

        return Map.of(
            "totalActive", activeBlocks.size(),
            "permanentBlocks", permanentBlocks,
            "temporaryBlocks", temporaryBlocks,
            "totalHits", totalHits,
            "byType", byType,
            "byCategory", byCategory,
            "byBlockType", byBlockType
        );
    }

    /**
     * 获取作业统计
     */
    public Map<String, Object> getJobStats(String gameId) {
        List<FlinkJobEntity> allJobs = flinkJobRepo.findByGameId(gameId);
        List<FlinkJobEntity> runningJobs = flinkJobRepo.findRunningJobs(gameId);

        long totalEventsProcessed = allJobs.stream()
            .mapToLong(j -> j.totalEventsProcessed != null ? j.totalEventsProcessed : 0)
            .sum();

        long totalRiskCasesCreated = allJobs.stream()
            .mapToLong(j -> j.totalRiskCasesCreated != null ? j.totalRiskCasesCreated : 0)
            .sum();

        long totalActionsExecuted = allJobs.stream()
            .mapToLong(j -> j.totalActionsExecuted != null ? j.totalActionsExecuted : 0)
            .sum();

        // 按状态分组
        Map<String, Long> byStatus = allJobs.stream()
            .collect(Collectors.groupingBy(j -> j.status.name(), Collectors.counting()));

        // 按类型分组
        Map<String, Long> byType = allJobs.stream()
            .collect(Collectors.groupingBy(j -> j.jobType, Collectors.counting()));

        return Map.of(
            "totalJobs", allJobs.size(),
            "runningJobs", runningJobs.size(),
            "stoppedJobs", allJobs.stream().filter(j -> j.isStopped()).count(),
            "failedJobs", allJobs.stream().filter(j -> j.status == FlinkJobEntity.JobStatus.FAILED).count(),
            "totalEventsProcessed", totalEventsProcessed,
            "totalRiskCasesCreated", totalRiskCasesCreated,
            "totalActionsExecuted", totalActionsExecuted,
            "byStatus", byStatus,
            "byType", byType
        );
    }

    /**
     * 获取完整仪表盘数据
     */
    public Map<String, Object> getDashboard(String gameId, LocalDateTime since) {
        return Map.of(
            "overview", getOverview(gameId, since),
            "trends", getRiskTrends(gameId, since, 24),
            "highRiskTargets", getHighRiskTargets(gameId, since, 10),
            "rulePerformance", getRulePerformance(gameId),
            "blockStats", getBlockStats(gameId),
            "jobStats", getJobStats(gameId)
        );
    }

    /**
     * 获取最近的风险案例
     */
    public List<Map<String, Object>> getRecentCases(String gameId, int limit) {
        List<RiskCaseEntity> cases = riskCaseRepo.findByGameId(gameId);

        return cases.stream()
            .limit(limit)
            .map(c -> Map.of(
                "caseId", c.id,
                "caseNumber", c.caseNumber,
                "targetType", c.targetType,
                "targetId", c.targetId,
                "riskLevel", c.riskLevel.name(),
                "actionTaken", c.actionTaken.name(),
                "status", c.executionStatus.name(),
                "createdAt", c.createdAt,
                "reviewStatus", c.reviewStatus != null ? c.reviewStatus : "none"
            ))
            .collect(Collectors.toList());
    }

    /**
     * 获取审核队列统计
     */
    public Map<String, Object> getReviewQueueStats(String gameId) {
        List<RiskCaseEntity> pendingReview = riskCaseRepo.findPendingReview(gameId);

        long total = pendingReview.size();
        long highPriority = pendingReview.stream()
            .filter(c -> c.isHighRisk())
            .count();

        // 按风险等级分组
        Map<String, Long> byRiskLevel = pendingReview.stream()
            .collect(Collectors.groupingBy(c -> c.riskLevel.name(), Collectors.counting()));

        // 平均等待时间
        double avgWaitMinutes = pendingReview.stream()
            .filter(c -> c.createdAt != null)
            .mapToLong(c -> ChronoUnit.MINUTES.between(c.createdAt, LocalDateTime.now()))
            .average()
            .orElse(0.0);

        return Map.of(
            "totalPending", total,
            "highPriority", highPriority,
            "byRiskLevel", byRiskLevel,
            "avgWaitMinutes", avgWaitMinutes
        );
    }
}
