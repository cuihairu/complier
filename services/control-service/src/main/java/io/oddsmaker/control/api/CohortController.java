package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.CohortEntity;
import io.oddsmaker.control.service.CohortService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 同期群API控制器
 * 提供同期群分析的API接口
 */
@RestController
@RequestMapping("/api/cohorts")
public class CohortController {

    @Autowired
    private CohortService cohortService;

    /**
     * 创建同期群
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ANALYZE_DATA:' + #request.gameId)")
    public ResponseEntity<CohortEntity> createCohort(@RequestBody CohortRequest request) {
        CohortEntity cohort = cohortService.createCohort(
            request.gameId,
            request.environmentId,
            request.name,
            request.displayName,
            request.description,
            request.cohortType != null ? CohortEntity.CohortType.valueOf(request.cohortType) : null,
            request.startDate,
            request.endDate,
            request.timeUnit,
            request.analysisType,
            request.metricType,
            request.behaviorDefinition,
            request.retentionPeriods,
            request.createdBy
        );
        return ResponseEntity.ok(cohort);
    }

    /**
     * 获取同期群详情
     */
    @GetMapping("/{cohortId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<CohortEntity> getCohort(
            @PathVariable String cohortId,
            @RequestParam String gameId) {
        CohortEntity cohort = cohortService.getCohort(cohortId);
        return ResponseEntity.ok(cohort);
    }

    /**
     * 获取游戏的同期群列表
     */
    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<CohortEntity>> getGameCohorts(@PathVariable String gameId) {
        List<CohortEntity> cohorts = cohortService.getGameCohorts(gameId);
        return ResponseEntity.ok(cohorts);
    }

    /**
     * 获取已完成的同期群
     */
    @GetMapping("/completed/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<CohortEntity>> getCompletedCohorts(@PathVariable String gameId) {
        List<CohortEntity> cohorts = cohortService.getCompletedCohorts(gameId);
        return ResponseEntity.ok(cohorts);
    }

    /**
     * 计算同期群
     */
    @PostMapping("/{cohortId}/calculate")
    @PreAuthorize("hasAuthority('ANALYZE_DATA:' + #gameId)")
    public ResponseEntity<CohortEntity> calculateCohort(
            @PathVariable String cohortId,
            @RequestParam String gameId) {
        CohortEntity cohort = cohortService.calculateCohort(cohortId);
        return ResponseEntity.ok(cohort);
    }

    /**
     * 获取同期群结果
     */
    @GetMapping("/{cohortId}/results")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getCohortResults(
            @PathVariable String cohortId,
            @RequestParam String gameId) {
        Map<String, Object> results = cohortService.getCohortResults(cohortId);
        return ResponseEntity.ok(results);
    }

    /**
     * 获取同期群统计
     */
    @GetMapping("/stats/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getCohortStats(@PathVariable String gameId) {
        Map<String, Object> stats = cohortService.getCohortStats(gameId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 搜索同期群
     */
    @GetMapping("/search/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<CohortEntity>> searchCohorts(
            @PathVariable String gameId,
            @RequestParam String query) {
        List<CohortEntity> cohorts = cohortService.searchCohorts(gameId, query);
        return ResponseEntity.ok(cohorts);
    }

    /**
     * 获取最近的同期群
     */
    @GetMapping("/recent/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<CohortEntity>> getRecentCohorts(@PathVariable String gameId) {
        List<CohortEntity> cohorts = cohortService.getRecentCohorts(gameId);
        return ResponseEntity.ok(cohorts);
    }

    // Request DTOs

    public static class CohortRequest {
        public String gameId;
        public String environmentId;
        public String name;
        public String displayName;
        public String description;
        public String cohortType;
        public LocalDate startDate;
        public LocalDate endDate;
        public String timeUnit;
        public String analysisType;
        public String metricType;
        public Map<String, Object> behaviorDefinition;
        public List<Integer> retentionPeriods;
        public String createdBy;
    }
}
