package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 模型预测记录仓库接口
 */
@Repository
public interface ModelPredictionRepo extends JpaRepository<MLModelPredictionEntity, String> {

    /**
     * 根据模型ID查找预测记录
     */
    List<MLModelPredictionEntity> findByModelIdOrderByCreatedAtDesc(String modelId);

    /**
     * 根据游戏ID查找预测记录
     */
    List<MLModelPredictionEntity> findByGameIdOrderByCreatedAtDesc(String gameId);

    /**
     * 根据状态查找预测记录
     */
    List<MLModelPredictionEntity> findByPredictionStatus(MLModelPredictionEntity.PredictionStatus status);

    /**
     * 根据请求ID查找预测记录
     */
    Optional<MLModelPredictionEntity> findByRequestId(String requestId);

    /**
     * 根据实体类型和ID查找预测记录
     */
    List<MLModelPredictionEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);

    /**
     * 根据批量ID查找预测记录
     */
    List<MLModelPredictionEntity> findByBatchIdOrderByCreatedAtDesc(String batchId);

    /**
     * 查找有反馈的预测记录
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.feedbackType IS NOT NULL AND p.modelId = :modelId")
    List<MLModelPredictionEntity> findWithFeedback(@Param("modelId") String modelId);

    /**
     * 查找正确的预测
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.feedbackType = 'CORRECT'")
    List<MLModelPredictionEntity> findCorrectPredictions(@Param("modelId") String modelId);

    /**
     * 查找错误的预测
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.feedbackType = 'INCORRECT'")
    List<MLModelPredictionEntity> findIncorrectPredictions(@Param("modelId") String modelId);

    /**
     * 统计各状态预测数量
     */
    @Query("SELECT p.predictionStatus, COUNT(p) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId GROUP BY p.predictionStatus")
    List<Object[]> countByStatus(@Param("modelId") String modelId);

    /**
     * 统计反馈类型分布
     */
    @Query("SELECT p.feedbackType, COUNT(p) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.feedbackType IS NOT NULL GROUP BY p.feedbackType")
    List<Object[]> countByFeedbackType(@Param("modelId") String modelId);

    /**
     * 计算平均延迟
     */
    @Query("SELECT AVG(p.latencyMs) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.latencyMs IS NOT NULL")
    Double calculateAverageLatency(@Param("modelId") String modelId);

    /**
     * 计算P95延迟
     */
    @Query("SELECT p.latencyMs FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.latencyMs IS NOT NULL ORDER BY p.latencyMs ASC")
    List<Integer> findLatenciesOrdered(@Param("modelId") String modelId);

    /**
     * 计算缓存命中率
     */
    @Query("SELECT COUNT(p) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.cacheHit = true")
    long countCacheHits(@Param("modelId") String modelId);

    /**
     * 查找A/B测试预测
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.isAbTest = true")
    List<MLModelPredictionEntity> findAbTestPredictions(@Param("modelId") String modelId);

    /**
     * 统计A/B测试组分布
     */
    @Query("SELECT p.abTestGroup, COUNT(p) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.isAbTest = true GROUP BY p.abTestGroup")
    List<Object[]> countByAbTestGroup(@Param("modelId") String modelId);

    /**
     * 查找时间范围内的预测记录
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.createdAt BETWEEN :startTime AND :endTime ORDER BY p.createdAt DESC")
    List<MLModelPredictionEntity> findByTimeRange(
            @Param("modelId") String modelId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的预测记录
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.modelId = :modelId ORDER BY p.createdAt DESC")
    List<MLModelPredictionEntity> findRecentByModelId(@Param("modelId") String modelId);

    /**
     * 统计每小时预测数量
     */
    @Query("SELECT FUNCTION('DATE', p.createdAt), FUNCTION('HOUR', p.createdAt), COUNT(p) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId GROUP BY FUNCTION('DATE', p.createdAt), FUNCTION('HOUR', p.createdAt) ORDER BY FUNCTION('DATE', p.createdAt), FUNCTION('HOUR', p.createdAt)")
    List<Object[]> countByHour(@Param("modelId") String modelId);

    /**
     * 查找失败的预测记录
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.predictionStatus = 'FAILED' ORDER BY p.createdAt DESC")
    List<MLModelPredictionEntity> findFailedPredictions(@Param("modelId") String modelId);

    /**
     * 统计错误代码分布
     */
    @Query("SELECT p.errorCode, COUNT(p) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.errorCode IS NOT NULL GROUP BY p.errorCode")
    List<Object[]> countByErrorCode(@Param("modelId") String modelId);

    /**
     * 查找客户端的预测记录
     */
    List<MLModelPredictionEntity> findByClientIdOrderByCreatedAtDesc(String clientId);

    /**
     * 查找金丝雀预测记录
     */
    @Query("SELECT p FROM MLModelPredictionEntity p WHERE p.modelId = :modelId AND p.isCanary = true")
    List<MLModelPredictionEntity> findCanaryPredictions(@Param("modelId") String modelId);

    /**
     * 删除指定模型的所有预测记录
     */
    void deleteByModelId(String modelId);

    /**
     * 删除指定时间之前的预测记录
     */
    void deleteByCreatedAtBefore(LocalDateTime threshold);

    /**
     * 统计总预测数量
     */
    @Query("SELECT COUNT(p) FROM MLModelPredictionEntity p WHERE p.modelId = :modelId")
    long countByModelId(@Param("modelId") String modelId);
}
