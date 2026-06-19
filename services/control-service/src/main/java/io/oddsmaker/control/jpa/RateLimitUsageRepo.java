package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RateLimitUsageRepo extends JpaRepository<RateLimitUsageEntity, String> {

    /**
     * 查找活跃的窗口
     */
    @Query("SELECT u FROM RateLimitUsageEntity u WHERE u.rateLimitId = :rateLimitId AND u.windowStart <= :now AND u.windowEnd > :now")
    Optional<RateLimitUsageEntity> findActiveWindow(@Param("rateLimitId") String rateLimitId, @Param("now") LocalDateTime now);

    /**
     * 查找过期的窗口
     */
    @Query("SELECT u FROM RateLimitUsageEntity u WHERE u.windowEnd < :now")
    List<RateLimitUsageEntity> findExpired(@Param("now") LocalDateTime now);

    /**
     * 根据限流规则查找
     */
    @Query("SELECT u FROM RateLimitUsageEntity u WHERE u.rateLimitId = :rateLimitId ORDER BY u.windowStart DESC")
    List<RateLimitUsageEntity> findByRateLimitId(@Param("rateLimitId") String rateLimitId);

    /**
     * 根据游戏查找
     */
    @Query("SELECT u FROM RateLimitUsageEntity u WHERE u.gameId = :gameId ORDER BY u.windowStart DESC")
    List<RateLimitUsageEntity> findByGameId(@Param("gameId") String gameId);

    /**
     * 根据API密钥查找
     */
    @Query("SELECT u FROM RateLimitUsageEntity u WHERE u.apiKeyId = :apiKeyId ORDER BY u.windowStart DESC")
    List<RateLimitUsageEntity> findByApiKeyId(@Param("apiKeyId") String apiKeyId);

    /**
     * 删除过期窗口
     */
    @Query("DELETE FROM RateLimitUsageEntity u WHERE u.windowEnd < :expireBefore")
    int deleteExpired(@Param("expireBefore") LocalDateTime expireBefore);
}
