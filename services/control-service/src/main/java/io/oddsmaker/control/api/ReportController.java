package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.ReportEntity;
import io.oddsmaker.control.jpa.ReportExecutionEntity;
import io.oddsmaker.control.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 报表API控制器
 * 提供自定义报表管理的API接口
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 创建报表
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ANALYZE_DATA:' + #request.gameId)")
    public ResponseEntity<ReportEntity> createReport(@RequestBody ReportRequest request) {
        ReportEntity report = reportService.createReport(
            request.gameId,
            request.environmentId,
            request.name,
            request.displayName,
            request.description,
            request.reportType != null ? ReportEntity.ReportType.valueOf(request.reportType) : null,
            request.reportCategory,
            request.queryConfig,
            request.visualization,
            request.chartType,
            request.createdBy
        );
        return ResponseEntity.ok(report);
    }

    /**
     * 获取报表详情
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<ReportEntity> getReport(
            @PathVariable String reportId,
            @RequestParam String gameId) {
        ReportEntity report = reportService.getReport(reportId);
        return ResponseEntity.ok(report);
    }

    /**
     * 获取游戏的报表列表
     */
    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<ReportEntity>> getGameReports(@PathVariable String gameId) {
        List<ReportEntity> reports = reportService.getGameReports(gameId);
        return ResponseEntity.ok(reports);
    }

    /**
     * 获取已发布的报表
     */
    @GetMapping("/published/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<ReportEntity>> getPublishedReports(@PathVariable String gameId) {
        List<ReportEntity> reports = reportService.getPublishedReports(gameId);
        return ResponseEntity.ok(reports);
    }

    /**
     * 发布报表
     */
    @PostMapping("/{reportId}/publish")
    @PreAuthorize("hasAuthority('MANAGE_GAME:' + #gameId)")
    public ResponseEntity<ReportEntity> publishReport(
            @PathVariable String reportId,
            @RequestParam String gameId,
            @RequestBody PublishRequest request) {
        ReportEntity report = reportService.publishReport(reportId, request.publishedBy);
        return ResponseEntity.ok(report);
    }

    /**
     * 执行报表
     */
    @PostMapping("/{reportId}/execute")
    @PreAuthorize("hasAuthority('ANALYZE_DATA:' + #gameId)")
    public ResponseEntity<ReportExecutionEntity> executeReport(
            @PathVariable String reportId,
            @RequestParam String gameId,
            @RequestBody ExecuteRequest request) {
        ReportExecutionEntity execution = reportService.executeReport(
            reportId,
            request.triggeredBy,
            request.triggerType,
            request.parameters,
            request.filters
        );
        return ResponseEntity.ok(execution);
    }

    /**
     * 获取报表执行历史
     */
    @GetMapping("/{reportId}/executions")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<ReportExecutionEntity>> getReportExecutions(
            @PathVariable String reportId,
            @RequestParam String gameId) {
        List<ReportExecutionEntity> executions = reportService.getReportExecutions(reportId);
        return ResponseEntity.ok(executions);
    }

    /**
     * 获取报表统计
     */
    @GetMapping("/{reportId}/stats")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getReportStats(
            @PathVariable String reportId,
            @RequestParam String gameId) {
        Map<String, Object> stats = reportService.getReportStats(reportId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取游戏的报表概览
     */
    @GetMapping("/overview/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getGameReportOverview(@PathVariable String gameId) {
        Map<String, Object> overview = reportService.getGameReportOverview(gameId);
        return ResponseEntity.ok(overview);
    }

    /**
     * 搜索报表
     */
    @GetMapping("/search/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<ReportEntity>> searchReports(
            @PathVariable String gameId,
            @RequestParam String query) {
        List<ReportEntity> reports = reportService.searchReports(gameId, query);
        return ResponseEntity.ok(reports);
    }

    /**
     * 获取热门报表
     */
    @GetMapping("/popular/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<ReportEntity>> getPopularReports(@PathVariable String gameId) {
        List<ReportEntity> reports = reportService.getPopularReports(gameId);
        return ResponseEntity.ok(reports);
    }

    /**
     * 获取最近运行的报表
     */
    @GetMapping("/recent/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<ReportEntity>> getRecentlyRunReports(@PathVariable String gameId) {
        List<ReportEntity> reports = reportService.getRecentlyRunReports(gameId);
        return ResponseEntity.ok(reports);
    }

    // Request DTOs

    public static class ReportRequest {
        public String gameId;
        public String environmentId;
        public String name;
        public String displayName;
        public String description;
        public String reportType;
        public String reportCategory;
        public Map<String, Object> queryConfig;
        public Map<String, Object> visualization;
        public String chartType;
        public String createdBy;
    }

    public static class PublishRequest {
        public String publishedBy;
    }

    public static class ExecuteRequest {
        public String triggeredBy;
        public String triggerType;
        public Map<String, Object> parameters;
        public Map<String, Object> filters;
    }
}
