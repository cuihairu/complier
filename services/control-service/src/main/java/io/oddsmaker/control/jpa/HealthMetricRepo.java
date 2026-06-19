package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HealthMetricRepo extends JpaRepository<HealthMetricEntity, String> {

    /**
     * 查找最近的指标
     */
    @Query("SELECT hm FROM HealthMetricEntity hm WHERE hm.metricType = :type AND hm.collectedAt >= :since ORDER BY hm.collectedAt DESC")
    List<HealthMetricEntity> findRecentByType(@Param("type") HealthMetricEntity.MetricType type, @Param("since") LocalDateTime since);

    /**
     * 根据来源查找
     */
    @Query("SELECT hm FROM HealthMetricEntity hm WHERE hm.source = :source ORDER BY hm.collectedAt DESC")
    List<HealthMetricEntity> findBySource(@Param("source") String source);

    /**
     * 查找异常指标
     */
    @Query("SELECT hm FROM HealthMetricEntity hm WHERE hm.isAnomaly = true AND hm.collectedAt >= :since ORDER BY hm.collectedAt DESC")
    List<HealthMetricEntity> findAnomalies(@Param("since") LocalDateTime since);

    /**
     * 查找警告级别的指标
     */
    @Query("SELECT hm FROM HealthMetricEntity hm WHERE hm.metricValue >= hm.warningThreshold AND hm.collectedAt >= :since ORDER BY hm.collectedAt DESC")
    List<HealthMetricEntity> findWarnings(@Param("since") LocalDateTime since);

    /**
     * 查找严重级别的指标
     */
    @Query("SELECT hm FROM HealthMetricEntity hm WHERE hm.metricValue >= hm.criticalThreshold AND hm.collectedAt >= :since ORDER BY hm.collectedAt DESC")
    List<HealthMetricEntity> findCritical(@Param("since") LocalDateTime since);

    /**
     * 删除过期指标
     */
    @Query("DELETE FROM HealthMetricEntity hm WHERE hm.collectedAt < :expireBefore")
    int deleteExpired(@Param("expireBefore") LocalDateTime expireBefore);

    /**
     * 计算平均指标值
     */
    @Query("SELECT AVG(hm.metricValue) FROM HealthMetricEntity hm WHERE hm.metricType = :type AND hm.collectedAt >= :since")
    Double averageByTypeSince(@Param("type") HealthMetricEntity.MetricType type, @Param("since") LocalDateTime since);
}
