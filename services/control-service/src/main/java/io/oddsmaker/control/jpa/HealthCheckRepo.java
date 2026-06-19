package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthCheckRepo extends JpaRepository<HealthCheckEntity, String> {

    /**
     * 查找启用的健康检查
     */
    @Query("SELECT hc FROM HealthCheckEntity hc WHERE hc.enabled = true ORDER BY hc.checkName")
    List<HealthCheckEntity> findEnabled();

    /**
     * 查找需要检查的项
     */
    @Query("SELECT hc FROM HealthCheckEntity hc WHERE hc.enabled = true AND (hc.lastCheckedAt IS NULL OR hc.lastCheckedAt < :since)")
    List<HealthCheckEntity> findDueChecks(@Param("since") LocalDateTime since);

    /**
     * 查找不健康的检查
     */
    @Query("SELECT hc FROM HealthCheckEntity hc WHERE hc.healthStatus IN ('UNHEALTHY', 'DOWN') AND hc.enabled = true ORDER BY hc.lastUnhealthyAt DESC")
    List<HealthCheckEntity> findUnhealthy();

    /**
     * 根据类型查找
     */
    @Query("SELECT hc FROM HealthCheckEntity hc WHERE hc.checkType = :type AND hc.enabled = true")
    List<HealthCheckEntity> findByType(@Param("type") HealthCheckEntity.CheckType type);

    /**
     * 根据名称查找
     */
    @Query("SELECT hc FROM HealthCheckEntity hc WHERE hc.checkName = :name")
    Optional<HealthCheckEntity> findByName(@Param("name") String name);

    /**
     * 查找慢响应的检查
     */
    @Query("SELECT hc FROM HealthCheckEntity hc WHERE hc.responseTimeMs > hc.warningThresholdMs AND hc.enabled = true")
    List<HealthCheckEntity> findSlowResponses();

    /**
     * 统计健康状态
     */
    @Query("SELECT COUNT(hc) FROM HealthCheckEntity hc WHERE hc.healthStatus = :status AND hc.enabled = true")
    long countByStatus(@Param("status") HealthCheckEntity.HealthStatus status);
}
