package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.jpa.AuditLogEntity;
import io.oddsmaker.control.jpa.AuditLogRepo;
import io.oddsmaker.control.jpa.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepo auditLogRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${oddsmaker.audit.retention-days:365}")
    private int retentionDays;

    /**
     * 记录审计日志
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditLogEntity.AuditAction action, String resourceType, String resourceId, String resourceName, String actionDescription, AuditLogEntity.AuditResult result, String userId, String userName, String userEmail, String clientIp, String userAgent, Map<String, Object> changes) {
        AuditLogEntity log = new AuditLogEntity();
        log.id = "al_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.resourceName = resourceName;
        log.actionDescription = actionDescription;
        log.result = result;
        log.userId = userId;
        log.userName = userName;
        log.userEmail = userEmail;
        log.clientIp = clientIp;
        log.userAgent = userAgent;
        log.createdAt = LocalDateTime.now();

        // 设置过期时间
        if (retentionDays > 0) {
            log.expireAt = LocalDateTime.now().plusDays(retentionDays);
        }

        // 序列化变更内容
        if (changes != null && !changes.isEmpty()) {
            try {
                log.changes = objectMapper.writeValueAsString(changes);
            } catch (Exception e) {
                logger.warn("Failed to serialize audit log changes", e);
                log.changes = "{}";
            }
        }

        auditLogRepo.save(log);

        // 如果是需要立即告警的事件，发送告警
        if (log.requiresImmediateAlert()) {
            sendImmediateAlert(log);
        }
    }

    /**
     * 记录成功的操作
     */
    public void logSuccess(AuditLogEntity.AuditAction action, String resourceType, String resourceId, String resourceName, String userId, String userName, String clientIp) {
        log(action, resourceType, resourceId, resourceName, null, AuditLogEntity.AuditResult.SUCCESS, userId, userName, null, clientIp, null, null);
    }

    /**
     * 记录失败的操作
     */
    public void logFailure(AuditLogEntity.AuditAction action, String resourceType, String resourceId, String resourceName, String errorMessage, String userId, String userName, String clientIp) {
        log(action, resourceType, resourceId, resourceName, errorMessage, AuditLogEntity.AuditResult.FAILURE, userId, userName, null, clientIp, null, null);
    }

    /**
     * 记录资源创建
     */
    public void logCreate(String resourceType, String resourceId, String resourceName, String userId, String userName, String clientIp, Map<String, Object> changes) {
        log(AuditLogEntity.AuditAction.CREATE, resourceType, resourceId, resourceName, null, AuditLogEntity.AuditResult.SUCCESS, userId, userName, null, clientIp, null, changes);
    }

    /**
     * 记录资源更新
     */
    public void logUpdate(String resourceType, String resourceId, String resourceName, String userId, String userName, String clientIp, Map<String, Object> changes) {
        log(AuditLogEntity.AuditAction.UPDATE, resourceType, resourceId, resourceName, null, AuditLogEntity.AuditResult.SUCCESS, userId, userName, null, clientIp, null, changes);
    }

    /**
     * 记录资源删除
     */
    public void logDelete(String resourceType, String resourceId, String resourceName, String userId, String userName, String clientIp) {
        log(AuditLogEntity.AuditAction.DELETE, resourceType, resourceId, resourceName, null, AuditLogEntity.AuditResult.SUCCESS, userId, userName, null, clientIp, null, null);
    }

    /**
     * 记录登录
     */
    public void logLogin(UserEntity user, String clientIp, String userAgent, boolean success) {
        AuditLogEntity.AuditResult result = success ? AuditLogEntity.AuditResult.SUCCESS : AuditLogEntity.AuditResult.FAILURE;
        log(AuditLogEntity.AuditAction.LOGIN, "user", user.id, user.name + " (" + user.email + ")",
            success ? "Login successful" : "Login failed", result,
            String.valueOf(user.id), user.name, user.email, clientIp, userAgent, null);
    }

    /**
     * 记录API密钥生成
     */
    public void logApiKeyGeneration(String apiKeyId, String gameName, String userId, String userName, String clientIp) {
        log(AuditLogEntity.AuditAction.API_KEY_GENERATE, "api_key", apiKeyId, gameName,
            "API key generated", AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null, null);
    }

    /**
     * 记录API密钥查看（敏感操作）
     */
    public void logApiKeyReveal(String apiKeyId, String gameName, String userId, String userName, String clientIp) {
        log(AuditLogEntity.AuditAction.API_KEY_REVEAL, "api_key", apiKeyId, gameName,
            "API key revealed (sensitive operation)", AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null, null);
    }

    /**
     * 记录数据导出
     */
    public void logDataExport(String resourceType, String resourceId, String resourceName, String userId, String userName, String clientIp) {
        log(AuditLogEntity.AuditAction.EXPORT_DATA, resourceType, resourceId, resourceName,
            "Data exported", AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null, null);
    }

    /**
     * 记录权限变更
     */
    public void logGrantPermission(String targetUserId, String targetUserName, String roleId, String roleName, String userId, String userName, String clientIp) {
        log(AuditLogEntity.AuditAction.GRANT, "role", roleId, roleName,
            "Granted role to user: " + targetUserName, AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null,
            Map.of("targetUserId", targetUserId, "targetUserName", targetUserName));
    }

    /**
     * 记录安全事件
     */
    public void logSecurityEvent(String eventType, String description, String userId, String userName, String clientIp, String userAgent, Map<String, Object> details) {
        AuditLogEntity log = new AuditLogEntity();
        log.id = "al_sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        log.action = AuditLogEntity.AuditAction.SECURITY_EVENT;
        log.resourceType = "security";
        log.resourceId = eventType;
        log.resourceName = eventType;
        log.actionDescription = description;
        log.result = AuditLogEntity.AuditResult.SUCCESS;
        log.userId = userId;
        log.userName = userName;
        log.clientIp = clientIp;
        log.userAgent = userAgent;
        log.createdAt = LocalDateTime.now();

        if (retentionDays > 0) {
            log.expireAt = LocalDateTime.now().plusDays(retentionDays);
        }

        if (details != null && !details.isEmpty()) {
            try {
                log.changes = objectMapper.writeValueAsString(details);
            } catch (Exception e) {
                logger.warn("Failed to serialize security event details", e);
                log.changes = "{}";
            }
        }

        auditLogRepo.save(log);

        // 安全事件总是需要立即告警
        sendImmediateAlert(log);
    }

    /**
     * 记录可疑活动
     */
    public void logSuspiciousActivity(String activityType, String description, String clientIp, String userAgent, Map<String, Object> context) {
        logSecurityEvent("SUSPICIOUS_" + activityType, description, null, null, clientIp, userAgent, context);
    }

    /**
     * 记录暴力破解尝试
     */
    public void logBruteForceAttempt(String targetResource, String clientIp, String userAgent, int attemptCount) {
        logSecurityEvent("BRUTE_FORCE", "Brute force attempt detected on " + targetResource,
            null, null, clientIp, userAgent,
            Map.of("targetResource", targetResource, "attemptCount", attemptCount));
    }

    /**
     * 记录异常访问模式
     */
    public void logAnomalousAccess(String pattern, String description, String clientIp, String userAgent, Map<String, Object> context) {
        logSecurityEvent("ANOMALOUS_ACCESS", description, null, null, clientIp, userAgent, context);
    }

    /**
     * 记录数据泄露尝试
     */
    public void logDataExfiltrationAttempt(String resourceType, String resourceId, String userId, String clientIp, String userAgent) {
        logSecurityEvent("DATA_EXFILTRATION", "Possible data exfiltration attempt on " + resourceType,
            userId, null, clientIp, userAgent,
            Map.of("resourceType", resourceType, "resourceId", resourceId));
    }

    /**
     * 记录权限提升尝试
     */
    public void logPrivilegeEscalationAttempt(String userId, String attemptedAction, String clientIp, String userAgent) {
        logSecurityEvent("PRIVILEGE_ESCALATION", "Privilege escalation attempt: " + attemptedAction,
            userId, null, clientIp, userAgent,
            Map.of("attemptedAction", attemptedAction));
    }

    /**
     * 记录配置变更
     */
    public void logConfigurationChange(String configType, String configKey, String oldValue, String newValue, String userId, String userName, String clientIp) {
        logSecurityEvent("CONFIG_CHANGE", "Configuration changed: " + configType + "." + configKey,
            userId, userName, clientIp, null,
            Map.of("configType", configType, "configKey", configKey, "oldValue", oldValue, "newValue", newValue));
    }

    /**
     * 记录密钥轮换
     */
    public void logKeyRotation(String keyType, String keyId, String userId, String userName, String clientIp) {
        logSecurityEvent("KEY_ROTATION", keyType + " key rotated: " + keyId,
            userId, userName, clientIp, null,
            Map.of("keyType", keyType, "keyId", keyId));
    }

    /**
     * 解除封禁
     */
    public void logUnblock(String gameId, String riskCaseId, String targetId, String unblockedBy, String clientIp) {
        log(AuditLogEntity.AuditAction.UNBLOCK, "risk_case", riskCaseId, targetId,
            "Unblocked target: " + targetId, AuditLogEntity.AuditResult.SUCCESS,
            unblockedBy, null, null, clientIp, null,
            Map.of("gameId", gameId, "targetId", targetId));
    }

    /**
     * 记录集成调用
     */
    public void logIntegrationCall(String integrationId, String integrationName, String action, String userId, String userName, String clientIp, Map<String, Object> details) {
        log(AuditLogEntity.AuditAction.INTEGRATION_CALL, "integration", integrationId, integrationName,
            action, AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null, details);
    }

    /**
     * 记录集成禁用
     */
    public void logIntegrationDisable(String integrationId, String integrationName, String userId, String userName, String clientIp, String reason) {
        log(AuditLogEntity.AuditAction.DISABLE, "integration", integrationId, integrationName,
            "Integration disabled: " + reason, AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null, Map.of("reason", reason));
    }

    /**
     * 记录集成删除
     */
    public void logIntegrationDelete(String integrationId, String integrationName, String userId, String userName, String clientIp) {
        log(AuditLogEntity.AuditAction.DELETE, "integration", integrationId, integrationName,
            "Integration deleted", AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null, null);
    }

    /**
     * 发送立即告警
     */
    private void sendImmediateAlert(AuditLogEntity log) {
        try {
            // TODO: 实现告警发送逻辑（邮件、Slack、Webhook等）
            logger.warn("Security alert requiring immediate attention: {}", log.getFullDescription());

            // 标记为已告警
            auditLogRepo.markAsAlerted(log.id);
        } catch (Exception e) {
            logger.error("Failed to send immediate alert for audit log: " + log.id, e);
        }
    }

    /**
     * 查询用户的审计日志
     */
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getUserAuditLogs(String userId) {
        return auditLogRepo.findByUserId(userId);
    }

    /**
     * 查询资源的审计日志
     */
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getResourceAuditLogs(String resourceType, String resourceId) {
        return auditLogRepo.findByResource(resourceType, resourceId);
    }

    /**
     * 查询游戏的审计日志
     */
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getGameAuditLogs(String gameId) {
        return auditLogRepo.findByGameId(gameId);
    }

    /**
     * 查询安全事件
     */
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getSecurityEvents() {
        return auditLogRepo.findSecurityEvents();
    }

    /**
     * 查询敏感操作
     */
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getSensitiveActions() {
        return auditLogRepo.findSensitiveActions();
    }

    /**
     * 查询失败的操作
     */
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getFailedOperations() {
        return auditLogRepo.findFailedOperations();
    }

    /**
     * 定期清理过期日志（每天凌晨执行）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredLogs() {
        try {
            int deleted = auditLogRepo.deleteExpiredLogs(LocalDateTime.now());
            if (deleted > 0) {
                logger.info("Cleaned up {} expired audit logs", deleted);
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup expired audit logs", e);
        }
    }

    /**
     * 统计用户操作次数
     */
    @Transactional(readOnly = true)
    public long countUserOperations(String userId, LocalDateTime since) {
        return auditLogRepo.countOperationsByUser(userId, since != null ? since : LocalDateTime.now().minusDays(30));
    }

    /**
     * 统计用户失败操作次数
     */
    @Transactional(readOnly = true)
    public long countUserFailedOperations(String userId, LocalDateTime since) {
        return auditLogRepo.countFailedOperationsByUser(userId, since != null ? since : LocalDateTime.now().minusDays(30));
    }

    /**
     * 记录封禁操作
     */
    public void logBlock(String gameId, String targetType, String targetId, String duration, String userId, String userName, String clientIp) {
        log(AuditLogEntity.AuditAction.BLOCK, targetType, targetId, targetId,
            "Blocked target for " + duration, AuditLogEntity.AuditResult.SUCCESS,
            userId, userName, null, clientIp, null,
            Map.of("gameId", gameId, "duration", duration, "actionType", "BLOCK"));
    }

    /**
     * 记录解除封禁操作
     */
    public void logUnblock(String gameId, String riskCaseId, String targetId, String unblockedBy, String clientIp) {
        log(AuditLogEntity.AuditAction.UNBLOCK, "risk_case", riskCaseId, riskCaseId,
            "Unblocked target: " + targetId, AuditLogEntity.AuditResult.SUCCESS,
            unblockedBy, null, null, clientIp, null,
            Map.of("gameId", gameId, "targetId", targetId, "actionType", "UNBLOCK"));
    }

    /**
     * 记录安全告警
     */
    public void logSecurityAlert(String gameId, String targetId, String riskLevel, String caseNumber) {
        log(AuditLogEntity.AuditAction.SECURITY_ALERT, "risk_case", caseNumber, caseNumber,
            "Security alert - " + riskLevel + " risk", AuditLogEntity.AuditResult.SUCCESS,
            "system", "Risk Management", null, null, null,
            Map.of("gameId", gameId, "targetId", targetId, "riskLevel", riskLevel));
    }

    /**
     * 记录集成创建
     */
    public void logIntegrationCreate(String integrationId, String integrationName, String integrationType, String createdBy, String gameId) {
        logCreate("integration", integrationId, integrationName, createdBy, createdBy, null,
            Map.of("integrationType", integrationType, "gameId", gameId));
    }

    /**
     * 记录集成调用
     */
    public void logIntegrationCall(String integrationId, String eventType, String result, String gameId) {
        log(AuditLogEntity.AuditAction.INTEGRATION_CALL, "integration", integrationId, integrationId,
            "Integration call - " + eventType + " - " + result,
            "SUCCESS".equals(result) ? AuditLogEntity.AuditResult.SUCCESS : AuditLogEntity.AuditResult.FAILURE,
            "system", "Integration Service", null, null, null,
            Map.of("eventType", eventType, "result", result, "gameId", gameId));
    }

    /**
     * 记录集成禁用
     */
    public void logIntegrationDisable(String integrationId, String integrationName, String gameId) {
        log(AuditLogEntity.AuditAction.DISABLE, "integration", integrationId, integrationName,
            "Integration disabled", AuditLogEntity.AuditResult.SUCCESS,
            "system", "Integration Service", null, null, null,
            Map.of("gameId", gameId));
    }

    /**
     * 记录集成删除
     */
    public void logIntegrationDelete(String integrationId, String integrationName, String gameId) {
        logDelete("integration", integrationId, integrationName, "system", "Integration Service", null);
    }
}
