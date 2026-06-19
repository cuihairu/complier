package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 封禁名单实体
 * 用于网关层强制拦截已封禁的实体
 */
@Entity
@Table(name = "block_lists")
public class BlockListEntity {

    @Id
    @Column(length = 32)
    public String id;

    // 关联信息
    @Column(name = "game_id", nullable = false, length = 32)
    public String gameId;

    @Column(name = "environment_id", length = 32)
    public String environmentId;

    // 关联的风险案例（可选）
    @Column(name = "risk_case_id", length = 32)
    public String riskCaseId;

    // 封禁目标
    @Column(name = "target_type", nullable = false, length = 50)
    public String targetType;  // device_id, user_id, player_id, ip, ip_range, account_id

    @Column(name = "target_value", nullable = false, length = 500)
    public String targetValue;  // 实际的封禁值

    @Column(name = "target_name", length = 200)
    public String targetName;  // 目标名称（用于显示）

    // 封禁原因
    @Column(name = "block_reason", length = 500)
    public String blockReason;  // 封禁原因

    @Column(name = "risk_level", length = 20)
    public String riskLevel;  // 风险等级：LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "block_category", length = 50)
    public String blockCategory;  // 封禁分类：fraud, cheating, abuse, tos_violation, security

    // 封禁类型
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public BlockType blockType = BlockType.HARD;  // 封禁类型

    @Column(name = "is_permanent")
    public Boolean isPermanent = false;  // 是否永久封禁

    @Column(name = "expires_at")
    public LocalDateTime expiresAt;  // 过期时间

    // 封禁范围
    @Column(name = "block_scope", length = 50)
    public String blockScope;  // 封禁范围：all, login, game, payment, chat, trade

    // 操作者信息
    @Column(name = "blocked_by", length = 64)
    public String blockedBy;  // 封禁人

    @Column(name = "blocked_at")
    public LocalDateTime blockedAt;  // 封禁时间

    // 解除封禁
    @Column(name = "unblocked_by", length = 64)
    public String unblockedBy;  // 解除人

    @Column(name = "unblocked_at")
    public LocalDateTime unblockedAt;  // 解除时间

    @Column(name = "unblock_reason", length = 500)
    public String unblockReason;  // 解除原因

    // 统计信息
    @Column(name = "hit_count")
    public Long hitCount = 0L;  // 命中次数

    @Column(name = "last_hit_at")
    public LocalDateTime lastHitAt;  // 最后命中时间

    // 备注
    @Column(name = "notes", columnDefinition = "TEXT")
    public String notes;  // 备注信息

    // 时间戳
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    public LocalDateTime deletedAt;  // 软删除时间

    public enum BlockType {
        HARD,              // 硬封禁：完全拒绝访问
        SOFT,              // 软封禁：限制功能
        TEMPORARY,         // 临时封禁：短时间限制
        SHADOW             // 影子封禁：表面正常但实际限制
    }

    // 业务方法
    public boolean isActive() {
        if (deletedAt != null) return false;
        if (unblockedAt != null) return false;
        if (Boolean.TRUE.equals(isPermanent)) return true;
        if (expiresAt == null) return true;
        return expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isExpired() {
        if (Boolean.TRUE.equals(isPermanent)) return false;
        if (expiresAt == null) return false;
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public long getRemainingMinutes() {
        if (Boolean.TRUE.equals(isPermanent)) return -1;  // 永久
        if (expiresAt == null) return 0;
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt.isBefore(now)) return 0;
        return java.time.temporal.ChronoUnit.MINUTES.between(now, expiresAt);
    }

    public void recordHit() {
        hitCount = (hitCount == null ? 0 : hitCount) + 1;
        lastHitAt = LocalDateTime.now();
    }

    public void unblock(String by, String reason) {
        unblockedBy = by;
        unblockedAt = LocalDateTime.now();
        unblockReason = reason;
    }

    public boolean isHardBlock() {
        return blockType == BlockType.HARD;
    }

    public boolean isSoftBlock() {
        return blockType == BlockType.SOFT;
    }

    public boolean isShadowBlock() {
        return blockType == BlockType.SHADOW;
    }

    public String getBlockDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(blockType.name().toLowerCase()).append(" ");
        sb.append(targetType).append(": ").append(targetValue);
        if (blockReason != null) {
            sb.append(" - ").append(blockReason);
        }
        if (Boolean.TRUE.equals(isPermanent)) {
            sb.append(" (permanent)");
        } else if (expiresAt != null) {
            sb.append(" (expires: ").append(expiresAt).append(")");
        }
        return sb.toString();
    }
}
