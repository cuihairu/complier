package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.HealthCheckEntity;
import io.oddsmaker.control.jpa.HealthMetricEntity;
import io.oddsmaker.control.jpa.SystemAlertEntity;
import io.oddsmaker.control.service.HealthMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 健康监控API控制器
 * 提供系统健康检查和告警管理的接口
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthMonitorService healthMonitorService;

    // ============== Health Check Endpoints ==============

    /**
     * 获取系统整体健康状况
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = healthMonitorService.getSystemHealth();
        return ResponseEntity.ok(health);
    }

    /**
     * 获取所有健康检查
     */
    @GetMapping("/checks")
    @PreAuthorize("hasAuthority('VIEW_HEALTH_CHECKS')")
    public ResponseEntity<List<HealthCheckEntity>> getHealthChecks() {
        List<HealthCheckEntity> checks = healthMonitorService.getHealthChecks();
        return ResponseEntity.ok(checks);
    }

    /**
     * 获取健康检查详情
     */
    @GetMapping("/checks/{checkName}")
    @PreAuthorize("hasAuthority('VIEW_HEALTH_CHECKS')")
    public ResponseEntity<HealthCheckEntity> getHealthCheck(@PathVariable String checkName) {
        HealthCheckEntity check = healthMonitorService.getHealthCheck(checkName);
        return ResponseEntity.ok(check);
    }

    /**
     * 执行健康检查
     */
    @PostMapping("/checks/{checkName}/run")
    @PreAuthorize("hasAuthority('MANAGE_HEALTH_CHECKS')")
    public ResponseEntity<HealthCheckEntity> runHealthCheck(@PathVariable String checkName) {
        HealthCheckEntity result = healthMonitorService.performHealthCheck(checkName);
        return ResponseEntity.ok(result);
    }

    /**
     * 公开健康检查端点（用于负载均衡器等）
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> status = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.ok(status);
    }

    /**
     * 公开就绪检查端点
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> systemHealth = healthMonitorService.getSystemHealth();
        HealthCheckEntity.HealthStatus overallStatus = (HealthCheckEntity.HealthStatus) systemHealth.get("overallStatus");

        boolean isReady = overallStatus == HealthCheckEntity.HealthStatus.HEALTHY;
        Map<String, Object> status = Map.of(
            "status", isReady ? "READY" : "NOT_READY",
            "timestamp", LocalDateTime.now(),
            "details", systemHealth
        );

        return ResponseEntity.ok(status);
    }

    // ============== Metrics Endpoints ==============

    /**
     * 获取最近的指标
     */
    @GetMapping("/metrics/{type}")
    @PreAuthorize("hasAuthority('VIEW_METRICS')")
    public ResponseEntity<List<HealthMetricEntity>> getRecentMetrics(
            @PathVariable HealthMetricEntity.MetricType type,
            @RequestParam(required = false) String since) {
        LocalDateTime sinceDate = since != null ? LocalDateTime.parse(since) : LocalDateTime.now().minusHours(1);
        List<HealthMetricEntity> metrics = healthMonitorService.getRecentMetrics(type, sinceDate);
        return ResponseEntity.ok(metrics);
    }

    // ============== Alerts Endpoints ==============

    /**
     * 获取活跃告警
     */
    @GetMapping("/alerts/active")
    @PreAuthorize("hasAuthority('VIEW_ALERTS')")
    public ResponseEntity<List<SystemAlertEntity>> getActiveAlerts() {
        List<SystemAlertEntity> alerts = healthMonitorService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/alerts/stats")
    @PreAuthorize("hasAuthority('VIEW_ALERTS')")
    public ResponseEntity<Map<String, Object>> getAlertStats() {
        Map<String, Object> stats = healthMonitorService.getAlertStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 确认告警
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    @PreAuthorize("hasAuthority('MANAGE_ALERTS')")
    public ResponseEntity<SystemAlertEntity> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestBody AcknowledgeRequest request) {
        SystemAlertEntity alert = healthMonitorService.acknowledgeAlert(
            alertId,
            request.acknowledgedBy,
            request.comment
        );
        return ResponseEntity.ok(alert);
    }

    /**
     * 解决告警
     */
    @PostMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("hasAuthority('MANAGE_ALERTS')")
    public ResponseEntity<SystemAlertEntity> resolveAlert(
            @PathVariable String alertId,
            @RequestBody ResolveRequest request) {
        SystemAlertEntity alert = healthMonitorService.resolveAlert(
            alertId,
            request.resolvedBy,
            request.comment
        );
        return ResponseEntity.ok(alert);
    }

    // Request DTOs

    public static class AcknowledgeRequest {
        public String acknowledgedBy;
        public String comment;
    }

    public static class ResolveRequest {
        public String resolvedBy;
        public String comment;
    }
}
