package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表服务
 * 管理自定义报表的创建、执行和调度
 */
@Service
@Transactional
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private ReportRepo reportRepo;

    @Autowired
    private ReportExecutionRepo executionRepo;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建报表
     */
    public ReportEntity createReport(String gameId, String environmentId, String name, String displayName,
                                     String description, ReportEntity.ReportType reportType, String reportCategory,
                                     Map<String, Object> queryConfig, Map<String, Object> visualization,
                                     String chartType, String createdBy) {

        // 检查名称唯一性
        var existing = reportRepo.findByGameIdAndName(gameId, name);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Report name already exists: " + name);
        }

        ReportEntity report = new ReportEntity();
        report.id = "rpt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        report.gameId = gameId;
        report.environmentId = environmentId;
        report.name = name;
        report.displayName = displayName;
        report.description = description;
        report.reportType = reportType != null ? reportType : ReportEntity.ReportType.CUSTOM;
        report.reportCategory = reportCategory;
        report.chartType = chartType;
        report.status = ReportEntity.ReportStatus.DRAFT;
        report.createdBy = createdBy;

        try {
            if (queryConfig != null) {
                report.queryConfig = objectMapper.writeValueAsString(queryConfig);
            }
            if (visualization != null) {
                report.visualization = objectMapper.writeValueAsString(visualization);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize report config", e);
            throw new RuntimeException("Failed to serialize report config", e);
        }

        report = reportRepo.save(report);

        // 记录审计日志
        auditLogService.logCreate("report", report.id, name, createdBy, null, null,
            Map.of("gameId", gameId, "reportType", report.reportType.name()));

        logger.info("Created report: {} for game: {}", name, gameId);
        return report;
    }

    /**
     * 发布报表
     */
    public ReportEntity publishReport(String reportId, String publishedBy) {
        ReportEntity report = reportRepo.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.markAsPublished();
        report.updatedBy = publishedBy;
        report = reportRepo.save(report);

        // 记录审计日志
        auditLogService.log(
            AuditLogEntity.AuditAction.ACTIVATE,
            "report",
            report.id,
            report.name,
            "Published report",
            AuditLogEntity.AuditResult.SUCCESS,
            publishedBy,
            null,
            null,
            null,
            null,
            Map.of("gameId", report.gameId)
        );

        logger.info("Published report: {}", report.name);
        return report;
    }

    /**
     * 执行报表
     */
    public ReportExecutionEntity executeReport(String reportId, String triggeredBy, String triggerType,
                                             Map<String, Object> parameters, Map<String, Object> filters) {
        ReportEntity report = reportRepo.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        // 创建执行记录
        ReportExecutionEntity execution = new ReportExecutionEntity();
        execution.id = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        execution.reportId = reportId;
        execution.gameId = report.gameId;
        execution.triggeredBy = triggeredBy;
        execution.triggerType = triggerType != null ? triggerType : "manual";
        execution.executionStatus = ReportExecutionEntity.ExecutionStatus.PENDING;

        try {
            if (parameters != null) {
                execution.parameters = objectMapper.writeValueAsString(parameters);
            }
            if (filters != null) {
                execution.filters = objectMapper.writeValueAsString(filters);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize execution parameters", e);
        }

        execution = executionRepo.save(execution);

        // 异步执行报表
        executeReportAsync(report, execution);

        return execution;
    }

    /**
     * 获取报表详情
     */
    @Transactional(readOnly = true)
    public ReportEntity getReport(String reportId) {
        return reportRepo.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    }

    /**
     * 获取游戏的报表列表
     */
    @Transactional(readOnly = true)
    public List<ReportEntity> getGameReports(String gameId) {
        return reportRepo.findByGameId(gameId);
    }

    /**
     * 获取已发布的报表
     */
    @Transactional(readOnly = true)
    public List<ReportEntity> getPublishedReports(String gameId) {
        return reportRepo.findPublishedByGameId(gameId);
    }

    /**
     * 获取报表执行历史
     */
    @Transactional(readOnly = true)
    public List<ReportExecutionEntity> getReportExecutions(String reportId) {
        return executionRepo.findByReportId(reportId);
    }

    /**
     * 获取报表统计
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getReportStats(String reportId) {
        long totalRuns = executionRepo.countByReportId(reportId);
        long successRuns = executionRepo.countSuccessByReportId(reportId);
        Double avgExecutionTime = executionRepo.averageExecutionTime(reportId);
        Long totalRows = executionRepo.sumRowCountByReportId(reportId);

        List<ReportExecutionEntity> recentExecutions = executionRepo.findByReportId(reportId);

        Map<String, Long> byStatus = recentExecutions.stream()
            .collect(Collectors.groupingBy(e -> e.executionStatus.name(), Collectors.counting()));

        Map<String, Long> byTriggerType = recentExecutions.stream()
            .collect(Collectors.groupingBy(e -> e.triggerType != null ? e.triggerType : "unknown", Collectors.counting()));

        return Map.of(
            "totalRuns", totalRuns,
            "successRuns", successRuns,
            "failedRuns", totalRuns - successRuns,
            "successRate", totalRuns > 0 ? (double) successRuns / totalRuns : 0.0,
            "avgExecutionTimeMs", avgExecutionTime != null ? avgExecutionTime : 0.0,
            "totalRows", totalRows != null ? totalRows : 0L,
            "byStatus", byStatus,
            "byTriggerType", byTriggerType
        );
    }

    /**
     * 获取游戏的报表概览
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGameReportOverview(String gameId) {
        List<ReportEntity> allReports = reportRepo.findByGameId(gameId);
        List<ReportEntity> publishedReports = reportRepo.findPublishedByGameId(gameId);
        List<ReportEntity> scheduledReports = reportRepo.findScheduledReports().stream()
            .filter(r -> r.gameId.equals(gameId))
            .collect(Collectors.toList());

        long totalExecutions = 0;
        long totalRows = 0;

        for (ReportEntity report : allReports) {
            Map<String, Object> stats = getReportStats(report.id);
            totalExecutions += (Long) stats.get("totalRuns");
            totalRows += (Long) stats.get("totalRows");
        }

        Map<String, Long> byType = allReports.stream()
            .collect(Collectors.groupingBy(r -> r.reportType.name(), Collectors.counting()));

        Map<String, Long> byCategory = allReports.stream()
            .filter(r -> r.reportCategory != null)
            .collect(Collectors.groupingBy(r -> r.reportCategory, Collectors.counting()));

        return Map.of(
            "totalReports", allReports.size(),
            "publishedReports", publishedReports.size(),
            "scheduledReports", scheduledReports.size(),
            "draftReports", allReports.stream().filter(r -> r.isDraft()).count(),
            "totalExecutions", totalExecutions,
            "totalRowsProcessed", totalRows,
            "byType", byType,
            "byCategory", byCategory
        );
    }

    /**
     * 定期检查超时的执行
     */
    @Scheduled(fixedDelay = 60000)  // 每分钟执行一次
    @Transactional
    public void checkTimeoutExecutions() {
        try {
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);  // 30分钟超时
            List<ReportExecutionEntity> timeoutExecutions = executionRepo.findTimeout(timeout);

            for (ReportExecutionEntity execution : timeoutExecutions) {
                execution.executionStatus = ReportExecutionEntity.ExecutionStatus.TIMEOUT;
                execution.statusMessage = "Execution timeout";
                execution.completedAt = LocalDateTime.now();
                executionRepo.save(execution);

                logger.warn("Report execution timed out: {}", execution.id);
            }

            if (!timeoutExecutions.isEmpty()) {
                logger.info("Marked {} executions as timeout", timeoutExecutions.size());
            }
        } catch (Exception e) {
            logger.error("Failed to check timeout executions", e);
        }
    }

    /**
     * 清理过期执行记录
     */
    @Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨3点执行
    @Transactional
    public void cleanupExpiredExecutions() {
        try {
            LocalDateTime expireAt = LocalDateTime.now().minusDays(90);  // 保留90天
            int deleted = executionRepo.deleteExpired(expireAt);
            if (deleted > 0) {
                logger.info("Cleaned up {} expired report executions", deleted);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup expired executions", e);
        }
    }

    // 私有辅助方法

    private void executeReportAsync(ReportEntity report, ReportExecutionEntity execution) {
        try {
            // 标记为运行中
            execution.markAsRunning();
            executionRepo.save(execution);

            // 更新报表运行记录
            report.recordRun("running");
            reportRepo.save(report);

            // TODO: 实现实际的报表查询逻辑
            // 这里简化实现，模拟报表执行
            simulateReportExecution(execution);

        } catch (Exception e) {
            execution.markAsFailed(e.getMessage());
            executionRepo.save(execution);

            report.recordRun("failed");
            reportRepo.save(report);

            logger.error("Report execution failed: {} - {}", execution.id, e.getMessage(), e);
        }
    }

    private void simulateReportExecution(ReportExecutionEntity execution) {
        try {
            // 模拟查询执行
            Thread.sleep(100);  // 模拟查询时间

            // 生成模拟结果
            long rowCount = (long) (Math.random() * 1000) + 100;
            String resultSummary = objectMapper.writeValueAsString(Map.of(
                "rowCount", rowCount,
                "columns", List.of("date", "metric1", "metric2"),
                "summary", Map.of("total", 10000, "avg", 50.0)
            ));

            execution.markAsCompleted(rowCount, resultSummary, 100L);
            executionRepo.save(execution);

        } catch (Exception e) {
            execution.markAsFailed(e.getMessage());
            executionRepo.save(execution);
        }
    }

    /**
     * 搜索报表
     */
    @Transactional(readOnly = true)
    public List<ReportEntity> searchReports(String gameId, String query) {
        return reportRepo.search(gameId, query);
    }

    /**
     * 获取热门报表
     */
    @Transactional(readOnly = true)
    public List<ReportEntity> getPopularReports(String gameId) {
        return reportRepo.findPopularReports(gameId).stream()
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取最近运行的报表
     */
    @Transactional(readOnly = true)
    public List<ReportEntity> getRecentlyRunReports(String gameId) {
        return reportRepo.findRecentlyRun(gameId).stream()
            .limit(10)
            .collect(Collectors.toList());
    }
}
