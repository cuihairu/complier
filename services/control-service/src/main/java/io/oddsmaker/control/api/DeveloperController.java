package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.*;
import io.oddsmaker.control.service.DeveloperPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 开发者门户API控制器
 * 提供SDK管理和开发者工具接口
 */
@RestController
@RequestMapping("/api/developer")
public class DeveloperController {

    @Autowired
    private DeveloperPortalService developerPortalService;

    // ==================== SDK密钥管理 ====================

    /**
     * 创建SDK密钥
     */
    @PostMapping("/sdk-keys")
    @PreAuthorize("hasAuthority('MANAGE_SDK_KEYS:' + #request.gameId)")
    public ResponseEntity<SDKKeyEntity> createSDKKey(@RequestBody CreateSDKKeyRequest request) {
        SDKKeyEntity key = developerPortalService.createSDKKey(
            request.gameId,
            request.environment,
            request.keyName,
            request.platform,
            request.deliveryMode,
            request.config,
            request.createdBy
        );
        return ResponseEntity.ok(key);
    }

    /**
     * 获取SDK密钥详情
     */
    @GetMapping("/sdk-keys/{keyId}")
    @PreAuthorize("hasAuthority('VIEW_SDK_KEYS')")
    public ResponseEntity<SDKKeyEntity> getSDKKey(@PathVariable String keyId) {
        SDKKeyEntity key = developerPortalService.getSDKKey(keyId);
        return ResponseEntity.ok(key);
    }

    /**
     * 获取游戏的SDK密钥列表
     */
    @GetMapping("/sdk-keys/game/{gameId}")
    @PreAuthorize("hasAuthority('VIEW_SDK_KEYS:' + #gameId)")
    public ResponseEntity<List<SDKKeyEntity>> getGameSDKKeys(
            @PathVariable String gameId,
            @RequestParam(required = false) String environment) {
        List<SDKKeyEntity> keys = developerPortalService.getGameSDKKeys(gameId, environment);
        return ResponseEntity.ok(keys);
    }

    /**
     * 更新SDK密钥配置
     */
    @PutMapping("/sdk-keys/{keyId}")
    @PreAuthorize("hasAuthority('MANAGE_SDK_KEYS')")
    public ResponseEntity<SDKKeyEntity> updateSDKKey(
            @PathVariable String keyId,
            @RequestBody UpdateSDKKeyRequest request) {
        SDKKeyEntity key = developerPortalService.updateSDKKey(keyId, request.updates, request.updatedBy);
        return ResponseEntity.ok(key);
    }

    /**
     * 暂停SDK密钥
     */
    @PostMapping("/sdk-keys/{keyId}/suspend")
    @PreAuthorize("hasAuthority('MANAGE_SDK_KEYS')")
    public ResponseEntity<SDKKeyEntity> suspendSDKKey(
            @PathVariable String keyId,
            @RequestBody SuspendRequest request) {
        SDKKeyEntity key = developerPortalService.suspendSDKKey(keyId, request.suspendedBy);
        return ResponseEntity.ok(key);
    }

    /**
     * 激活SDK密钥
     */
    @PostMapping("/sdk-keys/{keyId}/activate")
    @PreAuthorize("hasAuthority('MANAGE_SDK_KEYS')")
    public ResponseEntity<SDKKeyEntity> activateSDKKey(
            @PathVariable String keyId,
            @RequestBody ActivateRequest request) {
        SDKKeyEntity key = developerPortalService.activateSDKKey(keyId, request.activatedBy);
        return ResponseEntity.ok(key);
    }

    /**
     * 撤销SDK密钥
     */
    @PostMapping("/sdk-keys/{keyId}/revoke")
    @PreAuthorize("hasAuthority('MANAGE_SDK_KEYS')")
    public ResponseEntity<SDKKeyEntity> revokeSDKKey(
            @PathVariable String keyId,
            @RequestBody RevokeRequest request) {
        SDKKeyEntity key = developerPortalService.revokeSDKKey(keyId, request.revokedBy);
        return ResponseEntity.ok(key);
    }

