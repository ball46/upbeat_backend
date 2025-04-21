package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.dto.response.event.GameEvent;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.GameState;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GameEnvironmentImpl extends RedisEnvironmentImpl implements GameEnvironment {
    private final GameState gameState;
    private final List<GameEvent> events = new ArrayList<>();
    private final String playerId;

    public GameEnvironmentImpl(RedisGameStateRepository repository, String gameId, GameState gameState, String playerId) {
        super(repository, gameId, playerId);
        this.gameState = gameState;
        this.playerId = playerId;
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
}
