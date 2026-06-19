package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ML模型仓库接口
 */
@Repository
public interface MLModelRepo extends JpaRepository<MLModelEntity, String> {

    /**
     * 根据游戏ID查找模型
     */
    List<MLModelEntity> findByGameIdAndDeletedAtIsNull(String gameId);

    /**
     * 根据模型名称查找
     */
    Optional<MLModelEntity> findByModelNameAndGameIdAndDeletedAtIsNull(String modelName, String gameId);

    /**
     * 根据状态查找模型
     */
    List<MLModelEntity> findByModelStatusAndDeletedAtIsNull(MLModelEntity.ModelStatus status);

    /**
     * 根据模型类型查找
     */
    List<MLModelEntity> findByModelTypeAndDeletedAtIsNull(MLModelEntity.ModelType modelType);

    /**
     * 根据游戏ID和状态查找
     */
    List<MLModelEntity> findByGameIdAndModelStatusAndDeletedAtIsNull(String gameId, MLModelEntity.ModelStatus status);

    /**
     * 查找已部署的模型
     */
    @Query("SELECT m FROM MLModelEntity m WHERE m.modelStatus = 'DEPLOYED' AND m.deletedAt IS NULL")
    List<MLModelEntity> findAllDeployed();

    /**
     * 根据游戏ID查找已部署的模型
     */
    @Query("SELECT m FROM MLModelEntity m WHERE m.gameId = :gameId AND m.modelStatus = 'DEPLOYED' AND m.deletedAt IS NULL")
    List<MLModelEntity> findDeployedByGameId(@Param("gameId") String gameId);

    /**
     * 查找A/B测试模型
     */
    @Query("SELECT m FROM MLModelEntity m WHERE m.isAbTest = true AND m.deletedAt IS NULL")
    List<MLModelEntity> findAllAbTestModels();

    /**
     * 查找需要重训练的模型
     * 根据重训练策略和最后训练时间
     */
    @Query("SELECT m FROM MLModelEntity m WHERE m.modelStatus = 'DEPLOYED' AND m.deletedAt IS NULL AND m.lastTrainedAt < :threshold")
    List<MLModelEntity> findModelsNeedingRetrain(@Param("threshold") LocalDateTime threshold);

    /**
     * 统计各状态模型数量
     */
    @Query("SELECT m.modelStatus, COUNT(m) FROM MLModelEntity m WHERE m.deletedAt IS NULL GROUP BY m.modelStatus")
    List<Object[]> countByStatus();

    /**
     * 统计各类型模型数量
     */
    @Query("SELECT m.modelType, COUNT(m) FROM MLModelEntity m WHERE m.deletedAt IS NULL GROUP BY m.modelType")
    List<Object[]> countByType();

    /**
     * 查找最近训练的模型
     */
    @Query("SELECT m FROM MLModelEntity m WHERE m.deletedAt IS NULL AND m.lastTrainedAt IS NOT NULL ORDER BY m.lastTrainedAt DESC")
    List<MLModelEntity> findRecentlyTrained();

    /**
     * 根据框架查找模型
     */
    List<MLModelEntity> findByFrameworkAndDeletedAtIsNull(String framework);

    /**
     * 根据创建者查找模型
     */
    List<MLModelEntity> findByCreatedByAndDeletedAtIsNull(String createdBy);

    /**
     * 查找高预测量模型
     */
    @Query("SELECT m FROM MLModelEntity m WHERE m.deletedAt IS NULL AND m.predictionCount > :threshold ORDER BY m.predictionCount DESC")
    List<MLModelEntity> findHighVolumeModels(@Param("threshold") Long threshold);

    /**
     * 检查模型名称是否存在
     */
    boolean existsByModelNameAndGameIdAndDeletedAtIsNull(String modelName, String gameId);
}
