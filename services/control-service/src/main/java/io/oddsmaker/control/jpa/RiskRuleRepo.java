package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskRuleRepo extends JpaRepository<RiskRuleEntity, String> {

    /**
     * 查找游戏的所有风控规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND rr.deletedAt IS NULL ORDER BY rr.priority DESC, rr.createdAt")
    List<RiskRuleEntity> findByGameId(@Param("gameId") String gameId);

    /**
     * 查找游戏的活跃规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND rr.status = 'ACTIVE' AND rr.deletedAt IS NULL ORDER BY rr.priority DESC, rr.riskScore DESC")
    List<RiskRuleEntity> findActiveByGameId(@Param("gameId") String gameId);

    /**
     * 根据环境查找规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND (rr.environmentId = :environmentId OR rr.environmentId IS NULL) AND rr.deletedAt IS NULL ORDER BY rr.priority DESC")
    List<RiskRuleEntity> findByGameIdAndEnvironment(@Param("gameId") String gameId, @Param("environmentId") String environmentId);

    /**
     * 根据分类查找规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND rr.category = :category AND rr.deletedAt IS NULL ORDER BY rr.priority DESC")
    List<RiskRuleEntity> findByGameIdAndCategory(@Param("gameId") String gameId, @Param("category") RiskRuleEntity.RuleCategory category);

    /**
     * 根据风险等级查找规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND rr.riskLevel = :riskLevel AND rr.status = 'ACTIVE' AND rr.deletedAt IS NULL ORDER BY rr.priority DESC")
    List<RiskRuleEntity> findByGameIdAndRiskLevel(@Param("gameId") String gameId, @Param("riskLevel") RiskRuleEntity.RiskLevel riskLevel);

    /**
     * 查找自动封禁规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND rr.enableAutoBlock = true AND rr.status = 'ACTIVE' AND rr.deletedAt IS NULL ORDER BY rr.priority DESC")
    List<RiskRuleEntity> findAutoBlockRules(@Param("gameId") String gameId);

    /**
     * 查找需要审核的规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND rr.enableReviewQueue = true AND rr.status = 'ACTIVE' AND rr.deletedAt IS NULL ORDER BY rr.priority DESC")
    List<RiskRuleEntity> findReviewRequiredRules(@Param("gameId") String gameId);

    /**
     * 根据动作类型查找规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND rr.actionType = :actionType AND rr.status = 'ACTIVE' AND rr.deletedAt IS NULL")
    List<RiskRuleEntity> findByGameIdAndActionType(@Param("gameId") String gameId, @Param("actionType") RiskRuleEntity.ActionType actionType);

    /**
     * 搜索规则
     */
    @Query("SELECT rr FROM RiskRuleEntity rr WHERE rr.gameId = :gameId AND (rr.name LIKE %:query% OR rr.displayName LIKE %:query% OR rr.description LIKE %:query%) AND rr.deletedAt IS NULL")
    List<RiskRuleEntity> search(@Param("gameId") String gameId, @Param("query") String query);
}
