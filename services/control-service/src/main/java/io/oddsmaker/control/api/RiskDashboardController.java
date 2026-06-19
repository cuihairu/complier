package io.oddsmaker.control.api;

import io.oddsmaker.control.service.RiskDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 风控仪表盘API控制器
 * 提供风控数据可视化的API接口
 */
@RestController
@RequestMapping("/api/risk-dashboard")
public class RiskDashboardController {

    @Autowired
    private RiskDashboardService riskDashboardService;

    /**
     * 获取游戏的风控概览
     */
    @GetMapping("/overview/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getOverview(
            @PathVariable String gameId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        Map<String, Object> overview = riskDashboardService.getOverview(gameId, since);
        return ResponseEntity.ok(overview);
    }

    /**
     * 获取风险趋势
     */
    @GetMapping("/trends/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<Map<String, Object>>> getTrends(
            @PathVariable String gameId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "24") int intervalHours) {
        List<Map<String, Object>> trends = riskDashboardService.getRiskTrends(gameId, since, intervalHours);
        return ResponseEntity.ok(trends);
    }

    /**
     * 获取高风险目标
     */
    @GetMapping("/high-risk-targets/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<Map<String, Object>>> getHighRiskTargets(
            @PathVariable String gameId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> targets = riskDashboardService.getHighRiskTargets(gameId, since, limit);
        return ResponseEntity.ok(targets);
    }

    /**
     * 获取规则性能统计
     */
    @GetMapping("/rule-performance/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<Map<String, Object>>> getRulePerformance(@PathVariable String gameId) {
        List<Map<String, Object>> performance = riskDashboardService.getRulePerformance(gameId);
        return ResponseEntity.ok(performance);
    }

    /**
     * 获取封禁统计
     */
    @GetMapping("/block-stats/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getBlockStats(@PathVariable String gameId) {
        Map<String, Object> stats = riskDashboardService.getBlockStats(gameId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取作业统计
     */
    @GetMapping("/job-stats/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getJobStats(@PathVariable String gameId) {
        Map<String, Object> stats = riskDashboardService.getJobStats(gameId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取完整仪表盘数据
     */
    @GetMapping("/dashboard/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @PathVariable String gameId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        Map<String, Object> dashboard = riskDashboardService.getDashboard(gameId, since);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * 获取最近的风险案例
     */
    @GetMapping("/recent-cases/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<Map<String, Object>>> getRecentCases(
            @PathVariable String gameId,
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> cases = riskDashboardService.getRecentCases(gameId, limit);
        return ResponseEntity.ok(cases);
    }

    /**
     * 获取审核队列统计
     */
    @GetMapping("/review-queue-stats/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getReviewQueueStats(@PathVariable String gameId) {
        Map<String, Object> stats = riskDashboardService.getReviewQueueStats(gameId);
        return ResponseEntity.ok(stats);
    }
}
