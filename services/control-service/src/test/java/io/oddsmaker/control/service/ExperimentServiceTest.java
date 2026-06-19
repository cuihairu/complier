package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import io.oddsmaker.control.experiment.ExperimentRepo;
import io.oddsmaker.control.jpa.GameRepo;
import io.oddsmaker.control.jpa.GameEnvironmentRepo;

/**
 * ExperimentService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExperimentService 单元测试")
class ExperimentServiceTest {

    @Mock
    private ExperimentRepo experimentRepo;

    @Mock
    private GameRepo gameRepo;

    @Mock
    private GameEnvironmentRepo environmentRepo;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ExperimentService experimentService;

    @Test
    @DisplayName("服务加载测试")
    void serviceLoads() {
        // 基本的服务加载测试
    }
}
