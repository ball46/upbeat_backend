package com.example.upbeat_backend.repository;

import com.example.upbeat_backend.game.dto.reids.CurrentStateDTO;
import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.state.region.*;
import com.example.upbeat_backend.game.state.player.*;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@AllArgsConstructor
@Repository
public class RedisGameStateRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    // ======== GAME INFO ========
    public void initializeGameInfo(String gameId, int maxPlayers) {
        String key = "game:" + gameId + ":info";

        Map<String, Object> gameInfo = new HashMap<>();
        gameInfo.put("status", GameStatus.WAITING_FOR_PLAYERS.name());
        gameInfo.put("createdAt", Instant.now().getEpochSecond());
        gameInfo.put("maxPlayers", maxPlayers);
        gameInfo.put("currentTurn", 1);
        gameInfo.put("lastUpdatedAt", Instant.now().getEpochSecond());

        redisTemplate.opsForHash().putAll(key, gameInfo);
    }

    public void updateGameStatus(String gameId, GameStatus status) {
        String key = "game:" + gameId + ":info";
        redisTemplate.opsForHash().put(key, "status", status.name());
        redisTemplate.opsForHash().put(key, "lastUpdatedAt", Instant.now().getEpochSecond());
    }

    public void incrementTurn(String gameId) {
        String key = "game:" + gameId + ":info";
        redisTemplate.opsForHash().increment(key, "currentTurn", 1);
        redisTemplate.opsForHash().put(key, "lastUpdatedAt", Instant.now().getEpochSecond());
    }

    public GameInfoDTO getGameInfo(String gameId) {
        String key = "game:" + gameId + ":info";
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

        if (data.isEmpty()) return null;

        GameStatus status = GameStatus.valueOf((String) data.get("status"));
        Timestamp createdAt = new Timestamp(((Number) data.get("createdAt")).longValue());
        int maxPlayers = ((Number) data.get("maxPlayers")).intValue();
        int currentTurn = ((Number) data.get("currentTurn")).intValue();
        Timestamp lastUpdatedAt = new Timestamp(((Number) data.get("lastUpdatedAt")).longValue());

        return GameInfoDTO.builder()
                .gameStatus(status)
                .createAt(createdAt)
                .maxPlayers(maxPlayers)
                .currentTurn(currentTurn)
                .lastUpdateAt(lastUpdatedAt)
                .build();
    }

    public List<String> getPlayersWithCityCenters(String gameId) {
        List<String> activePlayers = new ArrayList<>();
        List<String> allPlayers = getGamePlayers(gameId);

        for (String playerId : allPlayers) {
            Player player = getPlayer(gameId, playerId);
            if (player != null && player.getCityCenterRow() >= 0 && player.getCityCenterCol() >= 0) {
                activePlayers.add(playerId);
            }
        }

        return activePlayers;
    }

    public void setGameWinner(String gameId, String playerId) {
        String key = "game:" + gameId + ":info";
        redisTemplate.opsForHash().put(key, "winner", playerId);
        redisTemplate.opsForHash().put(key, "status", GameStatus.FINISHED.name());
        redisTemplate.opsForHash().put(key, "lastUpdatedAt", Instant.now().getEpochSecond());
    }

    // ======== GAME CONFIGURATION ========
    public void saveGameConfig(String gameId, GameConfigDTO configDTO) {
        String key = "game:" + gameId + ":config";

        Map<String, Object> config = new HashMap<>();
        config.put("rows", configDTO.getRows());
        config.put("cols", configDTO.getCols());
        config.put("initPlanMin", configDTO.getInitPlanMin());
        config.put("initPlanSec", configDTO.getInitPlanSec());
        config.put("initBudget", configDTO.getInitBudget());
        config.put("initCenterDep", configDTO.getInitCenterDep());
        config.put("planRevMin", configDTO.getPlanRevMin());
        config.put("planRevSec", configDTO.getPlanRevSec());
        config.put("revCost", configDTO.getRevCost());
        config.put("maxDep", configDTO.getMaxDep());
        config.put("interestPct", configDTO.getInterestPct());

        redisTemplate.opsForHash().putAll(key, config);
    }

    public GameConfigDTO getGameConfig(String gameId) {
        String key = "game:" + gameId + ":config";
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

        if (data.isEmpty()) return null;

        return GameConfigDTO.builder()
                .rows(((Number) data.get("rows")).intValue())
                .cols(((Number) data.get("cols")).intValue())
                .initPlanMin(((Number) data.get("initPlanMin")).intValue())
                .initPlanSec(((Number) data.get("initPlanSec")).intValue())
                .initBudget(((Number) data.get("initBudget")).longValue())
                .initCenterDep(((Number) data.get("initCenterDep")).longValue())
                .planRevMin(((Number) data.get("planRevMin")).intValue())
                .planRevSec(((Number) data.get("planRevSec")).intValue())
                .revCost(((Number) data.get("revCost")).longValue())
                .maxDep(((Number) data.get("maxDep")).longValue())
                .interestPct(((Number) data.get("interestPct")).intValue())
                .build();
    }

    // ======== PLAYERS ========
    public void addPlayerToGame(String gameId, String playerId) {
        String key = "game:" + gameId + ":players";
        redisTemplate.opsForList().rightPush(key, playerId);
    }

    public List<String> getGamePlayers(String gameId) {
        String key = "game:" + gameId + ":players";
        return getStrings(key);
    }

    @NotNull
    private List<String> getStrings(String key) {
        List<Object> plans = redisTemplate.opsForList().range(key, 0, -1);
        if (plans == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object plan : plans) {
            result.add(plan.toString());
        }
        return result;
    }

    public void removePlayerFromGame(String gameId, String playerId) {
        String key = "game:" + gameId + ":players";
        List<Object> players = redisTemplate.opsForList().range(key, 0, -1);
        if (players != null) {
            players.remove(playerId);
            redisTemplate.delete(key);
            for (Object player : players) {
                redisTemplate.opsForList().rightPush(key, player.toString());
            }
        }
    }

    // ======== PLAYER DATA ========
    public void savePlayer(String gameId, Player player) {
        String key = "game:" + gameId + ":player:" + player.getId();

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("id", player.getId());
        playerData.put("name", player.getName());
        playerData.put("budget", player.getBudget());
        playerData.put("cityCenterRow", player.getCityCenterRow());
        playerData.put("cityCenterCol", player.getCityCenterCol());

        redisTemplate.opsForHash().putAll(key, playerData);
    }

    public void updatePlayerBudget(String gameId, String playerId, long budget) {
        String key = "game:" + gameId + ":player:" + playerId;
        redisTemplate.opsForHash().put(key, "budget", budget);
    }

    public void incrementPlayerBudget(String gameId, String playerId, long amount) {
        String key = "game:" + gameId + ":player:" + playerId;
        redisTemplate.opsForHash().increment(key, "budget", amount);
    }

    public Player getPlayer(String gameId, String playerId) {
        String key = "game:" + gameId + ":player:" + playerId;
        Map<Object, Object> playerData = redisTemplate.opsForHash().entries(key);

        if (playerData.isEmpty()) return null;

        String id = (String) playerData.get("id");
        String name = (String) playerData.get("name");
        long budget = ((Number) playerData.get("budget")).longValue();
        int cityCenterRow = ((Number) playerData.get("cityCenterRow")).intValue();
        int cityCenterCol = ((Number) playerData.get("cityCenterCol")).intValue();

        return new PlayerImpl(id, name, budget, cityCenterRow, cityCenterCol);
    }

    // ======== PLAYER PLANS ========
    public void savePlayerPlan(String gameId, String playerId, String plan) {
        String key = "game:" + gameId + ":player:" + playerId + ":plan";
        redisTemplate.opsForValue().set(key, plan);
    }

    public String getPlayerPlan(String gameId, String playerId) {
        String key = "game:" + gameId + ":player:" + playerId + ":plan";
        Object plan = redisTemplate.opsForValue().get(key);
        return plan != null ? plan.toString() : null;
    }

    // ======== PLAYER VARIABLES ========
    public void setPlayerVariable(String gameId, String playerId, String varName, Long value) {
        String key = "game:" + gameId + ":player:" + playerId + ":vars";
        redisTemplate.opsForHash().put(key, varName, value);
    }

    public Long getPlayerVariable(String gameId, String playerId, String varName) {
        String key = "game:" + gameId + ":player:" + playerId + ":vars";
        Object value = redisTemplate.opsForHash().get(key, varName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    public Map<String, Long> getAllPlayerVariables(String gameId, String playerId) {
        String key = "game:" + gameId + ":player:" + playerId + ":vars";
        Map<Object, Object> playerVars = redisTemplate.opsForHash().entries(key);
        if (playerVars.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : playerVars.entrySet()) {
            String varName = (String) entry.getKey();
            Long value = ((Number) entry.getValue()).longValue();
            result.put(varName, value);
        }
        return result;
    }

    // ======== TERRITORY SIZE ========
    public void saveTerritorySize(String gameId, int rows, int cols) {
        String key = "game:" + gameId + ":territory:size";
        Map<String, Integer> size = new HashMap<>();
        size.put("rows", rows);
        size.put("cols", cols);
        redisTemplate.opsForHash().putAll(key, size);
    }

    public TerritorySizeDTO getTerritorySize(String gameId) {
        String key = "game:" + gameId + ":territory:size";
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) return null;
        return TerritorySizeDTO.builder()
                .rows(((Number) data.get("rows")).intValue())
                .cols(((Number) data.get("cols")).intValue())
                .build();
    }

    // ======== REGIONS ========
    public void saveRegion(String gameId, Region region) {
        String key = "game:" + gameId + ":territory:regions";
        String field = region.getRow() + ":" + region.getCol();

        Map<String, Object> regionData = new HashMap<>();
        regionData.put("deposit", region.getDeposit());
        regionData.put("owner", region.getOwner() != null ? region.getOwner() : null);

        redisTemplate.opsForHash().put(key, field, regionData);
    }

    public void updateRegion(String gameId, int row, int col, long deposit, String owner) {
        String key = "game:" + gameId + ":territory:regions";
        String field = row + ":" + col;

        Map<String, Object> regionData = new HashMap<>();
        regionData.put("deposit", deposit);
        regionData.put("owner", owner);

        redisTemplate.opsForHash().put(key, field, regionData);
    }

    public Region getRegion(String gameId, int row, int col) {
        String key = "game:" + gameId + ":territory:regions";
        String field = row + ":" + col;
        Object regionObj = redisTemplate.opsForHash().get(key, field);

        GameConfigDTO gameConfig = getGameConfig(gameId);
        long maxDeposit = gameConfig.getMaxDep();

        if (regionObj == null) {
            return new RegionImpl(maxDeposit, row, col);
        }

        Map<?, ?> regionData = (Map<?, ?>) regionObj;

        return createRegion(maxDeposit, row, col, regionData);
    }

    public Map<String, Region> getAllRegions(String gameId) {
        String key = "game:" + gameId + ":territory:regions";
        Map<Object, Object> rawData = redisTemplate.opsForHash().entries(key);

        GameConfigDTO gameConfig = getGameConfig(gameId);
        long maxDeposit = gameConfig.getMaxDep();

        Map<String, Region> regions = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rawData.entrySet()) {
            String position = (String) entry.getKey();
            Map<?, ?> regionData = (Map<?, ?>) entry.getValue();

            String[] coords = position.split(":");
            int row = Integer.parseInt(coords[0]);
            int col = Integer.parseInt(coords[1]);

            Region region = createRegion(maxDeposit, row, col, regionData);

            regions.put(position, region);
        }

        return regions;
    }

    private Region createRegion(long maxDeposit, int row, int col, Map<?, ?> regionData) {
        Region region = new RegionImpl(maxDeposit, row, col);

        long deposit = ((Number) regionData.get("deposit")).longValue();
        String ownerId = (String) regionData.get("owner");

        if (deposit > 0) region.updateDeposit(deposit);
        if (ownerId != null) {
            region.updateOwner(ownerId);
        }

        return region;
    }

    // ======== CURRENT STATE ========
    public void saveCurrentState(String gameId, String currentPlayerId, int currentRow, int currentCol) {
        String key = "game:" + gameId + ":currentState";
        Map<String, Object> state = new HashMap<>();
        state.put("currentPlayerId", currentPlayerId);
        state.put("currentRow", currentRow);
        state.put("currentCol", currentCol);
        redisTemplate.opsForHash().putAll(key, state);
    }

    public void updateCurrentPosition(String gameId, int row, int col) {
        String key = "game:" + gameId + ":currentState";
        redisTemplate.opsForHash().put(key, "currentRow", row);
        redisTemplate.opsForHash().put(key, "currentCol", col);
    }

    public void updateCurrentPlayer(String gameId, String playerId) {
        Player player = getPlayer(gameId, playerId);
        String key = "game:" + gameId + ":currentState";
        redisTemplate.opsForHash().put(key, "currentPlayerId", playerId);
        redisTemplate.opsForHash().put(key, "currentRow", player.getCityCenterRow());
        redisTemplate.opsForHash().put(key, "currentCol", player.getCityCenterCol());
    }

    public CurrentStateDTO getCurrentState(String gameId) {
        String key = "game:" + gameId + ":currentState";
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) return null;
        return CurrentStateDTO.builder()
                .currentPlayerId((String) data.get("currentPlayerId"))
                .currentRow(((Number) data.get("currentRow")).intValue())
                .currentCol(((Number) data.get("currentCol")).intValue())
                .build();
    }

    // ======== DELETE GAME DATA ========
    public void deleteGameData(String gameId) {
        String gameInfoKey = "game:" + gameId + ":info";
        String playersKey = "game:" + gameId + ":players";
        String territorySizeKey = "game:" + gameId + ":territory:size";
        String regionsKey = "game:" + gameId + ":territory:regions";
        String currentStateKey = "game:" + gameId + ":currentState";

        redisTemplate.delete(Arrays.asList(
                gameInfoKey, playersKey, territorySizeKey, regionsKey, currentStateKey
        ));

        Set<Object> playerIds = redisTemplate.opsForSet().members(playersKey);
        if (playerIds != null) {
            for (Object playerId : playerIds) {
                String playerKey = "game:" + gameId + ":player:" + playerId;
                String playerPlansKey = playerKey + ":plan";
                String playerVarsKey = playerKey + ":vars";
                redisTemplate.delete(Arrays.asList(playerKey, playerPlansKey, playerVarsKey));
            }
        }
    }
}