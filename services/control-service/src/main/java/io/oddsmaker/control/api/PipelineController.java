package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.*;
import io.oddsmaker.control.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管道API控制器
 * 提供数据管道管理的接口
 */
@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    @Autowired
    private PipelineService pipelineService;

    /**
     * 创建管道
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PIPELINES:' + #request.gameId)")
    public ResponseEntity<PipelineEntity> createPipeline(@RequestBody PipelineRequest request) {
        PipelineEntity pipeline = pipelineService.createPipeline(
            request.gameId,
            request.environmentId,
            request.pipelineName,
            request.pipelineType,
            request.description,
            request.sourceConfig,
            request.transformConfig,
            request.destinationConfig,
            request.createdBy
        );
        return ResponseEntity.ok(pipeline);
    }

    /**
     * 获取管道详情
     */
    @GetMapping("/{pipelineId}")
    @PreAuthorize("hasAuthority('VIEW_PIPELINES')")
    public ResponseEntity<PipelineEntity> getPipeline(@PathVariable String pipelineId) {
        PipelineEntity pipeline = pipelineService.getPipeline(pipelineId);
        return ResponseEntity.ok(pipeline);
    }

    /**
     * 获取游戏的管道列表
     */
    @GetMapping("/game/{gameId}")
    @PreAuthorize("hasAuthority('VIEW_PIPELINES:' + #gameId)")
    public ResponseEntity<List<PipelineEntity>> getPipelines(@PathVariable String gameId) {
        List<PipelineEntity> pipelines = pipelineService.getPipelines(gameId);
        return ResponseEntity.ok(pipelines);
    }

    /**
     * 获取管道任务
     */
    @GetMapping("/{pipelineId}/jobs")
    @PreAuthorize("hasAuthority('VIEW_PIPELINES')")
    public ResponseEntity<List<PipelineJobEntity>> getPipelineJobs(@PathVariable String pipelineId) {
        List<PipelineJobEntity> jobs = pipelineService.getPipelineJobs(pipelineId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * 获取管道统计
     */
    @GetMapping("/{pipelineId}/stats")
    @PreAuthorize("hasAuthority('VIEW_PIPELINES')")
    public ResponseEntity<Map<String, Object>> getPipelineStats(@PathVariable String pipelineId) {
        Map<String, Object> stats = pipelineService.getPipelineStats(pipelineId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 执行管道
     */
    @PostMapping("/{pipelineId}/execute")
    @PreAuthorize("hasAuthority('EXECUTE_PIPELINES')")
    public ResponseEntity<PipelineJobEntity> executePipeline(
            @PathVariable String pipelineId,
            @RequestBody ExecuteRequest request) {
        PipelineJobEntity job = pipelineService.executePipeline(pipelineId, request.triggeredBy);
        return ResponseEntity.ok(job);
    }

    /**
     * 激活管道
     */
    @PostMapping("/{pipelineId}/activate")
    @PreAuthorize("hasAuthority('MANAGE_PIPELINES')")
    public ResponseEntity<PipelineEntity> activatePipeline(@PathVariable String pipelineId) {
        PipelineEntity pipeline = pipelineService.activatePipeline(pipelineId);
        return ResponseEntity.ok(pipeline);
    }

    /**
     * 暂停管道
     */
    @PostMapping("/{pipelineId}/pause")
    @PreAuthorize("hasAuthority('MANAGE_PIPELINES')")
    public ResponseEntity<PipelineEntity> pausePipeline(@PathVariable String pipelineId) {
        PipelineEntity pipeline = pipelineService.pausePipeline(pipelineId);
        return ResponseEntity.ok(pipeline);
    }

    /**
     * 停止管道
     */
    @PostMapping("/{pipelineId}/stop")
    @PreAuthorize("hasAuthority('MANAGE_PIPELINES')")
    public ResponseEntity<PipelineEntity> stopPipeline(@PathVariable String pipelineId) {
        PipelineEntity pipeline = pipelineService.stopPipeline(pipelineId);
        return ResponseEntity.ok(pipeline);
    }

    // ============== Quality Rules Endpoints ==============

    /**
     * 创建质量规则
     */
    @PostMapping("/quality-rules")
    @PreAuthorize("hasAuthority('MANAGE_QUALITY_RULES:' + #request.gameId)")
    public ResponseEntity<DataQualityRuleEntity> createQualityRule(@RequestBody QualityRuleRequest request) {
        DataQualityRuleEntity rule = pipelineService.createQualityRule(
            request.gameId,
            request.pipelineId,
            request.ruleName,
            request.ruleType,
            request.severity,
            request.targetTable,
            request.targetColumn,
            request.ruleDefinition,
            request.createdBy
        );
        return ResponseEntity.ok(rule);
    }

    /**
     * 获取质量规则列表
     */
    @GetMapping("/quality-rules/game/{gameId}")
    @PreAuthorize("hasAuthority('VIEW_QUALITY_RULES:' + #gameId)")
    public ResponseEntity<List<DataQualityRuleEntity>> getQualityRules(@PathVariable String gameId) {
        List<DataQualityRuleEntity> rules = pipelineService.getQualityRules(gameId);
        return ResponseEntity.ok(rules);
    }

    /**
     * 获取管道的质量规则
     */
    @GetMapping("/{pipelineId}/quality-rules")
    @PreAuthorize("hasAuthority('VIEW_QUALITY_RULES')")
    public ResponseEntity<List<DataQualityRuleEntity>> getPipelineQualityRules(@PathVariable String pipelineId) {
        List<DataQualityRuleEntity> rules = pipelineService.getPipelineQualityRules(pipelineId);
        return ResponseEntity.ok(rules);
    }

    // Request DTOs

    public static class PipelineRequest {
        public String gameId;
        public String environmentId;
        public String pipelineName;
        public PipelineEntity.PipelineType pipelineType;
        public String description;
        public Map<String, Object> sourceConfig;
        public Map<String, Object> transformConfig;
        public Map<String, Object> destinationConfig;
        public String createdBy;
    }

    public static class ExecuteRequest {
        public String triggeredBy;
    }

    public static class QualityRuleRequest {
        public String gameId;
        public String pipelineId;
        public String ruleName;
        public DataQualityRuleEntity.RuleType ruleType;
        public DataQualityRuleEntity.Severity severity;
        public String targetTable;
        public String targetColumn;
        public Map<String, Object> ruleDefinition;
        public String createdBy;
    }
}
