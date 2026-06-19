package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SDK密钥仓库接口
 */
@Repository
public interface SDKKeyRepo extends JpaRepository<SDKKeyEntity, String> {

    /**
     * 根据游戏ID查找SDK密钥
     */
    List<SDKKeyEntity> findByGameIdAndDeletedAtIsNull(String gameId);

    /**
     * 根据游戏ID和环境查找SDK密钥
     */
    List<SDKKeyEntity> findByGameIdAndEnvironmentAndDeletedAtIsNull(String gameId, String environment);

    /**
     * 根据游戏ID和平台查找SDK密钥
     */
    List<SDKKeyEntity> findByGameIdAndPlatformAndDeletedAtIsNull(String gameId, SDKKeyEntity.SDKPlatform platform);

    /**
     * 根据公钥查找
     */
    Optional<SDKKeyEntity> findByPublicKeyAndDeletedAtIsNull(String publicKey);

    /**
     * 根据状态查找
     */
    List<SDKKeyEntity> findByKeyStatusAndDeletedAtIsNull(SDKKeyEntity.KeyStatus status);

    /**
     * 查找活跃的SDK密钥
     */
    @Query("SELECT k FROM SDKKeyEntity k WHERE k.keyStatus = 'ACTIVE' AND k.deletedAt IS NULL AND (k.expiresAt IS NULL OR k.expiresAt > CURRENT_TIMESTAMP)")
    List<SDKKeyEntity> findAllActive();

    /**
     * 根据游戏ID查找活跃的SDK密钥
     */
    @Query("SELECT k FROM SDKKeyEntity k WHERE k.gameId = :gameId AND k.keyStatus = 'ACTIVE' AND k.deletedAt IS NULL AND (k.expiresAt IS NULL OR k.expiresAt > CURRENT_TIMESTAMP)")
    List<SDKKeyEntity> findActiveByGameId(@Param("gameId") String gameId);

    /**
     * 查找过期的SDK密钥
     */
    @Query("SELECT k FROM SDKKeyEntity k WHERE k.keyStatus = 'ACTIVE' AND k.expiresAt IS NOT NULL AND k.expiresAt < CURRENT_TIMESTAMP AND k.deletedAt IS NULL")
    List<SDKKeyEntity> findExpired();

    /**
     * 统计各平台SDK密钥数量
     */
    @Query("SELECT k.platform, COUNT(k) FROM SDKKeyEntity k WHERE k.deletedAt IS NULL GROUP BY k.platform")
    List<Object[]> countByPlatform();

    /**
     * 统计各状态SDK密钥数量
     */
    @Query("SELECT k.keyStatus, COUNT(k) FROM SDKKeyEntity k WHERE k.deletedAt IS NULL GROUP BY k.keyStatus")
    List<Object[]> countByStatus();

    /**
     * 查找高事件量的SDK密钥
     */
    @Query("SELECT k FROM SDKKeyEntity k WHERE k.deletedAt IS NULL AND k.totalEventsSent > :threshold ORDER BY k.totalEventsSent DESC")
    List<SDKKeyEntity> findHighVolumeKeys(@Param("threshold") Long threshold);

    /**
     * 查找有错误的SDK密钥
     */
    @Query("SELECT k FROM SDKKeyEntity k WHERE k.deletedAt IS NULL AND k.totalErrors > 0 ORDER BY k.totalErrors DESC")
    List<SDKKeyEntity> findKeysWithErrors();

    /**
     * 检查公钥是否存在
     */
    boolean existsByPublicKeyAndDeletedAtIsNull(String publicKey);

    /**
     * 根据交付模式查找
     */
    List<SDKKeyEntity> findByDeliveryModeAndDeletedAtIsNull(SDKKeyEntity.DeliveryMode deliveryMode);

    /**
     * 删除指定游戏的所有SDK密钥
     */
    void deleteByGameId(String gameId);
}
