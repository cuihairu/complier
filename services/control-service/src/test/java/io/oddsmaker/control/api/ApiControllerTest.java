package io.oddsmaker.control.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.dto.GameDTO;
import io.oddsmaker.control.jpa.GameEntity;
import io.oddsmaker.control.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApiController 集成测试
 */
@WebMvcTest(ApiController.class)
@DisplayName("ApiController 集成测试")
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ControlService controlService;

    @MockBean
    private GameService gameService;

    @MockBean
    private ExperimentService experimentService;

    @MockBean
    private StorageProfileService storageProfileService;

    private GameEntity testGame;
    private GameDTO testGameDTO;

    @BeforeEach
    void setUp() {
        testGame = new GameEntity();
        testGame.id = "game_test123";
        testGame.name = "Test Game";
        testGame.genre = "rpg";
        testGame.status = GameEntity.GameStatus.DRAFT;
        testGame.createdAt = LocalDateTime.now();

        testGameDTO = new GameDTO();
        testGameDTO.id = "game_test123";
        testGameDTO.name = "Test Game";
        testGameDTO.genre = "rpg";
    }

    @Test
    @DisplayName("获取游戏列表 - 成功")
    @WithMockUser(roles = "USER")
    void listGames_Success() throws Exception {
        // Given
        Page<GameDTO> page = new PageImpl<>(Arrays.asList(testGameDTO), PageRequest.of(0, 10), 1);
        when(gameService.getGames(any(PageRequest.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/games")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].id").value("game_test123"))
            .andExpect(jsonPath("$.items[0].name").value("Test Game"));
    }

    @Test
    @DisplayName("创建游戏 - 成功")
    @WithMockUser(roles = "ADMIN")
    void createGame_Success() throws Exception {
        // Given
        when(gameService.createGame(any(GameDTO.class))).thenReturn(testGameDTO);

        // When & Then
        mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testGameDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("game_test123"))
            .andExpect(jsonPath("$.name").value("Test Game"));
    }

    @Test
    @DisplayName("创建游戏 - 未授权")
    void createGame_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testGameDTO)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取游戏详情 - 存在")
    @WithMockUser(roles = "USER")
    void getGame_Exists() throws Exception {
        // Given
        when(gameService.getGame("game_test123")).thenReturn(Optional.of(testGameDTO));

        // When & Then
        mockMvc.perform(get("/api/games/{gameId}", "game_test123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("game_test123"))
            .andExpect(jsonPath("$.name").value("Test Game"));
    }

    @Test
    @DisplayName("获取游戏详情 - 不存在")
    @WithMockUser(roles = "USER")
    void getGame_NotExists() throws Exception {
        // Given
        when(gameService.getGame("game_notexist")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/games/{gameId}", "game_notexist"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("更新游戏 - 成功")
    @WithMockUser(roles = "ADMIN")
    void updateGame_Success() throws Exception {
        // Given
        GameDTO updateDTO = new GameDTO();
        updateDTO.name = "Updated Game";

        when(gameService.updateGame(eq("game_test123"), any(GameDTO.class))).thenReturn(testGameDTO);

        // When & Then
        mockMvc.perform(put("/api/games/{gameId}", "game_test123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("game_test123"));
    }

    @Test
    @DisplayName("删除游戏 - 成功")
    @WithMockUser(roles = "ADMIN")
    void deleteGame_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/games/{gameId}", "game_test123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deleted").value(true))
            .andExpect(jsonPath("$.gameId").value("game_test123"));
    }

    @Test
    @DisplayName("搜索游戏")
    @WithMockUser(roles = "USER")
    void searchGames() throws Exception {
        // Given
        Page<GameDTO> page = new PageImpl<>(Arrays.asList(testGameDTO), PageRequest.of(0, 10), 1);
        when(gameService.searchGames(eq("Test"), any(PageRequest.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/games")
                .param("q", "Test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].name").value("Test Game"));
    }

    @Test
    @DisplayName("发布游戏")
    @WithMockUser(roles = "ADMIN")
    void publishGame() throws Exception {
        // Given
        testGameDTO.status = GameEntity.GameStatus.STAGING;
        when(gameService.publishGame("game_test123")).thenReturn(testGameDTO);

        // When & Then
        mockMvc.perform(post("/api/games/{gameId}/publish", "game_test123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("game_test123"));
    }

    @Test
    @DisplayName("取消发布游戏")
    @WithMockUser(roles = "ADMIN")
    void unpublishGame() throws Exception {
        // Given
        testGameDTO.status = GameEntity.GameStatus.MAINTENANCE;
        when(gameService.unpublishGame("game_test123")).thenReturn(testGameDTO);

        // When & Then
        mockMvc.perform(post("/api/games/{gameId}/unpublish", "game_test123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("game_test123"));
    }

    @Test
    @DisplayName("获取环境列表")
    @WithMockUser(roles = "USER")
    void listEnvironments() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/games/{gameId}/environments", "game_test123"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("创建环境")
    @WithMockUser(roles = "ADMIN")
    void createEnvironment() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/games/{gameId}/environments", "game_test123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"prod\",\"type\":\"PRODUCTION\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取环境详情")
    @WithMockUser(roles = "USER")
    void getEnvironment() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/games/{gameId}/environments/{envName}", "game_test123", "prod"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("更新环境")
    @WithMockUser(roles = "ADMIN")
    void updateEnvironment() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/games/{gameId}/environments/{envName}", "game_test123", "prod")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Production\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("删除环境")
    @WithMockUser(roles = "ADMIN")
    void deleteEnvironment() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/games/{gameId}/environments/{envName}", "game_test123", "prod"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取存储配置列表")
    @WithMockUser(roles = "USER")
    void listStorageProfiles() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/storage-profiles"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("创建API Key")
    @WithMockUser(roles = "ADMIN")
    void createApiKey() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/api-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameId\":\"game_test123\",\"environmentId\":\"env_test123\",\"name\":\"test-key\"}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取API Key详情")
    @WithMockUser(roles = "USER")
    void getApiKey() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/api-keys/{apiKey}", "pk_test123"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("删除API Key")
    @WithMockUser(roles = "ADMIN")
    void deleteApiKey() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/api-keys/{apiKey}", "pk_test123"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取实验配置")
    @WithMockUser(roles = "USER")
    void getExperimentConfig() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/config/{gameId}/{environment}", "game_test123", "prod"))
            .andExpect(status().isOk());
    }
}
