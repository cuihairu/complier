package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 遥测配置仓库接口
 */
@Repository
public interface TelemetryConfigRepo extends JpaRepository<TelemetryConfigEntity, String> {

    /**
     * 根据游戏ID查找配置
     */
    List<TelemetryConfigEntity> findByGameIdAndDeletedAtIsNull(String gameId);

    /**
     * 根据游戏ID和环境查找配置
     */
    List<TelemetryConfigEntity> findByGameIdAndEnvironmentIdAndDeletedAtIsNull(String gameId, String environmentId);

    /**
     * 根据配置类型查找
     */
    List<TelemetryConfigEntity> findByConfigTypeAndDeletedAtIsNull(TelemetryConfigEntity.ConfigType configType);

    /**
     * 根据状态查找
     */
    List<TelemetryConfigEntity> findByConfigStatusAndDeletedAtIsNull(TelemetryConfigEntity.ConfigStatus status);

    /**
     * 查找活跃的配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.configStatus = 'ACTIVE' AND c.deletedAt IS NULL")
    List<TelemetryConfigEntity> findAllActive();

    /**
     * 查找默认配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.isDefault = true AND c.deletedAt IS NULL")
    List<TelemetryConfigEntity> findAllDefaults();

    /**
     * 根据游戏ID查找默认配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.gameId = :gameId AND c.isDefault = true AND c.deletedAt IS NULL")
    List<TelemetryConfigEntity> findDefaultsByGameId(@Param("gameId") String gameId);

    /**
     * 查找全局配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.gameId IS NULL AND c.deletedAt IS NULL")
    List<TelemetryConfigEntity> findGlobalConfigs();

    /**
     * 查找活跃的全局配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.gameId IS NULL AND c.configStatus = 'ACTIVE' AND c.deletedAt IS NULL")
    List<TelemetryConfigEntity> findActiveGlobalConfigs();

    /**
     * 根据游戏ID和配置类型查找活跃配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.gameId = :gameId AND c.configType = :configType AND c.configStatus = 'ACTIVE' AND c.deletedAt IS NULL ORDER BY c.priority ASC")
    List<TelemetryConfigEntity> findActiveByGameIdAndType(
            @Param("gameId") String gameId,
            @Param("configType") TelemetryConfigEntity.ConfigType configType);

    /**
     * 根据游戏ID、环境和配置类型查找活跃配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.gameId = :gameId AND c.environmentId = :environmentId AND c.configType = :configType AND c.configStatus = 'ACTIVE' AND c.deletedAt IS NULL ORDER BY c.priority ASC")
    List<TelemetryConfigEntity> findActiveByGameIdAndEnvironmentIdAndType(
            @Param("gameId") String gameId,
            @Param("environmentId") String environmentId,
            @Param("configType") TelemetryConfigEntity.ConfigType configType);

    /**
     * 查找高优先级配置
     */
    @Query("SELECT c FROM TelemetryConfigEntity c WHERE c.deletedAt IS NULL ORDER BY c.priority ASC")
    List<TelemetryConfigEntity> findAllByPriority();

    /**
     * 统计各类型配置数量
     */
    @Query("SELECT c.configType, COUNT(c) FROM TelemetryConfigEntity c WHERE c.deletedAt IS NULL GROUP BY c.configType")
    List<Object[]> countByType();

    /**
     * 统计各状态配置数量
     */
    @Query("SELECT c.configStatus, COUNT(c) FROM TelemetryConfigEntity c WHERE c.deletedAt IS NULL GROUP BY c.configStatus")
    List<Object[]> countByStatus();

    /**
     * 检查配置名称是否存在
     */
    boolean existsByConfigNameAndGameIdAndDeletedAtIsNull(String configName, String gameId);

    /**
     * 根据优先级范围查找
     */
    List<TelemetryConfigEntity> findByPriorityBetweenAndDeletedAtIsNull(Integer minPriority, Integer maxPriority);
}
