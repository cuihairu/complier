package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.ExportJobEntity;
import io.oddsmaker.control.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据导出API控制器
 * 提供数据导出管理的API接口
 */
@RestController
@RequestMapping("/api/exports")
public class ExportController {

    @Autowired
    private ExportService exportService;

    /**
     * 创建导出任务
     */
    @PostMapping
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #request.gameId)")
    public ResponseEntity<ExportJobEntity> createExportJob(@RequestBody ExportRequest request) {
        ExportJobEntity job = exportService.createExportJob(
            request.gameId,
            request.environmentId,
            request.userId,
            request.exportType,
            request.startTime,
            request.endTime,
            request.exportFormat,
            request.filters,
            request.dataSource,
            request.columns,
            request.compression,
            request.notifyOnComplete,
            request.notificationEmail
        );
        return ResponseEntity.ok(job);
    }

    /**
     * 获取导出任务详情
     */
    @GetMapping("/{exportJobId}")
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #gameId)")
    public ResponseEntity<ExportJobEntity> getExportJob(
            @PathVariable String exportJobId,
            @RequestParam String gameId) {
        ExportJobEntity job = exportService.getExportJob(exportJobId);
        return ResponseEntity.ok(job);
    }

    /**
     * 获取用户的导出任务列表
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #gameId)")
    public ResponseEntity<List<ExportJobEntity>> getUserExports(
            @PathVariable String userId,
            @RequestParam String gameId) {
        List<ExportJobEntity> jobs = exportService.getUserExports(userId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * 获取游戏的导出任务列表
     */
    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #gameId)")
    public ResponseEntity<List<ExportJobEntity>> getGameExports(@PathVariable String gameId) {
        List<ExportJobEntity> jobs = exportService.getGameExports(gameId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * 处理导出任务
     */
    @PostMapping("/{exportJobId}/process")
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #gameId)")
    public ResponseEntity<ExportJobEntity> processExportJob(
            @PathVariable String exportJobId,
            @RequestParam String gameId) {
        ExportJobEntity job = exportService.processExportJob(exportJobId);
        return ResponseEntity.ok(job);
    }

    /**
     * 取消导出任务
     */
    @PostMapping("/{exportJobId}/cancel")
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #gameId)")
    public ResponseEntity<ExportJobEntity> cancelExportJob(
            @PathVariable String exportJobId,
            @RequestParam String gameId,
            @RequestBody CancelRequest request) {
        ExportJobEntity job = exportService.cancelExportJob(exportJobId, request.reason);
        return ResponseEntity.ok(job);
    }

    /**
     * 获取导出统计
     */
    @GetMapping("/stats/{gameId}")
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getExportStats(@PathVariable String gameId) {
        Map<String, Object> stats = exportService.getExportStats(gameId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取用户导出统计
     */
    @GetMapping("/user-stats/{userId}")
    @PreAuthorize("hasAuthority('EXPORT_DATA:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getUserExportStats(
            @PathVariable String userId,
            @RequestParam String gameId) {
        Map<String, Object> stats = exportService.getUserExportStats(userId);
        return ResponseEntity.ok(stats);
    }

    // Request DTOs

    public static class ExportRequest {
        public String gameId;
        public String environmentId;
        public String userId;
        public String exportType;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String exportFormat;
        public Map<String, Object> filters;
        public Map<String, Object> dataSource;
        public List<String> columns;
        public String compression;
        public Boolean notifyOnComplete;
        public String notificationEmail;
    }

    public static class CancelRequest {
        public String reason;
    }
}
