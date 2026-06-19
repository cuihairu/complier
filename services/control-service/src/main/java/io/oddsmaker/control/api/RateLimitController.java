package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.QuotaEntity;
import io.oddsmaker.control.jpa.RateLimitEntity;
import io.oddsmaker.control.service.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 限流和配额API控制器
 * 提供API限流和资源配额管理的接口
 */
@RestController
@RequestMapping("/api/rate-limits")
public class RateLimitController {

    @Autowired
    private RateLimitService rateLimitService;

    // ============== Rate Limit Endpoints ==============

    /**
     * 创建限流规则
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_RATE_LIMITS:' + #request.gameId)")
    public ResponseEntity<RateLimitEntity> createRateLimit(@RequestBody RateLimitRequest request) {
        RateLimitEntity rule = rateLimitService.createRateLimit(
            request.gameId,
            request.apiKeyId,
            request.endpoint,
            request.userId,
            request.scope,
            request.limit,
            request.windowType,
            request.windowSize,
            request.algorithm,
            request.burst,
            request.description,
            request.createdBy
        );
        return ResponseEntity.ok(rule);
    }

    /**
     * 获取限流规则详情
     */
    @GetMapping("/{ruleId}")
    @PreAuthorize("hasAuthority('VIEW_RATE_LIMITS:' + #gameId)")
    public ResponseEntity<RateLimitEntity> getRateLimit(
            @PathVariable String ruleId,
            @RequestParam String gameId) {
        RateLimitEntity rule = rateLimitService.getRateLimit(ruleId);
        return ResponseEntity.ok(rule);
    }

    /**
     * 获取游戏的限流规则
     */
    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('VIEW_RATE_LIMITS:' + #gameId)")
    public ResponseEntity<List<RateLimitEntity>> getRateLimits(@PathVariable String gameId) {
        List<RateLimitEntity> rules = rateLimitService.getRateLimits(gameId);
        return ResponseEntity.ok(rules);
    }

    /**
     * 更新限流规则
     */
    @PutMapping("/{ruleId}")
    @PreAuthorize("hasAuthority('MANAGE_RATE_LIMITS:' + #gameId)")
    public ResponseEntity<RateLimitEntity> updateRateLimit(
            @PathVariable String ruleId,
            @RequestParam String gameId,
            @RequestBody UpdateRequest request) {
        RateLimitEntity rule = rateLimitService.updateRateLimit(
            ruleId,
            request.limit,
            request.burst,
            request.enabled
        );
        return ResponseEntity.ok(rule);
    }

    /**
     * 删除限流规则
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasAuthority('MANAGE_RATE_LIMITS:' + #gameId)")
    public ResponseEntity<Void> deleteRateLimit(
            @PathVariable String ruleId,
            @RequestParam String gameId) {
        rateLimitService.deleteRateLimit(ruleId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取限流统计
     */
    @GetMapping("/stats/{gameId}")
    @PreAuthorize("hasAuthority('VIEW_RATE_LIMITS:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getRateLimitStats(@PathVariable String gameId) {
        Map<String, Object> stats = rateLimitService.getRateLimitStats(gameId);
        return ResponseEntity.ok(stats);
    }

    // ============== Quota Endpoints ==============

    /**
     * 创建配额
     */
    @PostMapping("/quotas")
    @PreAuthorize("hasAuthority('MANAGE_QUOTAS:' + #request.gameId)")
    public ResponseEntity<QuotaEntity> createQuota(@RequestBody QuotaRequest request) {
        QuotaEntity quota = rateLimitService.createQuota(
            request.gameId,
            request.environmentId,
            request.resourceType,
            request.limit,
            request.warningThreshold,
            request.alertThreshold,
            request.hardLimit,
            request.createdBy
        );
        return ResponseEntity.ok(quota);
    }

    /**
     * 检查配额
     */
    @GetMapping("/quotas/check")
    @PreAuthorize("hasAuthority('VIEW_QUOTAS:' + #gameId)")
    public ResponseEntity<RateLimitService.QuotaCheckResult> checkQuota(
            @RequestParam String gameId,
            @RequestParam(required = false) String environmentId,
            @RequestParam QuotaEntity.ResourceType resourceType) {
        RateLimitService.QuotaCheckResult result = rateLimitService.checkQuota(gameId, environmentId, resourceType);
        return ResponseEntity.ok(result);
    }

    /**
     * 更新配额使用量
     */
    @PostMapping("/quotas/update-usage")
    @PreAuthorize("hasAuthority('UPDATE_QUOTA_USAGE:' + #gameId)")
    public ResponseEntity<Void> updateQuotaUsage(
            @RequestParam String gameId,
            @RequestParam(required = false) String environmentId,
            @RequestParam QuotaEntity.ResourceType resourceType,
            @RequestParam long amount) {
        rateLimitService.updateQuotaUsage(gameId, environmentId, resourceType, amount);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取配额统计
     */
    @GetMapping("/quotas/stats/{gameId}")
    @PreAuthorize("hasAuthority('VIEW_QUOTAS:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getQuotaStats(@PathVariable String gameId) {
        Map<String, Object> stats = rateLimitService.getQuotaStats(gameId);
        return ResponseEntity.ok(stats);
    }

    // Request DTOs

    public static class RateLimitRequest {
        public String gameId;
        public String apiKeyId;
        public String endpoint;
        public String userId;
        public RateLimitEntity.Scope scope;
        public Integer limit;
        public RateLimitEntity.WindowType windowType;
        public Integer windowSize;
        public RateLimitEntity.Algorithm algorithm;
        public Integer burst;
        public String description;
        public String createdBy;
    }

    public static class UpdateRequest {
        public Integer limit;
        public Integer burst;
        public Boolean enabled;
    }

    public static class QuotaRequest {
        public String gameId;
        public String environmentId;
        public QuotaEntity.ResourceType resourceType;
        public Long limit;
        public Double warningThreshold;
        public Double alertThreshold;
        public Boolean hardLimit;
        public String createdBy;
    }
}
