package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 风控管理服务
 */
@Service
@Transactional
public class RiskManagementService {

    private static final Logger logger = LoggerFactory.getLogger(RiskManagementService.class);

    @Autowired
    private RiskRuleRepo riskRuleRepo;

    @Autowired
    private RiskCaseRepo riskCaseRepo;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 检查事件是否触发风控规则
     * 返回需要执行的动作列表
     */
    @Transactional(readOnly = true)
    public List<RiskAction> evaluateEvent(String gameId, String environmentId, String eventType, Map<String, Object> eventData, String targetId, String targetType) {
        List<RiskRuleEntity> rules = riskRuleRepo.findByGameIdAndEnvironment(gameId, environmentId);

        return rules.stream()
            .filter(RiskRuleEntity::isActive)
            .filter(rule -> !rule.isInTestMode())
            .filter(rule -> matchesRule(rule, eventType, eventData))
            .map(rule -> createRiskAction(rule, targetId, targetType, eventData))
            .collect(Collectors.toList());
    }

    /**
     * 创建风控案例
     */
    public RiskCaseEntity createRiskCase(String riskRuleId, String targetId, String targetType, Map<String, Object> context) {
        RiskRuleEntity rule = riskRuleRepo.findById(riskRuleId)
            .orElseThrow(() -> new IllegalArgumentException("Risk rule not found: " + riskRuleId));

        RiskCaseEntity riskCase = new RiskCaseEntity();
        riskCase.id = "rc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        riskCase.riskRuleId = riskRuleId;
        riskCase.gameId = rule.gameId;
        riskCase.environmentId = rule.environmentId;
        riskCase.caseNumber = generateCaseNumber();
        riskCase.targetType = targetType;
        riskCase.targetId = targetId;
        riskCase.riskLevel = rule.riskLevel;
        riskCase.riskScore = rule.riskScore;
        riskCase.actionTaken = rule.actionType;
        riskCase.executionStatus = RiskCaseEntity.ExecutionStatus.PENDING;
        riskCase.createdAt = LocalDateTime.now();

        try {
            if (context != null) {
                riskCase.contextData = objectMapper.writeValueAsString(context);
                riskCase.evidenceData = objectMapper.writeValueAsString(Map.of(
                    "event_type", context.get("event_type"),
                    "timestamp", context.get("timestamp"),
                    "ip", context.get("client_ip"),
                    "user_agent", context.get("user_agent")
                ));
            }
        } catch (Exception e) {
            logger.warn("Failed to serialize risk case context", e);
        }

        riskCase = riskCaseRepo.save(riskCase);
        rule.recordTrigger();
        riskRuleRepo.save(rule);

        logger.info("Risk case created: {} for target: {}", riskCase.id, targetId);
        return riskCase;
    }

    /**
     * 执行风控动作
     */
    public void executeAction(String riskCaseId) {
        RiskCaseEntity riskCase = riskCaseRepo.findById(riskCaseId)
            .orElseThrow(() -> new IllegalArgumentException("Risk case not found: " + riskCaseId));

        if (riskCase.executionStatus != RiskCaseEntity.ExecutionStatus.PENDING) {
            logger.warn("Risk case {} already executed or cancelled", riskCaseId);
            return;
        }

        RiskRuleEntity rule = riskRuleRepo.findById(riskCase.riskRuleId)
            .orElseThrow(() -> new IllegalArgumentException("Risk rule not found"));

        try {
            switch (rule.actionType) {
                case BLOCK:
                    if (Boolean.TRUE.equals(rule.enableAutoBlock)) {
                        executeBlock(riskCase, rule);
                    }
                    break;
                case ALERT:
                    executeAlert(riskCase);
                    break;
                case REVIEW:
                    addToReviewQueue(riskCase);
                    break;
                case WEBHOOK:
                    executeWebhook(riskCase, rule);
                    break;
                case THROTTLE:
                    executeThrottle(riskCase, rule);
                    break;
                default:
                    logger.info("Action {} requires manual processing", rule.actionType);
            }

            riskCase.markAsExecuted();
            if (rule.actionType == RiskRuleEntity.ActionType.BLOCK) {
                rule.recordBlock();
            }
            riskRuleRepo.save(rule);

        } catch (Exception e) {
            riskCase.markAsFailed(e.getMessage());
            logger.error("Failed to execute risk case {}: {}", riskCaseId, e.getMessage());
        }

        riskCaseRepo.save(riskCase);
    }

    /**
     * 解除封禁
     */
    public void unblockTarget(String riskCaseId, String unblockedBy, String reason) {
        RiskCaseEntity riskCase = riskCaseRepo.findById(riskCaseId)
            .orElseThrow(() -> new IllegalArgumentException("Risk case not found: " + riskCaseId));

        if (!riskCase.isBlocked()) {
            throw new IllegalStateException("Risk case is not blocked");
        }

        riskCase.unblock(unblockedBy, reason);
        riskCaseRepo.save(riskCase);

        // 记录审计日志
        auditLogService.logUnblock(riskCase.gameId, riskCaseId, riskCase.targetId, unblockedBy, null);

        logger.info("Unblocked target {} for case {}", riskCase.targetId, riskCaseId);
    }