    /**
     * 删除SDK密钥
     */
    @DeleteMapping("/sdk-keys/{keyId}")
    @PreAuthorize("hasAuthority('MANAGE_SDK_KEYS')")
    public ResponseEntity<Void> deleteSDKKey(
            @PathVariable String keyId,
            @RequestBody DeleteRequest request) {
        developerPortalService.deleteSDKKey(keyId, request.deletedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * 验证SDK密钥
     */
    @GetMapping("/sdk-keys/{publicKey}/validate")
    public ResponseEntity<Map<String, Object>> validateSDKKey(
            @PathVariable String publicKey,
            @RequestParam String gameId,
            @RequestParam String environment) {
        boolean valid = developerPortalService.validateSDKKey(publicKey, gameId, environment);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // ==================== SDK版本管理 ====================

    /**
     * 创建SDK版本
     */
    @PostMapping("/sdk-versions")
    @PreAuthorize("hasAuthority('MANAGE_SDK_VERSIONS')")
    public ResponseEntity<SDKVersionEntity> createSDKVersion(@RequestBody CreateSDKVersionRequest request) {
        SDKVersionEntity version = developerPortalService.createSDKVersion(
            request.platform,
            request.version,
            request.changeType,
            request.releaseNotes,
            request.changelog,
            request.createdBy
        );
        return ResponseEntity.ok(version);
    }

    /**
     * 获取SDK版本详情
     */
    @GetMapping("/sdk-versions/{versionId}")
    @PreAuthorize("hasAuthority('VIEW_SDK_VERSIONS')")
    public ResponseEntity<SDKVersionEntity> getSDKVersion(@PathVariable String versionId) {
        SDKVersionEntity version = developerPortalService.getSDKVersion(versionId);
        return ResponseEntity.ok(version);
    }

    /**
     * 获取平台的版本列表
     */
    @GetMapping("/sdk-versions/platform/{platform}")
    @PreAuthorize("hasAuthority('VIEW_SDK_VERSIONS')")
    public ResponseEntity<List<SDKVersionEntity>> getPlatformVersions(
            @PathVariable SDKVersionEntity.SDKPlatform platform,
            @RequestParam(required = false) String status) {
        List<SDKVersionEntity> versions = developerPortalService.getPlatformVersions(platform, status);
        return ResponseEntity.ok(versions);
    }

    /**
     * 获取最新版本
     */
    @GetMapping("/sdk-versions/platform/{platform}/latest")
    @PreAuthorize("hasAuthority('VIEW_SDK_VERSIONS')")
    public ResponseEntity<SDKVersionEntity> getLatestVersion(@PathVariable SDKVersionEntity.SDKPlatform platform) {
        SDKVersionEntity version = developerPortalService.getLatestVersion(platform);
        return ResponseEntity.ok(version);
    }

    /**
     * 发布版本
     */
    @PostMapping("/sdk-versions/{versionId}/release")
    @PreAuthorize("hasAuthority('MANAGE_SDK_VERSIONS')")
    public ResponseEntity<SDKVersionEntity> releaseVersion(
            @PathVariable String versionId,
            @RequestBody ReleaseVersionRequest request) {
        SDKVersionEntity version = developerPortalService.releaseVersion(
            versionId,
            request.downloadUrl,
            request.packageName,
            request.packageManager,
            request.checksumSha256,
            request.fileSizeBytes,
            request.releasedBy
        );
        return ResponseEntity.ok(version);
    }

    /**
     * 弃用版本
     */
    @PostMapping("/sdk-versions/{versionId}/deprecate")
    @PreAuthorize("hasAuthority('MANAGE_SDK_VERSIONS')")
    public ResponseEntity<SDKVersionEntity> deprecateVersion(
            @PathVariable String versionId,
            @RequestBody DeprecateVersionRequest request) {
        SDKVersionEntity version = developerPortalService.deprecateVersion(
            versionId,
            request.deprecationNotice,
            request.deprecatedBy
        );
        return ResponseEntity.ok(version);
    }

    /**
     * 退役版本
     */
    @PostMapping("/sdk-versions/{versionId}/retire")
    @PreAuthorize("hasAuthority('MANAGE_SDK_VERSIONS')")
    public ResponseEntity<SDKVersionEntity> retireVersion(
            @PathVariable String versionId,
            @RequestBody RetireRequest request) {
        SDKVersionEntity version = developerPortalService.retireVersion(versionId, request.retiredBy);
        return ResponseEntity.ok(version);
    }

    /**
     * 记录下载
     */
    @PostMapping("/sdk-versions/{versionId}/download")
    public ResponseEntity<Void> recordDownload(@PathVariable String versionId) {
        developerPortalService.recordDownload(versionId);
        return ResponseEntity.ok().build();
    }

    // ==================== 遥测配置管理 ====================

    /**
     * 创建遥测配置
     */
    @PostMapping("/telemetry-configs")
    @PreAuthorize("hasAuthority('MANAGE_TELEMETRY:' + #request.gameId)")
    public ResponseEntity<TelemetryConfigEntity> createTelemetryConfig(@RequestBody CreateTelemetryConfigRequest request) {
        TelemetryConfigEntity config = developerPortalService.createTelemetryConfig(
            request.gameId,
            request.environmentId,
            request.configName,
            request.configType,
            request.description,
            request.isDefault,
            request.config,
            request.createdBy
        );
        return ResponseEntity.ok(config);
    }

    /**
     * 获取遥测配置详情
     */
    @GetMapping("/telemetry-configs/{configId}")
    @PreAuthorize("hasAuthority('VIEW_TELEMETRY')")
    public ResponseEntity<TelemetryConfigEntity> getTelemetryConfig(@PathVariable String configId) {
        TelemetryConfigEntity config = developerPortalService.getTelemetryConfig(configId);
        return ResponseEntity.ok(config);
    }

    /**
     * 获取游戏的遥测配置列表
     */
    @GetMapping("/telemetry-configs/game/{gameId}")
    @PreAuthorize("hasAuthority('VIEW_TELEMETRY:' + #gameId)")
    public ResponseEntity<List<TelemetryConfigEntity>> getGameTelemetryConfigs(
            @PathVariable String gameId,
            @RequestParam(required = false) String environmentId) {
        List<TelemetryConfigEntity> configs = developerPortalService.getGameTelemetryConfigs(gameId, environmentId);
        return ResponseEntity.ok(configs);
    }

    /**
     * 获取有效的遥测配置
     */
    @GetMapping("/telemetry-configs/effective")
    @PreAuthorize("hasAuthority('VIEW_TELEMETRY')")
    public ResponseEntity<TelemetryConfigEntity> getEffectiveConfig(
            @RequestParam String gameId,
            @RequestParam String environmentId,
            @RequestParam TelemetryConfigEntity.ConfigType configType) {
        TelemetryConfigEntity config = developerPortalService.getEffectiveConfig(gameId, environmentId, configType);
        return ResponseEntity.ok(config);
    }

    /**
     * 更新遥测配置
     */
    @PutMapping("/telemetry-configs/{configId}")
    @PreAuthorize("hasAuthority('MANAGE_TELEMETRY')")
    public ResponseEntity<TelemetryConfigEntity> updateTelemetryConfig(
            @PathVariable String configId,
            @RequestBody UpdateTelemetryConfigRequest request) {
        TelemetryConfigEntity config = developerPortalService.updateTelemetryConfig(configId, request.updates, request.updatedBy);
        return ResponseEntity.ok(config);
    }

    /**
     * 激活遥测配置
     */
    @PostMapping("/telemetry-configs/{configId}/activate")
    @PreAuthorize("hasAuthority('MANAGE_TELEMETRY')")
    public ResponseEntity<TelemetryConfigEntity> activateTelemetryConfig(
            @PathVariable String configId,
            @RequestBody ActivateRequest request) {
        TelemetryConfigEntity config = developerPortalService.activateTelemetryConfig(configId, request.activatedBy);
        return ResponseEntity.ok(config);
    }

    /**
     * 停用遥测配置
     */
    @PostMapping("/telemetry-configs/{configId}/deactivate")
    @PreAuthorize("hasAuthority('MANAGE_TELEMETRY')")
    public ResponseEntity<TelemetryConfigEntity> deactivateTelemetryConfig(
            @PathVariable String configId,
            @RequestBody DeactivateRequest request) {
        TelemetryConfigEntity config = developerPortalService.deactivateTelemetryConfig(configId, request.deactivatedBy);
        return ResponseEntity.ok(config);
    }

    /**
     * 归档遥测配置
     */
    @PostMapping("/telemetry-configs/{configId}/archive")
    @PreAuthorize("hasAuthority('MANAGE_TELEMETRY')")
    public ResponseEntity<TelemetryConfigEntity> archiveTelemetryConfig(
            @PathVariable String configId,
            @RequestBody ArchiveRequest request) {
        TelemetryConfigEntity config = developerPortalService.archiveTelemetryConfig(configId, request.archivedBy);
        return ResponseEntity.ok(config);
    }

    /**
     * 删除遥测配置
     */
    @DeleteMapping("/telemetry-configs/{configId}")
    @PreAuthorize("hasAuthority('MANAGE_TELEMETRY')")
    public ResponseEntity<Void> deleteTelemetryConfig(
            @PathVariable String configId,
            @RequestBody DeleteRequest request) {
        developerPortalService.deleteTelemetryConfig(configId, request.deletedBy);
        return ResponseEntity.ok().build();
    }

    // ==================== 统计信息 ====================

    /**
     * 获取SDK统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('VIEW_SDK_STATS')")
    public ResponseEntity<Map<String, Object>> getSDKStatistics() {
        Map<String, Object> stats = developerPortalService.getSDKStatistics();
        return ResponseEntity.ok(stats);
    }

    // ==================== 请求/响应类 ====================

    /**
     * 创建SDK密钥请求
     */
    public static class CreateSDKKeyRequest {
        public String gameId;
        public String environment;
        public String keyName;
        public SDKKeyEntity.SDKPlatform platform;
        public SDKKeyEntity.DeliveryMode deliveryMode;
        public Map<String, Object> config;
        public String createdBy;
    }

    /**
     * 更新SDK密钥请求
     */
    public static class UpdateSDKKeyRequest {
        public Map<String, Object> updates;
        public String updatedBy;
    }

    /**
     * 暂停请求
     */
    public static class SuspendRequest {
        public String suspendedBy;
    }

    /**
     * 激活请求
     */
    public static class ActivateRequest {
        public String activatedBy;
    }

    /**
     * 撤销请求
     */
    public static class RevokeRequest {
        public String revokedBy;
    }

    /**
     * 删除请求
     */
    public static class DeleteRequest {
        public String deletedBy;
    }

    /**
     * 创建SDK版本请求
     */
    public static class CreateSDKVersionRequest {
        public SDKVersionEntity.SDKPlatform platform;
        public String version;
        public SDKVersionEntity.ChangeType changeType;
        public String releaseNotes;
        public String changelog;
        public String createdBy;
    }

    /**
     * 发布版本请求
     */
    public static class ReleaseVersionRequest {
        public String downloadUrl;
        public String packageName;
        public String packageManager;
        public String checksumSha256;
        public Long fileSizeBytes;
        public String releasedBy;
    }

    /**
     * 弃用版本请求
     */
    public static class DeprecateVersionRequest {
        public String deprecationNotice;
        public String deprecatedBy;
    }

    /**
     * 退役请求
     */
    public static class RetireRequest {
        public String retiredBy;
    }

    /**
     * 创建遥测配置请求
     */
    public static class CreateTelemetryConfigRequest {
        public String gameId;
        public String environmentId;
        public String configName;
        public TelemetryConfigEntity.ConfigType configType;
        public String description;
        public boolean isDefault;
        public Map<String, Object> config;
        public String createdBy;
    }

    /**
     * 更新遥测配置请求
     */
    public static class UpdateTelemetryConfigRequest {
        public Map<String, Object> updates;
        public String updatedBy;
    }

    /**
     * 停用请求
     */
    public static class DeactivateRequest {
        public String deactivatedBy;
    }

    /**
     * 归档请求
     */
    public static class ArchiveRequest {
        public String archivedBy;
    }
}
