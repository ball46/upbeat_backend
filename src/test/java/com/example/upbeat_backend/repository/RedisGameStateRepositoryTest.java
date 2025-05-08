package com.example.upbeat_backend.repository;

import com.example.upbeat_backend.game.dto.reids.CurrentStateDTO;
import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.player.PlayerImpl;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.game.state.region.RegionImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;

import java.sql.Timestamp;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisGameStateRepositoryTest {
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private RedisGameStateRepository repository;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private final String gameId = "test-game";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    private static @NotNull Map<Object, Object> getObjectObjectMap() {
        Map<Object, Object> redisData = new HashMap<>();
        redisData.put("rows", 10);
        redisData.put("cols", (short) 10);
        redisData.put("initBudget", 1000L);
        redisData.put("maxDep", 9999L);

        redisData.put("initPlanMin", 1);
        redisData.put("initPlanSec", 0);
        redisData.put("initCenterDep", 100);
        redisData.put("planRevMin", 0);
        redisData.put("planRevSec", 30);
        redisData.put("revCost", 100);
        redisData.put("interestPct", 5);
        return redisData;
    }

    // --- GAME INFO TESTS ---

    @Test
    void initializeGameInfo_shouldCreateGameInfo() {
        int maxPlayers = 4;

        repository.initializeGameInfo(gameId, maxPlayers);

        verify(hashOperations).putAll(eq("game:test-game:info"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();

        assertThat(capturedMap).containsKey("winner");
        assertThat(capturedMap.get("winner")).isNull();
        assertThat(capturedMap.get("status")).isEqualTo(GameStatus.WAITING_FOR_PLAYERS.name());
        assertThat(capturedMap).containsKey("createdAt");
        assertThat(capturedMap.get("maxPlayers")).isEqualTo(maxPlayers);
        assertThat(capturedMap.get("currentTurn")).isEqualTo(1);
        assertThat(capturedMap).containsKey("lastUpdatedAt");
    }

    @Test
    void updateGameStatus_shouldUpdateStatus() {
        GameStatus newStatus = GameStatus.IN_PROGRESS;

        repository.updateGameStatus(gameId, newStatus);

        verify(hashOperations).put("game:test-game:info", "status", newStatus.name());
        verify(hashOperations).put(eq("game:test-game:info"), eq("lastUpdatedAt"), anyLong());
    }

    @Test
    void incrementTurn_shouldIncrementTurnCounter() {
        String gameId = "test-game";

        repository.incrementTurn(gameId);

        verify(hashOperations).increment("game:test-game:info", "currentTurn", 1);
        verify(hashOperations).put(eq("game:test-game:info"), eq("lastUpdatedAt"), anyLong());
    }

    @Test
    void getGameInfo_shouldReturnGameInfo() {
        Map<Object, Object> mockData = new HashMap<>();
        mockData.put("winner", "player1");
        mockData.put("status", GameStatus.FINISHED.name());
        mockData.put("createdAt", 1622548800L); // June 1, 2021
        mockData.put("maxPlayers", 4);
        mockData.put("currentTurn", 10);
        mockData.put("lastUpdatedAt", 1622548900L);

        when(hashOperations.entries("game:test-game:info")).thenReturn(mockData);

        GameInfoDTO result = repository.getGameInfo(gameId);

        assertThat(result).isNotNull();
        assertThat(result.getGameStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(result.getWinner()).isEqualTo("player1");
        assertThat(result.getMaxPlayers()).isEqualTo(4);
        assertThat(result.getCurrentTurn()).isEqualTo(10);
        assertThat(result.getCreateAt()).isEqualTo(new Timestamp(1622548800L));
        assertThat(result.getLastUpdateAt()).isEqualTo(new Timestamp(1622548900L));
    }

    @Test
    void getGameInfo_shouldReturnNullWhenNoData() {
        when(hashOperations.entries("game:test-game:info")).thenReturn(Collections.emptyMap());

        GameInfoDTO result = repository.getGameInfo(gameId);

        assertThat(result).isNull();
    }

    @Test
    void getPlayersWithCityCenters_shouldReturnPlayersWithCenters() {
        List<Object> playersList = Arrays.asList("player1", "player2", "player3");

        Map<Object, Object> player1Data = new HashMap<>();
        player1Data.put("id", "player1");
        player1Data.put("name", "Player 1");
        player1Data.put("budget", 1000L);
        player1Data.put("cityCenterRow", 5);
        player1Data.put("cityCenterCol", 5);

        Map<Object, Object> player2Data = new HashMap<>();
        player2Data.put("id", "player2");
        player2Data.put("name", "Player 2");
        player2Data.put("budget", 1000L);
        player2Data.put("cityCenterRow", -1); // No city center
        player2Data.put("cityCenterCol", -1);

        Map<Object, Object> player3Data = new HashMap<>();
        player3Data.put("id", "player3");
        player3Data.put("name", "Player 3");
        player3Data.put("budget", 1000L);
        player3Data.put("cityCenterRow", 7);
        player3Data.put("cityCenterCol", 7);

        when(listOperations.range("game:test-game:players", 0, -1)).thenReturn(playersList);
        when(hashOperations.entries("game:test-game:player:player1")).thenReturn(player1Data);
        when(hashOperations.entries("game:test-game:player:player2")).thenReturn(player2Data);
        when(hashOperations.entries("game:test-game:player:player3")).thenReturn(player3Data);

        List<String> result = repository.getPlayersWithCityCenters(gameId);

        assertThat(result).containsExactly("player1", "player3");
    }

    @Test
    void setGameWinner_shouldSetWinner() {
        String playerId = "player1";

        repository.setGameWinner(gameId, playerId);

        verify(hashOperations).put("game:test-game:info", "winner", playerId);
        verify(hashOperations).put("game:test-game:info", "status", GameStatus.FINISHED.name());
        verify(hashOperations).put(eq("game:test-game:info"), eq("lastUpdatedAt"), anyLong());
    }

    // --- GAME CONFIG TESTS ---

    @Test
    void saveGameConfig_shouldSaveConfiguration() {
        GameConfigDTO config = GameConfigDTO.builder()
                .rows(10)
                .cols(10)
                .initPlanMin(1)
                .initPlanSec(0)
                .initBudget(1000L)
                .initCenterDep(100L)
                .planRevMin(0)
                .planRevSec(30)
                .revCost(100L)
                .maxDep(9999L)
                .interestPct(5)
                .build();

        repository.saveGameConfig(gameId, config);

        verify(hashOperations).putAll(eq("game:test-game:config"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();

        assertThat(capturedMap.get("rows")).isEqualTo(10);
        assertThat(capturedMap.get("cols")).isEqualTo(10);
        assertThat(capturedMap.get("initPlanMin")).isEqualTo(1);
        assertThat(capturedMap.get("initPlanSec")).isEqualTo(0);
        assertThat(capturedMap.get("initBudget")).isEqualTo(1000L);
        assertThat(capturedMap.get("initCenterDep")).isEqualTo(100L);
        assertThat(capturedMap.get("planRevMin")).isEqualTo(0);
        assertThat(capturedMap.get("planRevSec")).isEqualTo(30);
        assertThat(capturedMap.get("revCost")).isEqualTo(100L);
        assertThat(capturedMap.get("maxDep")).isEqualTo(9999L);
        assertThat(capturedMap.get("interestPct")).isEqualTo(5);
    }

    @Test
    void saveGameConfig_shouldHandleMinimalConfiguration() {
        GameConfigDTO minimalConfig = GameConfigDTO.builder()
                .rows(5).cols(5)
                .initBudget(500L)
                .maxDep(1000L)
                .build();

        repository.saveGameConfig(gameId, minimalConfig);

        verify(hashOperations).putAll(eq("game:test-game:config"), any());
    }

    @Test
    void getGameConfig_shouldReturnValidConfig() {
        Map<Object, Object> redisData = getObjectObjectMap();

        when(hashOperations.entries("game:test-game:config")).thenReturn(redisData);

        GameConfigDTO result = repository.getGameConfig(gameId);

        assertThat(result).isNotNull();
        assertThat(result.getRows()).isEqualTo(10);
        assertThat(result.getCols()).isEqualTo(10);
        assertThat(result.getInitBudget()).isEqualTo(1000);
        assertThat(result.getMaxDep()).isEqualTo(9999);
        assertThat(result.getInitPlanMin()).isEqualTo(1);
        assertThat(result.getInitPlanSec()).isEqualTo(0);
        assertThat(result.getInitCenterDep()).isEqualTo(100);
        assertThat(result.getPlanRevMin()).isEqualTo(0);
        assertThat(result.getPlanRevSec()).isEqualTo(30);
        assertThat(result.getRevCost()).isEqualTo(100);
        assertThat(result.getInterestPct()).isEqualTo(5);
    }

    @Test
    void getGameConfig_shouldReturnNullWhenNoData() {
        when(hashOperations.entries("game:test-game:config")).thenReturn(Collections.emptyMap());

        GameConfigDTO result = repository.getGameConfig(gameId);

        assertThat(result).isNull();
    }

    @Test
    void getGameConfig_shouldHandleDifferentDataTypes() {
        Map<Object, Object> redisData = getObjectObjectMap();

        when(hashOperations.entries("game:test-game:config")).thenReturn(redisData);

        GameConfigDTO result = repository.getGameConfig(gameId);

        assertThat(result).isNotNull();
        assertThat(result.getRows()).isEqualTo(10);
        assertThat(result.getCols()).isEqualTo(10);
        assertThat(result.getInitBudget()).isEqualTo(1000);
        assertThat(result.getMaxDep()).isEqualTo(9999);
        assertThat(result.getInterestPct()).isEqualTo(5);
    }

    // --- PLAYERS TESTS ---

    @Test
    void addPlayerToGame_shouldAddPlayerToList() {
        String playerId = "player123";

        repository.addPlayerToGame(gameId, playerId);

        verify(listOperations).rightPush("game:test-game:players", playerId);
    }

    @Test
    void getGamePlayers_shouldReturnAllPlayers() {
        List<Object> mockPlayers = Arrays.asList("player1", "player2", "player3");
        when(listOperations.range("game:test-game:players", 0, -1)).thenReturn(mockPlayers);

        List<String> players = repository.getGamePlayers(gameId);

        assertThat(players).hasSize(3);
        assertThat(players).containsExactly("player1", "player2", "player3");
    }

    @Test
    void getGamePlayers_shouldReturnEmptyList_whenNoPlayersExist() {
        when(listOperations.range("game:test-game:players", 0, -1)).thenReturn(null);

        List<String> players = repository.getGamePlayers(gameId);

        assertThat(players).isEmpty();
    }

    @Test
    void removePlayerFromGame_shouldRemovePlayerFromList() {
        List<Object> currentPlayers = new ArrayList<>(Arrays.asList("player1", "player2", "player3"));
        String playerToRemove = "player2";

        when(listOperations.range("game:test-game:players", 0, -1)).thenReturn(currentPlayers);

        repository.removePlayerFromGame(gameId, playerToRemove);

        verify(redisTemplate).delete("game:test-game:players");

        verify(listOperations).rightPush("game:test-game:players", "player1");
        verify(listOperations).rightPush("game:test-game:players", "player3");
        verify(listOperations, never()).rightPush("game:test-game:players", "player2");
    }

    @Test
    void removePlayerFromGame_shouldDoNothing_whenPlayerListIsNull() {
        when(listOperations.range("game:test-game:players", 0, -1)).thenReturn(null);

        repository.removePlayerFromGame(gameId, "player1");

        verify(redisTemplate, never()).delete(anyString());
        verify(listOperations, never()).rightPush(anyString(), anyString());
    }

    // --- PLAYER DATA TESTS ---

    @Test
    void savePlayer_shouldSavePlayerData() {
        String playerId = "player123";
        Player player = new PlayerImpl(playerId, "Test Player", 1000L, 5, 5);

        repository.savePlayer(gameId, player);

        verify(hashOperations).putAll(eq("game:test-game:player:player123"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();

        assertThat(capturedMap.get("id")).isEqualTo("player123");
        assertThat(capturedMap.get("name")).isEqualTo("Test Player");
        assertThat(capturedMap.get("budget")).isEqualTo(1000L);
        assertThat(capturedMap.get("cityCenterRow")).isEqualTo(5);
        assertThat(capturedMap.get("cityCenterCol")).isEqualTo(5);
    }

    @Test
    void updatePlayerBudget_shouldUpdateBudget() {
        String playerId = "player123";
        long newBudget = 2000L;

        repository.updatePlayerBudget(gameId, playerId, newBudget);

        verify(hashOperations).put("game:test-game:player:player123", "budget", 2000L);
    }

    @Test
    void incrementPlayerBudget_shouldIncrementBudget() {
        String playerId = "player123";
        long incrementAmount = 500L;

        repository.incrementPlayerBudget(gameId, playerId, incrementAmount);

        verify(hashOperations).increment("game:test-game:player:player123", "budget", 500L);
    }

    @Test
    void getPlayer_shouldReturnPlayer() {
        String playerId = "player123";
        Map<Object, Object> playerData = new HashMap<>();
        playerData.put("id", "player123");
        playerData.put("name", "Test Player");
        playerData.put("budget", 1000L);
        playerData.put("cityCenterRow", 5);
        playerData.put("cityCenterCol", 5);

        when(hashOperations.entries("game:test-game:player:player123")).thenReturn(playerData);

        Player result = repository.getPlayer(gameId, playerId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("player123");
        assertThat(result.getName()).isEqualTo("Test Player");
        assertThat(result.getBudget()).isEqualTo(1000L);
        assertThat(result.getCityCenterRow()).isEqualTo(5);
        assertThat(result.getCityCenterCol()).isEqualTo(5);
    }

    @Test
    void getPlayer_shouldReturnNullWhenNoData() {
        String playerId = "nonexistent";
        when(hashOperations.entries("game:test-game:player:nonexistent")).thenReturn(Collections.emptyMap());

        Player result = repository.getPlayer(gameId, playerId);

        assertThat(result).isNull();
    }

    // --- PLAYER PLANS TESTS ---

    @Test
    void savePlayerPlan_shouldSavePlanToRedis() {
        String playerId = "player123";
        String plan = "move up; collect(100); move right;";

        repository.savePlayerPlan(gameId, playerId, plan);

        verify(valueOperations).set("game:test-game:player:player123:plan", plan);
    }

    @Test
    void getPlayerPlan_shouldReturnPlan() {
        String playerId = "player123";
        String plan = "move up; collect(100); move right;";

        when(valueOperations.get("game:test-game:player:player123:plan")).thenReturn(plan);

        String result = repository.getPlayerPlan(gameId, playerId);

        assertThat(result).isEqualTo(plan);
    }

    @Test
    void getPlayerPlan_shouldReturnNullWhenNoPlan() {
        String playerId = "player123";
        when(valueOperations.get("game:test-game:player:player123:plan")).thenReturn(null);

        String result = repository.getPlayerPlan(gameId, playerId);

        assertThat(result).isNull();
    }

    // --- PLAYER VARIABLES TESTS ---

    @Test
    void setPlayerVariable_shouldStoreVariableInRedis() {
        String playerId = "player123";
        String varName = "testVar";
        Long value = 42L;

        repository.setPlayerVariable(gameId, playerId, varName, value);

        verify(hashOperations).put("game:test-game:player:player123:vars", varName, value);
    }

    @Test
    void getPlayerVariable_shouldReturnVariable() {
        String playerId = "player123";
        String varName = "testVar";
        Long value = 42L;

        when(hashOperations.get("game:test-game:player:player123:vars", varName)).thenReturn(value);

        Long result = repository.getPlayerVariable(gameId, playerId, varName);

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void getPlayerVariable_shouldReturnNullWhenNoVariable() {
        String playerId = "player123";
        String varName = "nonExistentVar";

        when(hashOperations.get("game:test-game:player:player123:vars", varName)).thenReturn(null);

        Long result = repository.getPlayerVariable(gameId, playerId, varName);

        assertThat(result).isNull();
    }

    @Test
    void getAllPlayerVariables_shouldReturnAllVariables() {
        String playerId = "player123";
        Map<Object, Object> redisVariables = new HashMap<>();
        redisVariables.put("var1", 10L);
        redisVariables.put("var2", 20L);
        redisVariables.put("var3", 30L);

        when(hashOperations.entries("game:test-game:player:player123:vars")).thenReturn(redisVariables);

        Map<String, Long> result = repository.getAllPlayerVariables(gameId, playerId);

        assertThat(result).hasSize(3);
        assertThat(result).containsEntry("var1", 10L);
        assertThat(result).containsEntry("var2", 20L);
        assertThat(result).containsEntry("var3", 30L);
    }

    @Test
    void getAllPlayerVariables_shouldReturnEmptyMapWhenNoVariables() {
        String playerId = "player123";
        when(hashOperations.entries("game:test-game:player:player123:vars")).thenReturn(Collections.emptyMap());

        Map<String, Long> result = repository.getAllPlayerVariables(gameId, playerId);

        assertThat(result).isEmpty();
    }

    // --- TERRITORY SIZE TESTS ---

    @Test
    void saveTerritorySize_shouldSaveDimensions() {
        int rows = 15;
        int cols = 20;

        repository.saveTerritorySize(gameId, rows, cols);

        verify(hashOperations).putAll(eq("game:test-game:territory:size"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();

        assertThat(capturedMap.get("rows")).isEqualTo(15);
        assertThat(capturedMap.get("cols")).isEqualTo(20);
    }

    @Test
    void getTerritorySize_shouldReturnCorrectDimensions() {
        Map<Object, Object> sizeData = new HashMap<>();
        sizeData.put("rows", 15);
        sizeData.put("cols", 20);

        when(hashOperations.entries("game:test-game:territory:size")).thenReturn(sizeData);

        TerritorySizeDTO result = repository.getTerritorySize(gameId);

        assertThat(result).isNotNull();
        assertThat(result.getRows()).isEqualTo(15);
        assertThat(result.getCols()).isEqualTo(20);
    }

    @Test
    void getTerritorySize_shouldReturnNull_whenNoData() {
        when(hashOperations.entries("game:test-game:territory:size")).thenReturn(Collections.emptyMap());

        TerritorySizeDTO result = repository.getTerritorySize(gameId);

        assertThat(result).isNull();
    }

    // --- REGIONS TESTS ---

    @Test
    void saveRegion_shouldSaveRegionData() {
        Region region = new RegionImpl(1000, 5, 7);
        region.updateDeposit(500);
        region.updateOwner("player1");

        repository.saveRegion(gameId, region);

        verify(hashOperations).put(eq("game:test-game:territory:regions"), eq("5:7"), mapCaptor.capture());
        Map<String, Object> capturedData = mapCaptor.getValue();

        assertThat(capturedData.get("deposit")).isEqualTo(500L);
        assertThat(capturedData.get("owner")).isEqualTo("player1");
    }

    @Test
    void updateRegion_shouldUpdateRegionData() {
        int row = 3;
        int col = 4;
        long deposit = 250;
        String owner = "player2";

        repository.updateRegion(gameId, row, col, deposit, owner);

        verify(hashOperations).put(eq("game:test-game:territory:regions"), eq("3:4"), mapCaptor.capture());
        Map<String, Object> capturedData = mapCaptor.getValue();

        assertThat(capturedData.get("deposit")).isEqualTo(250L);
        assertThat(capturedData.get("owner")).isEqualTo("player2");
    }

    @Test
    void getRegion_shouldReturnExistingRegion() {
        Map<String, Object> regionData = new HashMap<>();
        regionData.put("deposit", 300L);
        regionData.put("owner", "player1");

        Map<Object, Object> configData = getObjectObjectMap();

        when(hashOperations.get("game:test-game:territory:regions", "2:3")).thenReturn(regionData);
        when(hashOperations.entries("game:test-game:config")).thenReturn(configData);

        Region result = repository.getRegion(gameId, 2, 3);

        assertThat(result).isNotNull();
        assertThat(result.getRow()).isEqualTo(2);
        assertThat(result.getCol()).isEqualTo(3);
        assertThat(result.getDeposit()).isEqualTo(300L);
        assertThat(result.getOwner()).isEqualTo("player1");
    }

    @Test
    void getRegion_shouldCreateNewRegion_whenNotExists() {
        Map<Object, Object> configData = getObjectObjectMap();

        when(hashOperations.get("game:test-game:territory:regions", "4:5")).thenReturn(null);
        when(hashOperations.entries("game:test-game:config")).thenReturn(configData);

        Region result = repository.getRegion(gameId, 4, 5);

        assertThat(result).isNotNull();
        assertThat(result.getRow()).isEqualTo(4);
        assertThat(result.getCol()).isEqualTo(5);
        assertThat(result.getMaxDeposit()).isEqualTo(9999L);
        assertThat(result.getDeposit()).isEqualTo(0L);
        assertThat(result.getOwner()).isNull();
    }

    @Test
    void getAllRegions_shouldReturnAllRegionsMap() {
        Map<String, Object> region1Data = new HashMap<>();
        region1Data.put("deposit", 100L);
        region1Data.put("owner", "player1");

        Map<String, Object> region2Data = new HashMap<>();
        region2Data.put("deposit", 200L);
        region2Data.put("owner", "player2");

        Map<Object, Object> allRegionsData = new HashMap<>();
        allRegionsData.put("1:2", region1Data);
        allRegionsData.put("3:4", region2Data);

        Map<Object, Object> configData = getObjectObjectMap();

        when(hashOperations.entries("game:test-game:territory:regions")).thenReturn(allRegionsData);
        when(hashOperations.entries("game:test-game:config")).thenReturn(configData);

        Map<String, Region> result = repository.getAllRegions(gameId);

        assertThat(result).hasSize(2);

        Region region1 = result.get("1:2");
        assertThat(region1).isNotNull();
        assertThat(region1.getRow()).isEqualTo(1);
        assertThat(region1.getCol()).isEqualTo(2);
        assertThat(region1.getDeposit()).isEqualTo(100L);
        assertThat(region1.getOwner()).isEqualTo("player1");

        Region region2 = result.get("3:4");
        assertThat(region2).isNotNull();
        assertThat(region2.getRow()).isEqualTo(3);
        assertThat(region2.getCol()).isEqualTo(4);
        assertThat(region2.getDeposit()).isEqualTo(200L);
        assertThat(region2.getOwner()).isEqualTo("player2");
    }

    // --- CURRENT STATE TESTS ---

    @Test
    void saveCurrentState_shouldSaveStateCorrectly() {
        String currentPlayerId = "player1";
        int currentRow = 5;
        int currentCol = 7;

        repository.saveCurrentState(gameId, currentPlayerId, currentRow, currentCol);

        verify(hashOperations).putAll(eq("game:test-game:currentState"), mapCaptor.capture());
        Map<String, Object> capturedMap = mapCaptor.getValue();

        assertThat(capturedMap.get("currentPlayerId")).isEqualTo("player1");
        assertThat(capturedMap.get("currentRow")).isEqualTo(5);
        assertThat(capturedMap.get("currentCol")).isEqualTo(7);
    }

    @Test
    void updateCurrentPosition_shouldUpdatePositionOnly() {
        int newRow = 8;
        int newCol = 9;

        repository.updateCurrentPosition(gameId, newRow, newCol);

        verify(hashOperations).put("game:test-game:currentState", "currentRow", 8);
        verify(hashOperations).put("game:test-game:currentState", "currentCol", 9);
    }

    @Test
    void updateCurrentPlayer_shouldUpdatePlayerAndResetPosition() {
        String playerId = "player2";
        Player player = new PlayerImpl(playerId, "Player 2", 1000L, 3, 4);

        when(hashOperations.entries("game:test-game:player:player2")).thenReturn(Map.of(
                "id", "player2",
                "name", "Player 2",
                "budget", 1000L,
                "cityCenterRow", 3,
                "cityCenterCol", 4
        ));

        repository.updateCurrentPlayer(gameId, playerId);

        verify(hashOperations).put("game:test-game:currentState", "currentPlayerId", "player2");
        verify(hashOperations).put("game:test-game:currentState", "currentRow", 3);
        verify(hashOperations).put("game:test-game:currentState", "currentCol", 4);
    }

    @Test
    void getCurrentState_shouldReturnCurrentState() {
        Map<Object, Object> stateData = new HashMap<>();
        stateData.put("currentPlayerId", "player1");
        stateData.put("currentRow", 5);
        stateData.put("currentCol", 7);

        when(hashOperations.entries("game:test-game:currentState")).thenReturn(stateData);

        CurrentStateDTO result = repository.getCurrentState(gameId);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentPlayerId()).isEqualTo("player1");
        assertThat(result.getCurrentRow()).isEqualTo(5);
        assertThat(result.getCurrentCol()).isEqualTo(7);
    }

    @Test
    void getCurrentState_shouldReturnNull_whenNoData() {
        when(hashOperations.entries("game:test-game:currentState")).thenReturn(Collections.emptyMap());

        CurrentStateDTO result = repository.getCurrentState(gameId);

        assertThat(result).isNull();
    }

    // --- DELETE GAME DATA TESTS ---

    @Test
    void deleteGameData_shouldDeleteAllGameKeys() {
        when(redisTemplate.opsForSet().members("game:test-game:players"))
                .thenReturn(Set.of("player1", "player2"));

        repository.deleteGameData(gameId);

        verify(redisTemplate).delete(argThat((List<String> keys) ->
                keys.contains("game:test-game:info") &&
                keys.contains("game:test-game:players") &&
                keys.contains("game:test-game:territory:size") &&
                keys.contains("game:test-game:territory:regions") &&
                keys.contains("game:test-game:currentState")
        ));

        verify(redisTemplate).delete(argThat((List<String> keys) ->
                keys.contains("game:test-game:player:player1") &&
                keys.contains("game:test-game:player:player1:plan") &&
                keys.contains("game:test-game:player:player1:vars")
        ));

        verify(redisTemplate).delete(argThat((List<String> keys) ->
                keys.contains("game:test-game:player:player2") &&
                keys.contains("game:test-game:player:player2:plan") &&
                keys.contains("game:test-game:player:player2:vars")
        ));
    }

    @Test
    void deleteGameData_shouldHandleEmptyPlayerSet() {
        when(redisTemplate.opsForSet().members("game:test-game:players")).thenReturn(null);

        repository.deleteGameData(gameId);

        verify(redisTemplate).delete(argThat((List<String> keys) ->
                keys.contains("game:test-game:info") &&
                keys.contains("game:test-game:players") &&
                keys.contains("game:test-game:territory:size") &&
                keys.contains("game:test-game:territory:regions") &&
                keys.contains("game:test-game:currentState")
        ));

        verify(redisTemplate, times(1)).delete(any(List.class));
    }
}