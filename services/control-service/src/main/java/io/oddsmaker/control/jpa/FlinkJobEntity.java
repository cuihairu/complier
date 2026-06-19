package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Flink作业实体
 * 管理实时风险控制作业的配置和运行状态
 */
@Entity
@Table(name = "flink_jobs")
public class FlinkJobEntity {

    @Id
    @Column(length = 32)
    public String id;

    // 基本信息
    @Column(name = "game_id", nullable = false, length = 32)
    public String gameId;

    @Column(name = "environment_id", length = 32)
    public String environmentId;

    @Column(nullable = false, length = 100)
    public String name;  // 作业名称

    @Column(name = "display_name", length = 200)
    public String displayName;  // 显示名称

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;  // 作业描述

    @Column(name = "job_type", nullable = false, length = 50)
    public String jobType;  // 作业类型：risk_evaluation, fraud_detection, anomaly_detection

    // 作业配置
    @Column(name = "job_config", columnDefinition = "TEXT")
    public String jobConfig;  // JSON格式的作业配置

    @Column(name = "source_config", columnDefinition = "TEXT")
    public String sourceConfig;  // 数据源配置（Kafka topic等）

    @Column(name = "sink_config", columnDefinition = "TEXT")
    public String sinkConfig;  // 输出配置（告警、通知等）

    @Column(name = "parallelism")
    public Integer parallelism = 1;  // 并行度

    @Column(name = "checkpoint_interval")
    public Integer checkpointInterval = 60000;  // 检查点间隔（毫秒）

    // 风控规则配置
    @Column(name = "rule_ids", columnDefinition = "TEXT")
    public String ruleIds;  // 关联的风控规则ID列表（JSON数组）

    @Column(name = "evaluation_config", columnDefinition = "TEXT")
    public String evaluationConfig;  // 评估配置（窗口、聚合等）

    // 运行状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public JobStatus status = JobStatus.DRAFT;  // 作业状态

    @Column(name = "flink_job_id", length = 100)
    public String flinkJobId;  // Flink集群中的作业ID

    @Column(name = "flink_url", length = 500)
    public String flinkUrl;  // Flink Web UI URL

    @Column(name = "deployed_at")
    public LocalDateTime deployedAt;  // 部署时间

    @Column(name = "started_at")
    public LocalDateTime startedAt;  // 启动时间

    @Column(name = "stopped_at")
    public LocalDateTime stoppedAt;  // 停止时间

    // 运行统计
    @Column(name = "total_events_processed")
    public Long totalEventsProcessed = 0L;  // 处理的总事件数

    @Column(name = "total_risk_cases_created")
    public Long totalRiskCasesCreated = 0L;  // 创建的总风险案例数

    @Column(name = "total_actions_executed")
    public Long totalActionsExecuted = 0L;  // 执行的总动作数

    @Column(name = "last_metrics_update")
    public LocalDateTime lastMetricsUpdate;  // 最后指标更新时间

    // 错误信息
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;  // 错误信息

    @Column(name = "failure_count")
    public Integer failureCount = 0;  // 失败次数

    @Column(name = "last_failure_at")
    public LocalDateTime lastFailureAt;  // 最后失败时间

    // 版本控制
    @Column(name = "version", length = 20)
    public String version = "1.0";  // 版本号

    @Column(name = "parent_job_id", length = 32)
    public String parentJobId;  // 父作业ID（用于版本升级）

    // 操作信息
    @Column(name = "created_by", length = 64)
    public String createdBy;  // 创建人

    @Column(name = "updated_by", length = 64)
    public String updatedBy;  // 更新人

    // 时间戳
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    public LocalDateTime deletedAt;  // 软删除时间

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_job_id", insertable = false, updatable = false)
    public FlinkJobEntity parentJob;

    public enum JobStatus {
        DRAFT,              // 草稿
        DEPLOYING,          // 部署中
        RUNNING,            // 运行中
        STOPPING,           // 停止中
        STOPPED,            // 已停止
        FAILED,             // 失败
        PAUSED              // 暂停
    }

    public enum JobType {
        RISK_EVALUATION,    // 风险评估
        FRAUD_DETECTION,    // 欺诈检测
        ANOMALY_DETECTION,  // 异常检测
        REALTIME_AGGREGATION, // 实时聚合
        PATTERN_MATCHING   // 模式匹配
    }

    // 业务方法
    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }

    public boolean isStopped() {
        return status == JobStatus.STOPPED || status == JobStatus.FAILED;
    }

    public boolean canDeploy() {
        return status == JobStatus.DRAFT || status == JobStatus.STOPPED || status == JobStatus.FAILED;
    }

    public boolean canStop() {
        return status == JobStatus.RUNNING || status == JobStatus.DEPLOYING;
    }

    public void markAsDeployed(String flinkJobId, String flinkUrl) {
        this.status = JobStatus.RUNNING;
        this.flinkJobId = flinkJobId;
        this.flinkUrl = flinkUrl;
        this.deployedAt = LocalDateTime.now();
        this.startedAt = LocalDateTime.now();
    }

    public void markAsStopped() {
        this.status = JobStatus.STOPPED;
        this.stoppedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.failureCount = (this.failureCount == null ? 0 : this.failureCount) + 1;
        this.lastFailureAt = LocalDateTime.now();
    }

    public void updateMetrics(Long eventsProcessed, Long casesCreated, Long actionsExecuted) {
        this.totalEventsProcessed = eventsProcessed;
        this.totalRiskCasesCreated = casesCreated;
        this.totalActionsExecuted = actionsExecuted;
        this.lastMetricsUpdate = LocalDateTime.now();
    }

    public String getJobDescription() {
        return String.format("[%s] %s - %s (%s)",
            jobType,
            displayName != null ? displayName : name,
            status.name().toLowerCase(),
            environmentId != null ? environmentId : "global"
        );
    }

    public double getRiskCaseRate() {
        if (totalEventsProcessed == null || totalEventsProcessed == 0) return 0.0;
        if (totalRiskCasesCreated == null) return 0.0;
        return (double) totalRiskCasesCreated / totalEventsProcessed;
    }
}
