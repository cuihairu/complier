package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportExecutionRepo extends JpaRepository<ReportExecutionEntity, String> {

    /**
     * 根据报表查找执行记录
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.reportId = :reportId ORDER BY re.createdAt DESC")
    List<ReportExecutionEntity> findByReportId(@Param("reportId") String reportId);

    /**
     * 根据游戏查找执行记录
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.gameId = :gameId ORDER BY re.createdAt DESC")
    List<ReportExecutionEntity> findByGameId(@Param("gameId") String gameId);

    /**
     * 查找待执行的记录
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.executionStatus = 'PENDING' ORDER BY re.createdAt ASC")
    List<ReportExecutionEntity> findPending();

    /**
     * 查找运行中的记录
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.executionStatus = 'RUNNING' ORDER BY re.startTime ASC")
    List<ReportExecutionEntity> findRunning();

    /**
     * 查找失败的记录
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.executionStatus IN ('FAILED', 'TIMEOUT') ORDER BY re.createdAt DESC")
    List<ReportExecutionEntity> findFailed();

    /**
     * 根据时间范围查找
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.reportId = :reportId AND re.createdAt >= :startTime AND re.createdAt <= :endTime ORDER BY re.createdAt DESC")
    List<ReportExecutionEntity> findByReportIdAndTimeRange(@Param("reportId") String reportId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计报表的执行次数
     */
    @Query("SELECT COUNT(re) FROM ReportExecutionEntity re WHERE re.reportId = :reportId")
    long countByReportId(@Param("reportId") String reportId);

    /**
     * 统计成功执行的次数
     */
    @Query("SELECT COUNT(re) FROM ReportExecutionEntity re WHERE re.reportId = :reportId AND re.executionStatus = 'COMPLETED'")
    long countSuccessByReportId(@Param("reportId") String reportId);

    /**
     * 统计平均执行时间
     */
    @Query("SELECT AVG(re.executionTimeMs) FROM ReportExecutionEntity re WHERE re.reportId = :reportId AND re.executionStatus = 'COMPLETED'")
    Double averageExecutionTime(@Param("reportId") String reportId);

    /**
     * 删除过期记录
     */
    @Query("DELETE FROM ReportExecutionEntity re WHERE re.createdAt < :expireAt")
    int deleteExpired(@Param("expireAt") LocalDateTime expireAt);

    /**
     * 查找最近的执行记录
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.gameId = :gameId AND re.executionStatus = 'COMPLETED' ORDER BY re.createdAt DESC")
    List<ReportExecutionEntity> findRecentCompleted(@Param("gameId") String gameId);

    /**
     * 统计总行数
     */
    @Query("SELECT SUM(re.rowCount) FROM ReportExecutionEntity re WHERE re.reportId = :reportId AND re.executionStatus = 'COMPLETED'")
    Long sumRowCountByReportId(@Param("reportId") String reportId);

    /**
     * 查找超时的执行记录
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.executionStatus = 'RUNNING' AND re.startTime < :timeout")
    List<ReportExecutionEntity> findTimeout(@Param("timeout") LocalDateTime timeout);

    /**
     * 根据触发类型查找
     */
    @Query("SELECT re FROM ReportExecutionEntity re WHERE re.reportId = :reportId AND re.triggerType = :triggerType ORDER BY re.createdAt DESC")
    List<ReportExecutionEntity> findByReportIdAndTriggerType(@Param("reportId") String reportId, @Param("triggerType") String triggerType);
}
