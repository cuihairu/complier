package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PipelineRepo extends JpaRepository<PipelineEntity, String> {

    /**
     * 查找游戏的管道
     */
    @Query("SELECT p FROM PipelineEntity p WHERE p.gameId = :gameId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PipelineEntity> findByGameId(@Param("gameId") String gameId);

    /**
     * 查找活跃的管道
     */
    @Query("SELECT p FROM PipelineEntity p WHERE p.pipelineStatus = 'ACTIVE' AND p.enabled = true AND p.deletedAt IS NULL")
    List<PipelineEntity> findActive();

    /**
     * 查找游戏的环境管道
     */
    @Query("SELECT p FROM PipelineEntity p WHERE p.gameId = :gameId AND p.environmentId = :environmentId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PipelineEntity> findByGameAndEnvironment(@Param("gameId") String gameId, @Param("environmentId") String environmentId);

    /**
     * 根据类型查找管道
     */
    @Query("SELECT p FROM PipelineEntity p WHERE p.pipelineType = :type AND p.deletedAt IS NULL ORDER BY p.priority DESC")
    List<PipelineEntity> findByType(@Param("type") PipelineEntity.PipelineType type);

    /**
     * 查找失败的管道
     */
    @Query("SELECT p FROM PipelineEntity p WHERE p.pipelineStatus = 'FAILED' AND p.deletedAt IS NULL")
    List<PipelineEntity> findFailed();

    /**
     * 查找需要调度的管道
     */
    @Query("SELECT p FROM PipelineEntity p WHERE p.pipelineStatus = 'ACTIVE' AND p.enabled = true AND (p.lastRunAt IS NULL OR p.lastRunAt < :since) AND p.deletedAt IS NULL")
    List<PipelineEntity> findScheduledPipelines(@Param("since") LocalDateTime since);

    /**
     * 统计管道状态
     */
    @Query("SELECT COUNT(p) FROM PipelineEntity p WHERE p.pipelineStatus = :status AND p.deletedAt IS NULL")
    long countByStatus(@Param("status") PipelineEntity.PipelineStatus status);
}
