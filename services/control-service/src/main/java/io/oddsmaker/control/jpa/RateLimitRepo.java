package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateLimitRepo extends JpaRepository<RateLimitEntity, String> {

    /**
     * 查找全局启用的限流规则
     */
    @Query("SELECT rl FROM RateLimitEntity rl WHERE rl.scope = 'GLOBAL' AND rl.enabled = true AND rl.deletedAt IS NULL ORDER BY rl.priority DESC")
    List<RateLimitEntity> findGlobal();

    /**
     * 查找游戏的限流规则
     */
    @Query("SELECT rl FROM RateLimitEntity rl WHERE (rl.scope = 'GLOBAL' OR rl.gameId = :gameId) AND rl.enabled = true AND rl.deletedAt IS NULL ORDER BY rl.priority DESC")
    List<RateLimitEntity> findForGame(@Param("gameId") String gameId);

    /**
     * 查找API密钥的限流规则
     */
    @Query("SELECT rl FROM RateLimitEntity rl WHERE (rl.scope = 'GLOBAL' OR rl.gameId = :gameId OR rl.apiKeyId = :apiKeyId) AND rl.enabled = true AND rl.deletedAt IS NULL ORDER BY rl.priority DESC")
    List<RateLimitEntity> findForApiKey(@Param("gameId") String gameId, @Param("apiKeyId") String apiKeyId);

    /**
     * 查找端点的限流规则
     */
    @Query("SELECT rl FROM RateLimitEntity rl WHERE (rl.scope = 'GLOBAL' OR rl.scope = 'ENDPOINT') AND rl.endpoint = :endpoint AND rl.enabled = true AND rl.deletedAt IS NULL ORDER BY rl.priority DESC")
    List<RateLimitEntity> findForEndpoint(@Param("endpoint") String endpoint);

    /**
     * 查找用户的限流规则
     */
    @Query("SELECT rl FROM RateLimitEntity rl WHERE (rl.scope = 'GLOBAL' OR rl.scope = 'USER') AND rl.userId = :userId AND rl.enabled = true AND rl.deletedAt IS NULL ORDER BY rl.priority DESC")
    List<RateLimitEntity> findForUser(@Param("userId") String userId);

    /**
     * 根据游戏查找
     */
    @Query("SELECT rl FROM RateLimitEntity rl WHERE rl.gameId = :gameId AND rl.deletedAt IS NULL ORDER BY rl.createdAt DESC")
    List<RateLimitEntity> findByGameId(@Param("gameId") String gameId);

    /**
     * 根据API密钥查找
     */
    @Query("SELECT rl FROM RateLimitEntity rl WHERE rl.apiKeyId = :apiKeyId AND rl.deletedAt IS NULL ORDER BY rl.createdAt DESC")
    List<RateLimitEntity> findByApiKeyId(@Param("apiKeyId") String apiKeyId);

    /**
     * 统计游戏的限流规则数量
     */
    @Query("SELECT COUNT(rl) FROM RateLimitEntity rl WHERE rl.gameId = :gameId AND rl.deletedAt IS NULL")
    long countByGameId(@Param("gameId") String gameId);
}