    /**
     * 完成审核
     */
    public void completeReview(String riskCaseId, String reviewer, String notes, String disposition) {
        RiskCaseEntity riskCase = riskCaseRepo.findById(riskCaseId)
            .orElseThrow(() -> new IllegalArgumentException("Risk case not found: " + riskCaseId));

        riskCase.completeReview(reviewer, notes, disposition);
        riskCaseRepo.save(riskCase);

        // 如果是误报，考虑解除封禁
        if ("confirmed_benign".equals(disposition) && riskCase.isBlocked()) {
            unblockTarget(riskCaseId, reviewer, "Review confirmed as benign: " + notes);
        }

        logger.info("Completed review for case {} with disposition: {}", riskCaseId, disposition);
    }

    /**
     * 获取待审核案例
     */
    @Transactional(readOnly = true)
    public Page<RiskCaseEntity> getPendingReview(String gameId, Pageable pageable) {
        // 实现分页查询
        return null; // 简化实现
    }

    /**
     * 获取高风险目标
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHighRiskTargets(String gameId, LocalDateTime since) {
        List<Object[]> results = riskCaseRepo.findFrequentTargets(since, 5);

        return results.stream()
            .map(row -> Map.of(
                "targetType", row[0],
                "targetId", row[1],
                "caseCount", row[2]
            ))
            .collect(Collectors.toList());
    }

    // 私有辅助方法

    private boolean matchesRule(RiskRuleEntity rule, String eventType, Map<String, Object> eventData) {
        // TODO: 实现规则匹配逻辑
        // 这里简化实现，实际需要解析ruleConditions并匹配
        return true;
    }

    private RiskAction createRiskAction(RiskRuleEntity rule, String targetId, String targetType, Map<String, Object> context) {
        RiskAction action = new RiskAction();
        action.ruleId = rule.id;
        action.ruleName = rule.name;
        action.actionType = rule.actionType;
        action.riskLevel = rule.riskLevel;
        action.riskScore = rule.riskScore;
        action.targetId = targetId;
        action.targetType = targetType;
        action.blockDuration = rule.blockDuration;
        action.needsReview = rule.needsReview();
        return action;
    }

    private void executeBlock(RiskCaseEntity riskCase, RiskRuleEntity rule) {
        // TODO: 实现实际的封禁逻辑
        // 例如：调用API封禁设备、IP或账号
        logger.info("Executing block for target {} with duration {} minutes",
            riskCase.targetId, rule.blockDuration);

        // 记录审计日志
        auditLogService.logBlock(
            riskCase.gameId,
            riskCase.targetType,
            riskCase.targetId,
            rule.blockDuration + " minutes",
            "system",
            "Risk Management",
            null
        );
    }

    private void executeAlert(RiskCaseEntity riskCase) {
        // TODO: 发送告警通知
        logger.warn("Security alert: {} - {} ({})",
            riskCase.getCaseTitle(), riskCase.targetType, riskCase.targetId);

        // 记录审计日志
        auditLogService.logSecurityAlert(
            riskCase.gameId,
            riskCase.targetId,
            riskCase.riskLevel.name(),
            riskCase.caseNumber
        );
    }

    private void addToReviewQueue(RiskCaseEntity riskCase) {
        riskCase.reviewStatus = "pending";
        logger.info("Added case {} to review queue", riskCase.id);
    }

    private void executeWebhook(RiskCaseEntity riskCase, RiskRuleEntity rule) {
        if (!Boolean.TRUE.equals(rule.enableWebhook) || rule.webhookUrl == null) {
            return;
        }

        // TODO: 实现Webhook调用
        logger.info("Sending webhook to {} for case {}", rule.webhookUrl, riskCase.id);
    }

    private void executeThrottle(RiskCaseEntity riskCase, RiskRuleEntity rule) {
        // TODO: 实现限流逻辑
        logger.info("Throttling target {} for case {}", riskCase.targetId, riskCase.id);
    }

    private String generateCaseNumber() {
        String date = LocalDateTime.now().toLocalDate().toString().replace("-", "");
        long sequence = System.currentTimeMillis() % 10000;
        return "CASE_" + date + "_" + sequence;
    }

    public static class RiskAction {
        public String ruleId;
        public String ruleName;
        public RiskRuleEntity.ActionType actionType;
        public RiskRuleEntity.RiskLevel riskLevel;
        public Integer riskScore;
        public String targetId;
        public String targetType;
        public Integer blockDuration;
        public boolean needsReview;
    }
}
