package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.region.Region;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

public class GameStateImpl implements GameState {
    private final String gameId;
    private final RedisTemplate<String, Object> redisTemplate;

    private Player currentPlayer;

    public GameStateImpl(String gameId, RedisTemplate<String, Object> redisTemplate) {
        this.gameId = gameId;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean move(Keyword direction) {
        return false;
    }

    @Override
    public void invest(long amount) {

    }

    @Override
    public void collect(long amount) {

    }

    @Override
    public boolean shoot(Keyword direction, long damage) {
        return false;
    }

    @Override
    public long getNearbyInfo(Keyword direction) {
        return 0;
    }

    @Override
    public boolean isOpponentInDirection(Keyword direction) {
        return false;
    }

    @Override
    public long getRow() {
        return 0;
    }

    @Override
    public long getCol() {
        return 0;
    }

    @Override
    public List<Object> getTerritory() {
        return List.of();
    }
}
