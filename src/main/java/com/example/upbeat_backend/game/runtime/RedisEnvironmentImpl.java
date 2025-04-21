package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.exception.parser.ParserException;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class RedisEnvironmentImpl implements Environment {
    private final RedisGameStateRepository repository;
    @Getter
    private final String gameId;
    private final String playerId;

    @Override
    public void setVariable(String name, long value) {
        repository.setPlayerVariable(gameId, playerId, name, value);
    }

    @Override
    public long getVariable(String name) {
        Object value = repository.getPlayerVariable(gameId, playerId, name);
        if (value == null) {
            throw new ParserException.UndefinedVariable(name);
        }
        return value instanceof Integer ? ((Integer) value).longValue() : (Long) value;
    }

    @Override
    public boolean hasVariable(String name) {
        return repository.getPlayerVariable(gameId, playerId, name) != null;
    }

    @Override
    public void reset() {
        // ล้างตัวแปรทั้งหมดของผู้เล่น
    }
}