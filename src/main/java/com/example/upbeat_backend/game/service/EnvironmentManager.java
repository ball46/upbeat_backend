package com.example.upbeat_backend.game.service;

import com.example.upbeat_backend.game.runtime.Environment;
import com.example.upbeat_backend.game.runtime.RedisEnvironment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EnvironmentManager {
    private final RedisTemplate<String, Long> redisTemplate;
    private final Map<String, Environment> gameEnvironments = new ConcurrentHashMap<>();

    public EnvironmentManager(RedisTemplate<String, Long> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Environment getEnvironmentForGame(String gameId) {
        return gameEnvironments.computeIfAbsent(gameId,
            id -> new RedisEnvironment(redisTemplate, id));
    }

    public void resetGame(String gameId) {
        if (gameEnvironments.containsKey(gameId)) {
            gameEnvironments.get(gameId).reset();
        }
    }

    public void endGame(String gameId) {
        if (gameEnvironments.containsKey(gameId)) {
            gameEnvironments.get(gameId).reset();
            gameEnvironments.remove(gameId);
        }
    }
}