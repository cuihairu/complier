package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.*;
import io.oddsmaker.control.service.MaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统管理API控制器
 * 提供维护模式、功能开关和系统配置管理的接口
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private MaintenanceService maintenanceService;

    // ============== Maintenance Window Endpoints ==============

    /**
     * 创建维护窗口
     */
    @PostMapping("/maintenances")
    @PreAuthorize("hasAuthority('MANAGE_MAINTENANCE')")
    public ResponseEntity<MaintenanceWindowEntity> createMaintenance(@RequestBody MaintenanceRequest request) {
        MaintenanceWindowEntity window = maintenanceService.createMaintenanceWindow(
            request.title,
            request.description,
            request.maintenanceType,
            request.impactScope,
            request.gameId,
            request.environmentId,
            request.scheduledStart,
            request.scheduledEnd,
            request.createdBy
        );
        return ResponseEntity.ok(window);
    }

    /**
     * 获取活跃的维护窗口
     */
    @GetMapping("/maintenances/active")
    @PreAuthorize("hasAuthority('VIEW_MAINTENANCE')")
    public ResponseEntity<List<MaintenanceWindowEntity>> getActiveMaintenances() {
        List<MaintenanceWindowEntity> windows = maintenanceService.getActiveMaintenances();
        return ResponseEntity.ok(windows);
    }

    /**
     * 获取即将到来的维护
     */
    @GetMapping("/maintenances/upcoming")
    @PreAuthorize("hasAuthority('VIEW_MAINTENANCE')")
    public ResponseEntity<List<MaintenanceWindowEntity>> getUpcomingMaintenances() {
        List<MaintenanceWindowEntity> windows = maintenanceService.getUpcomingMaintenances();
        return ResponseEntity.ok(windows);
    }

    /**
     * 开始维护
     */
    @PostMapping("/maintenances/{windowId}/start")
    @PreAuthorize("hasAuthority('MANAGE_MAINTENANCE')")
    public ResponseEntity<MaintenanceWindowEntity> startMaintenance(@PathVariable String windowId) {
        MaintenanceWindowEntity window = maintenanceService.startMaintenance(windowId);
        return ResponseEntity.ok(window);
    }

    /**
     * 完成维护
     */
    @PostMapping("/maintenances/{windowId}/complete")
    @PreAuthorize("hasAuthority('MANAGE_MAINTENANCE')")
    public ResponseEntity<MaintenanceWindowEntity> completeMaintenance(
            @PathVariable String windowId,
            @RequestBody CompleteRequest request) {
        MaintenanceWindowEntity window = maintenanceService.completeMaintenance(windowId, request.notes);
        return ResponseEntity.ok(window);
    }

    /**
     * 取消维护
     */
    @PostMapping("/maintenances/{windowId}/cancel")
    @PreAuthorize("hasAuthority('MANAGE_MAINTENANCE')")
    public ResponseEntity<MaintenanceWindowEntity> cancelMaintenance(
            @PathVariable String windowId,
            @RequestBody CancelRequest request) {
        MaintenanceWindowEntity window = maintenanceService.cancelMaintenance(windowId, request.reason);
        return ResponseEntity.ok(window);
    }

    /**
     * 检查游戏是否在维护中
     */
    @GetMapping("/maintenances/check/{gameId}")
    public ResponseEntity<Map<String, Boolean>> checkMaintenance(@PathVariable String gameId) {
        boolean inMaintenance = maintenanceService.isGameInMaintenance(gameId);
        return ResponseEntity.ok(Map.of("inMaintenance", inMaintenance));
    }

    // ============== System Config Endpoints ==============

    /**
     * 获取所有配置
     */
    @GetMapping("/configs")
    @PreAuthorize("hasAuthority('VIEW_SYSTEM_CONFIGS')")
    public ResponseEntity<List<SystemConfigEntity>> getAllConfigs() {
        List<SystemConfigEntity> configs = maintenanceService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取公开配置
     */
    @GetMapping("/configs/public")
    public ResponseEntity<Map<String, String>> getPublicConfigs() {
        Map<String, String> configs = maintenanceService.getPublicConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取配置值
     */
    @GetMapping("/configs/{configKey}")
    @PreAuthorize("hasAuthority('VIEW_SYSTEM_CONFIGS')")
    public ResponseEntity<String> getConfigValue(@PathVariable String configKey) {
        String value = maintenanceService.getConfigValue(configKey);
        return ResponseEntity.ok(value);
    }

    /**
     * 设置配置值
     */
    @PutMapping("/configs/{configKey}")
    @PreAuthorize("hasAuthority('MANAGE_SYSTEM_CONFIGS')")
    public ResponseEntity<SystemConfigEntity> setConfigValue(
            @PathVariable String configKey,
            @RequestBody ConfigValueRequest request) {
        SystemConfigEntity config = maintenanceService.setConfigValue(configKey, request.value, request.modifiedBy);
        return ResponseEntity.ok(config);
    }

    // ============== Feature Flag Endpoints ==============

    /**
     * 获取所有功能开关
     */
    @GetMapping("/features")
    @PreAuthorize("hasAuthority('VIEW_FEATURE_FLAGS')")
    public ResponseEntity<List<FeatureFlagEntity>> getAllFeatureFlags() {
        List<FeatureFlagEntity> flags = maintenanceService.getAllFeatureFlags();
        return ResponseEntity.ok(flags);
    }

    /**
     * 获取启用的功能开关
     */
    @GetMapping("/features/enabled")
    @PreAuthorize("hasAuthority('VIEW_FEATURE_FLAGS')")
    public ResponseEntity<List<FeatureFlagEntity>> getEnabledFeatureFlags() {
        List<FeatureFlagEntity> flags = maintenanceService.getEnabledFeatureFlags();
        return ResponseEntity.ok(flags);
    }

    /**
     * 检查功能是否启用
     */
    @GetMapping("/features/{flagKey}/check")
    public ResponseEntity<Map<String, Boolean>> checkFeature(
            @PathVariable String flagKey,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String gameId) {
        boolean enabled = maintenanceService.isFeatureEnabled(flagKey, userId, gameId);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    /**
     * 启用功能
     */
    @PostMapping("/features/{flagKey}/enable")
    @PreAuthorize("hasAuthority('MANAGE_FEATURE_FLAGS')")
    public ResponseEntity<FeatureFlagEntity> enableFeature(
            @PathVariable String flagKey,
            @RequestBody ModifyRequest request) {
        FeatureFlagEntity flag = maintenanceService.enableFeature(flagKey, request.modifiedBy);
        return ResponseEntity.ok(flag);
    }

    /**
     * 禁用功能
     */
    @PostMapping("/features/{flagKey}/disable")
    @PreAuthorize("hasAuthority('MANAGE_FEATURE_FLAGS')")
    public ResponseEntity<FeatureFlagEntity> disableFeature(
            @PathVariable String flagKey,
            @RequestBody ModifyRequest request) {
        FeatureFlagEntity flag = maintenanceService.disableFeature(flagKey, request.modifiedBy);
        return ResponseEntity.ok(flag);
    }

    /**
     * 设置功能百分比
     */
    @PostMapping("/features/{flagKey}/percentage")
    @PreAuthorize("hasAuthority('MANAGE_FEATURE_FLAGS')")
    public ResponseEntity<FeatureFlagEntity> setFeaturePercentage(
            @PathVariable String flagKey,
            @RequestBody PercentageRequest request) {
        FeatureFlagEntity flag = maintenanceService.setFeaturePercentage(flagKey, request.percentage, request.modifiedBy);
        return ResponseEntity.ok(flag);
    }

    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('VIEW_SYSTEM_STATUS')")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = maintenanceService.getSystemStatus();
        return ResponseEntity.ok(status);
    }

    // Request DTOs

    public static class MaintenanceRequest {
        public String title;
        public String description;
        public MaintenanceWindowEntity.MaintenanceType maintenanceType;
        public MaintenanceWindowEntity.ImpactScope impactScope;
        public String gameId;
        public String environmentId;
        public LocalDateTime scheduledStart;
        public LocalDateTime scheduledEnd;
        public String createdBy;
    }

    public static class CompleteRequest {
        public String notes;
    }

    public static class CancelRequest {
        public String reason;
    }

    public static class ConfigValueRequest {
        public String value;
        public String modifiedBy;
    }

    public static class ModifyRequest {
        public String modifiedBy;
    }

    public static class PercentageRequest {
        public Integer percentage;
        public String modifiedBy;
    }
}
