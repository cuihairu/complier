package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import io.oddsmaker.control.jpa.RiskRuleRepo;
import io.oddsmaker.control.jpa.RiskCaseRepo;

/**
 * RiskManagementService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskManagementService 单元测试")
class RiskManagementServiceTest {

    @Mock
    private RiskRuleRepo riskRuleRepo;

    @Mock
    private RiskCaseRepo riskCaseRepo;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RiskManagementService riskManagementService;

    @Test
    @DisplayName("服务加载测试")
    void serviceLoads() {
        // 基本的服务加载测试
    }
}
