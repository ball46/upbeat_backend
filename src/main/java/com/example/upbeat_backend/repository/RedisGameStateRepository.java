package com.example.upbeat_backend.repository;

import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.region.Region;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
public class RedisGameStateRepository {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisGameStateRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // ======== GAME INFO ========
    public void saveGameInfo(String gameId, GameStatus status, int maxPlayers, int currentTurn) {
        String key = "game:" + gameId + ":info";

        Map<String, Object> gameInfo = new HashMap<>();
        gameInfo.put("status", status.name());
        gameInfo.put("createdAt", Instant.now().getEpochSecond());
        gameInfo.put("maxPlayers", maxPlayers);
        gameInfo.put("currentTurn", currentTurn);
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

    public Map<Object, Object> getGameInfo(String gameId) {
        String key = "game:" + gameId + ":info";
        return redisTemplate.opsForHash().entries(key);
    }

    // ======== PLAYERS ========
    public void addPlayerToGame(String gameId, String playerId) {
        String key = "game:" + gameId + ":players";
        redisTemplate.opsForSet().add(key, playerId);
    }

    public Set<Object> getGamePlayers(String gameId) {
        String key = "game:" + gameId + ":players";
        return redisTemplate.opsForSet().members(key);
    }

    public void removePlayerFromGame(String gameId, String playerId) {
        String key = "game:" + gameId + ":players";
        redisTemplate.opsForSet().remove(key, playerId);
    }

    // ======== PLAYER DATA ========
    public void savePlayer(String gameId, Player player) {
        String key = "game:" + gameId + ":player:" + player.getId();

        Map<String, Object> playerData = new HashMap<>();
        playerData.put("id", player.getId());
        playerData.put("name", player.getName());
        playerData.put("budget", player.getBudget());
        playerData.put("cityCenterRow", player.getCityCenter().getRow());
        playerData.put("cityCenterCol", player.getCityCenter().getCol());

        redisTemplate.opsForHash().putAll(key, playerData);
    }

    public void updatePlayerBudget(String gameId, String playerId, long budget) {
        String key = "game:" + gameId + ":player:" + playerId;
        redisTemplate.opsForHash().put(key, "budget", budget);
    }

    public Map<Object, Object> getPlayer(String gameId, String playerId) {
        String key = "game:" + gameId + ":player:" + playerId;
        return redisTemplate.opsForHash().entries(key);
    }

    // ======== PLAYER PLANS ========
    public void savePlayerPlan(String gameId, String playerId, String plan) {
        String key = "game:" + gameId + ":player:" + playerId + ":plans";
        redisTemplate.opsForList().rightPush(key, plan);
    }

    public void clearPlayerPlans(String gameId, String playerId) {
        String key = "game:" + gameId + ":player:" + playerId + ":plans";
        redisTemplate.delete(key);
    }

    public List<Object> getPlayerPlans(String gameId, String playerId) {
        String key = "game:" + gameId + ":player:" + playerId + ":plans";
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    // ======== PLAYER VARIABLES ========
    public void setPlayerVariable(String gameId, String playerId, String varName, Object value) {
        String key = "game:" + gameId + ":player:" + playerId + ":vars";
        redisTemplate.opsForHash().put(key, varName, value);
    }

    public Object getPlayerVariable(String gameId, String playerId, String varName) {
        String key = "game:" + gameId + ":player:" + playerId + ":vars";
        return redisTemplate.opsForHash().get(key, varName);
    }

    public Map<Object, Object> getAllPlayerVariables(String gameId, String playerId) {
        String key = "game:" + gameId + ":player:" + playerId + ":vars";
        return redisTemplate.opsForHash().entries(key);
    }

    // ======== TERRITORY SIZE ========
    public void saveTerritorySize(String gameId, int rows, int cols) {
        String key = "game:" + gameId + ":territory:size";
        Map<String, Object> size = new HashMap<>();
        size.put("rows", rows);
        size.put("cols", cols);
        redisTemplate.opsForHash().putAll(key, size);
    }

    public Map<Object, Object> getTerritorySize(String gameId) {
        String key = "game:" + gameId + ":territory:size";
        return redisTemplate.opsForHash().entries(key);
    }

    // ======== REGIONS ========
    public void saveRegion(String gameId, Region region) {
        String key = "game:" + gameId + ":territory:regions";
        String field = region.getRow() + ":" + region.getCol();

        Map<String, Object> regionData = new HashMap<>();
        regionData.put("deposit", region.getDeposit());
        regionData.put("owner", region.getOwner() != null ? region.getOwner().getId() : null);

        redisTemplate.opsForHash().put(key, field, regionData);
    }

    public Object getRegion(String gameId, int row, int col) {
        String key = "game:" + gameId + ":territory:regions";
        String field = row + ":" + col;
        return redisTemplate.opsForHash().get(key, field);
    }

    public Map<Object, Object> getAllRegions(String gameId) {
        String key = "game:" + gameId + ":territory:regions";
        return redisTemplate.opsForHash().entries(key);
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
        String key = "game:" + gameId + ":currentState";
        redisTemplate.opsForHash().put(key, "currentPlayerId", playerId);
    }

    public Map<Object, Object> getCurrentState(String gameId) {
        String key = "game:" + gameId + ":currentState";
        return redisTemplate.opsForHash().entries(key);
    }

    // ใช้ลบข้อมูลทั้งหมดของเกมเมื่อเกมจบ
    public void deleteGameData(String gameId) {
        String gameInfoKey = "game:" + gameId + ":info";
        String playersKey = "game:" + gameId + ":players";
        String territorySizeKey = "game:" + gameId + ":territory:size";
        String regionsKey = "game:" + gameId + ":territory:regions";
        String currentStateKey = "game:" + gameId + ":currentState";

        // ลบข้อมูลทั้งหมดของเกม
        redisTemplate.delete(Arrays.asList(
                gameInfoKey, playersKey, territorySizeKey, regionsKey, currentStateKey
        ));

        // ลบข้อมูลผู้เล่น (ต้องดึงรายการผู้เล่นก่อน)
        Set<Object> playerIds = redisTemplate.opsForSet().members(playersKey);
        if (playerIds != null) {
            for (Object playerId : playerIds) {
                String playerKey = "game:" + gameId + ":player:" + playerId;
                String playerPlansKey = playerKey + ":plans";
                String playerVarsKey = playerKey + ":vars";
                redisTemplate.delete(Arrays.asList(playerKey, playerPlansKey, playerVarsKey));
            }
        }
    }
}