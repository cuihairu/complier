package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.BlockListEntity;
import io.oddsmaker.control.service.BlockListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 封禁名单API控制器
 * 提供封禁管理的API接口
 */
@RestController
@RequestMapping("/api/block-lists")
public class BlockListController {

    private static final Logger logger = LoggerFactory.getLogger(BlockListController.class);

    @Autowired
    private BlockListService blockListService;

    /**
     * 检查目标是否被封禁
     */
    @GetMapping("/check")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> checkBlock(
            @RequestParam String gameId,
            @RequestParam String targetType,
            @RequestParam String targetValue) {

        boolean blocked = blockListService.isBlocked(gameId, targetType, targetValue);
        return ResponseEntity.ok(Map.of(
            "blocked", blocked,
            "gameId", gameId,
            "targetType", targetType,
            "targetValue", targetValue
        ));
    }

    /**
     * 获取游戏的活跃封禁列表
     */
    @GetMapping("/active/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<BlockListEntity>> getActiveBlocks(@PathVariable String gameId) {
        List<BlockListEntity> blocks = blockListService.getActiveBlocks(gameId);
        return ResponseEntity.ok(blocks);
    }

    /**
     * 获取封禁详情
     */
    @GetMapping("/{blockId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<BlockListEntity> getBlock(@PathVariable String blockId) {
        BlockListEntity block = blockListService.getBlock(blockId);
        return ResponseEntity.ok(block);
    }

    /**
     * 添加封禁
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #request.gameId)")
    public ResponseEntity<BlockListEntity> addBlock(@RequestBody BlockRequest request) {
        BlockListEntity block = blockListService.addBlock(
            request.gameId,
            request.environmentId,
            request.targetType,
            request.targetValue,
            request.blockReason,
            request.blockCategory,
            request.blockType != null ? BlockListEntity.BlockType.valueOf(request.blockType) : BlockListEntity.BlockType.HARD,
            request.isPermanent != null ? request.isPermanent : false,
            request.durationMinutes,
            request.blockedBy,
            request.riskCaseId,
            request.notes
        );
        return ResponseEntity.ok(block);
    }

    /**
     * 解除封禁
     */
    @PostMapping("/{blockId}/unblock")
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #gameId)")
    public ResponseEntity<Void> unblock(
            @PathVariable String blockId,
            @RequestParam String gameId,
            @RequestBody UnblockRequest request) {
        blockListService.unblock(blockId, request.unblockedBy, request.reason);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量解除封禁
     */
    @PostMapping("/batch-unblock")
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #gameId)")
    public ResponseEntity<Map<String, Object>> batchUnblock(
            @RequestParam String gameId,
            @RequestBody BatchUnblockRequest request) {
        int count = blockListService.batchUnblock(request.blockIds, request.unblockedBy, request.reason);
        return ResponseEntity.ok(Map.of("unblocked", count));
    }

    /**
     * 从风险案例创建封禁
     */
    @PostMapping("/from-risk-case/{riskCaseId}")
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #gameId)")
    public ResponseEntity<BlockListEntity> createFromRiskCase(
            @PathVariable String riskCaseId,
            @RequestParam String gameId,
            @RequestParam String blockedBy) {
        BlockListEntity block = blockListService.createBlockFromRiskCase(riskCaseId, blockedBy);
        return ResponseEntity.ok(block);
    }

    /**
     * 获取封禁统计
     */
    @GetMapping("/stats/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> getBlockStats(@PathVariable String gameId) {
        Map<String, Object> stats = blockListService.getBlockStats(gameId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 搜索封禁
     */
    @GetMapping("/search/{gameId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<BlockListEntity>> searchBlocks(
            @PathVariable String gameId,
            @RequestParam String query) {
        List<BlockListEntity> blocks = blockListService.searchBlocks(gameId, query);
        return ResponseEntity.ok(blocks);
    }

    /**
     * 根据类型获取封禁列表
     */
    @GetMapping("/by-type/{gameId}/{targetType}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<List<BlockListEntity>> getBlocksByType(
            @PathVariable String gameId,
            @PathVariable String targetType) {
        List<BlockListEntity> blocks = blockListService.getBlocksByType(gameId, targetType);
        return ResponseEntity.ok(blocks);
    }

    // Request DTOs

    public static class BlockRequest {
        public String gameId;
        public String environmentId;
        public String targetType;  // device_id, user_id, player_id, ip, ip_range, account_id
        public String targetValue;
        public String blockReason;
        public String blockCategory;  // fraud, cheating, abuse, tos_violation, security
        public String blockType;      // HARD, SOFT, TEMPORARY, SHADOW
        public Boolean isPermanent;
        public Integer durationMinutes;
        public String blockedBy;
        public String riskCaseId;
        public String notes;
    }

    public static class UnblockRequest {
        public String unblockedBy;
        public String reason;
    }

    public static class BatchUnblockRequest {
        public List<String> blockIds;
        public String unblockedBy;
        public String reason;
    }
}
