package com.example.upbeat_backend.game.service;

import com.example.upbeat_backend.game.runtime.Environment;
import com.example.upbeat_backend.game.runtime.GameEnvironment;
import com.example.upbeat_backend.game.runtime.RedisEnvironmentImpl;
import com.example.upbeat_backend.game.runtime.GameEnvironmentImpl;
import com.example.upbeat_backend.game.state.GameState;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EnvironmentManager {
    private final RedisGameStateRepository repository;
    private final Map<String, Environment> gameEnvironments = new ConcurrentHashMap<>();

    public EnvironmentManager(RedisGameStateRepository repository) {
        this.repository = repository;
    }

    public Environment getEnvironmentForGame(String gameId, String playerId) {
        String key = gameId + ":" + playerId;
        return gameEnvironments.computeIfAbsent(key,
            id -> new RedisEnvironmentImpl(repository, gameId, playerId));
    }

    public GameEnvironment getGameEnvironmentForGame(String gameId, GameState gameState, String playerId) {
        String key = gameId + ":" + playerId;
        return (GameEnvironment) gameEnvironments.computeIfAbsent(key,
                id -> new GameEnvironmentImpl(repository, gameId, gameState, playerId));
    }

    public void resetGame(String gameId) {
        gameEnvironments.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            if (key.startsWith(gameId + ":")) {
                entry.getValue().reset();
                return true;
            }
            return false;
        });
    }

    public void endGame(String gameId) {
        resetGame(gameId);
        repository.deleteGameData(gameId);
    }
}