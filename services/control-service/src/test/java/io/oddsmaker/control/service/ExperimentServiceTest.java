package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.dto.ExperimentDTO;
import io.oddsmaker.control.experiment.ExperimentEntity;
import io.oddsmaker.control.experiment.ExperimentRepo;
import io.oddsmaker.control.jpa.GameEntity;
import io.oddsmaker.control.jpa.GameEnvironmentEntity;
import io.oddsmaker.control.jpa.GameEnvironmentRepo;
import io.oddsmaker.control.jpa.GameRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    private ExperimentEntity testExperiment;
    private ExperimentDTO testExperimentDTO;
    private GameEntity testGame;
    private GameEnvironmentEntity testEnvironment;

    @BeforeEach
    void setUp() {
        testGame = new GameEntity();
        testGame.id = "game_test123";
        testGame.name = "Test Game";
        testGame.deletedAt = null;

        testEnvironment = new GameEnvironmentEntity();
        testEnvironment.id = "env_test123";
        testEnvironment.gameId = "game_test123";
        testEnvironment.name = "prod";
        testEnvironment.deletedAt = null;

        testExperiment = new ExperimentEntity();
        testExperiment.id = "exp_test123";
        testExperiment.gameId = "game_test123";
        testExperiment.environmentId = "env_test123";
        testExperiment.name = "Test Experiment";
        testExperiment.status = "draft";
        testExperiment.salt = "exp_test123";
        testExperiment.configJson = "{\"variants\":[{\"name\":\"A\",\"weight\":50},{\"name\":\"B\",\"weight\":50}]}";
        testExperiment.createdAt = Instant.now();
        testExperiment.updatedAt = Instant.now();

        testExperimentDTO = new ExperimentDTO();
        testExperimentDTO.id = "exp_test123";
        testExperimentDTO.gameId = "game_test123";
        testExperimentDTO.environmentId = "env_test123";
        testExperimentDTO.name = "Test Experiment";
        testExperimentDTO.status = "draft";
    }

    @Test
    @DisplayName("创建实验 - 成功")
    void createExperiment_Success() {
        // Given
        when(gameRepo.findById("game_test123")).thenReturn(Optional.of(testGame));
        when(environmentRepo.findByGameIdAndDeletedAtIsNull("game_test123")).thenReturn(Arrays.asList(testEnvironment));
        when(experimentRepo.existsById(anyString())).thenReturn(false);
        when(experimentRepo.save(any(ExperimentEntity.class))).thenReturn(testExperiment);

        // When
        ExperimentDTO result = experimentService.createExperiment(testExperimentDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("Test Experiment");
        verify(experimentRepo, times(1)).save(any(ExperimentEntity.class));
    }

    @Test
    @DisplayName("创建实验 - 游戏不存在")
    void createExperiment_GameNotFound() {
        // Given
        when(gameRepo.findById("game_notexist")).thenReturn(Optional.empty());
        testExperimentDTO.gameId = "game_notexist";

        // When & Then
        assertThatThrownBy(() -> experimentService.createExperiment(testExperimentDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Game not found");
    }

    @Test
    @DisplayName("创建实验 - ID已存在")
    void createExperiment_IdAlreadyExists() {
        // Given
        when(gameRepo.findById("game_test123")).thenReturn(Optional.of(testGame));
        when(environmentRepo.findByGameIdAndDeletedAtIsNull("game_test123")).thenReturn(Arrays.asList(testEnvironment));
        when(experimentRepo.existsById("exp_test123")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> experimentService.createExperiment(testExperimentDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Experiment already exists");
    }

    @Test
    @DisplayName("获取实验 - 存在")
    void getExperiment_Exists() {
        // Given
        when(experimentRepo.findById("exp_test123")).thenReturn(Optional.of(testExperiment));

        // When
        Optional<ExperimentDTO> result = experimentService.getExperiment("exp_test123");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().name).isEqualTo("Test Experiment");
    }

    @Test
    @DisplayName("获取实验 - 不存在")
    void getExperiment_NotExists() {
        // Given
        when(experimentRepo.findById("exp_notexist")).thenReturn(Optional.empty());

        // When
        Optional<ExperimentDTO> result = experimentService.getExperiment("exp_notexist");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("更新实验 - 成功")
    void updateExperiment_Success() {
        // Given
        ExperimentDTO updateDTO = new ExperimentDTO();
        updateDTO.name = "Updated Experiment";

        when(experimentRepo.findById("exp_test123")).thenReturn(Optional.of(testExperiment));
        when(experimentRepo.save(any(ExperimentEntity.class))).thenReturn(testExperiment);

        // When
        ExperimentDTO result = experimentService.updateExperiment("exp_test123", updateDTO);

        // Then
        assertThat(result).isNotNull();
        verify(experimentRepo, times(1)).save(any(ExperimentEntity.class));
    }

    @Test
    @DisplayName("更新实验 - 不存在")
    void updateExperiment_NotExists() {
        // Given
        when(experimentRepo.findById("exp_notexist")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> experimentService.updateExperiment("exp_notexist", testExperimentDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Experiment not found");
    }

    @Test
    @DisplayName("更新实验 - 不能更改游戏")
    void updateExperiment_CannotChangeGame() {
        // Given
        ExperimentDTO updateDTO = new ExperimentDTO();
        updateDTO.gameId = "game_different";

        when(experimentRepo.findById("exp_test123")).thenReturn(Optional.of(testExperiment));

        // When & Then
        assertThatThrownBy(() -> experimentService.updateExperiment("exp_test123", updateDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Experiment game cannot be changed");
    }

    @Test
    @DisplayName("发布实验 - 成功")
    void publishExperiment_Success() {
        // Given
        when(experimentRepo.findById("exp_test123")).thenReturn(Optional.of(testExperiment));
        when(experimentRepo.save(any(ExperimentEntity.class))).thenReturn(testExperiment);

        // When
        ExperimentDTO result = experimentService.publishExperiment("exp_test123");

        // Then
        assertThat(result).isNotNull();
        assertThat(testExperiment.status).isEqualTo("running");
        verify(experimentRepo, times(1)).save(any(ExperimentEntity.class));
    }

    @Test
    @DisplayName("发布实验 - 不存在")
    void publishExperiment_NotExists() {
        // Given
        when(experimentRepo.findById("exp_notexist")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> experimentService.publishExperiment("exp_notexist"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Experiment not found");
    }

    @Test
    @DisplayName("暂停实验 - 成功")
    void pauseExperiment_Success() {
        // Given
        testExperiment.status = "running";
        when(experimentRepo.findById("exp_test123")).thenReturn(Optional.of(testExperiment));
        when(experimentRepo.save(any(ExperimentEntity.class))).thenReturn(testExperiment);

        // When
        ExperimentDTO result = experimentService.pauseExperiment("exp_test123");

        // Then
        assertThat(result).isNotNull();
        assertThat(testExperiment.status).isEqualTo("paused");
        verify(experimentRepo, times(1)).save(any(ExperimentEntity.class));
    }

    @Test
    @DisplayName("暂停实验 - 不存在")
    void pauseExperiment_NotExists() {
        // Given
        when(experimentRepo.findById("exp_notexist")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> experimentService.pauseExperiment("exp_notexist"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Experiment not found");
    }

    @Test
    @DisplayName("删除实验 - 成功")
    void deleteExperiment_Success() {
        // Given
        when(experimentRepo.existsById("exp_test123")).thenReturn(true);
        doNothing().when(experimentRepo).deleteById("exp_test123");

        // When
        boolean result = experimentService.deleteExperiment("exp_test123");

        // Then
        assertThat(result).isTrue();
        verify(experimentRepo, times(1)).deleteById("exp_test123");
    }

    @Test
    @DisplayName("删除实验 - 不存在")
    void deleteExperiment_NotExists() {
        // Given
        when(experimentRepo.existsById("exp_notexist")).thenReturn(false);

        // When
        boolean result = experimentService.deleteExperiment("exp_notexist");

        // Then
        assertThat(result).isFalse();
        verify(experimentRepo, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("获取实验列表 - 分页")
    void listExperiments_Paged() {
        // Given
        List<ExperimentEntity> experiments = Arrays.asList(testExperiment);
        Page<ExperimentEntity> page = new PageImpl<>(experiments, PageRequest.of(0, 10), 1);
        when(experimentRepo.search(any(), any(), any(), any(PageRequest.class))).thenReturn(page);

        // When
        var result = experimentService.listExperiments("game_test123", null, null, null, 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("获取运行中的配置")
    void getRunningConfig_Success() {
        // Given
        when(gameRepo.findById("game_test123")).thenReturn(Optional.of(testGame));
        when(environmentRepo.findByGameIdAndNameAndDeletedAtIsNull("game_test123", "prod"))
            .thenReturn(Arrays.asList(testEnvironment));
        when(experimentRepo.findRunningConfigs("game_test123", "env_test123"))
            .thenReturn(Arrays.asList(testExperiment));

        // When
        var result = experimentService.getRunningConfig("game_test123", "prod");

        // Then
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("自动生成ID")
    void createExperiment_AutoGenerateId() {
        // Given
        testExperimentDTO.id = null;
        when(gameRepo.findById("game_test123")).thenReturn(Optional.of(testGame));
        when(environmentRepo.findByGameIdAndDeletedAtIsNull("game_test123")).thenReturn(Arrays.asList(testEnvironment));
        when(experimentRepo.existsById(anyString())).thenReturn(false);
        when(experimentRepo.save(any(ExperimentEntity.class))).thenReturn(testExperiment);

        // When
        ExperimentDTO result = experimentService.createExperiment(testExperimentDTO);

        // Then
        assertThat(result).isNotNull();
        verify(experimentRepo, times(1)).save(any(ExperimentEntity.class));
    }

    @Test
    @DisplayName("自动生成盐值")
    void createExperiment_AutoGenerateSalt() {
        // Given
        testExperimentDTO.salt = null;
        when(gameRepo.findById("game_test123")).thenReturn(Optional.of(testGame));
        when(environmentRepo.findByGameIdAndDeletedAtIsNull("game_test123")).thenReturn(Arrays.asList(testEnvironment));
        when(experimentRepo.existsById(anyString())).thenReturn(false);
        when(experimentRepo.save(any(ExperimentEntity.class))).thenReturn(testExperiment);

        // When
        ExperimentDTO result = experimentService.createExperiment(testExperimentDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(testExperiment.salt).isEqualTo(testExperiment.id);
    }
}
