package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.exception.parser.ParserException;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisEnvironment implements Environment {
    private final RedisTemplate<String, Long> redisTemplate;
    private final String keyPrefix;

    public RedisEnvironment(RedisTemplate<String, Long> redisTemplate, String gameId) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = "game:" + gameId + ":vars:";
    }

    @Override
    public void setVariable(String name, long value) {
        redisTemplate.opsForValue().set(keyPrefix + name, value);
    }

    @Override
    public long getVariable(String name) {
        if (!hasVariable(name)) {
            throw new ParserException.UndefinedVariable(name);
        }
        Long value = redisTemplate.opsForValue().get(keyPrefix + name);
        if (value == null) {
            throw new ParserException.UndefinedVariable(name);
        }
        return value;
    }

    @Override
    public boolean hasVariable(String name) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(keyPrefix + name));
    }

    @Override
    public void reset() {
        redisTemplate.delete(redisTemplate.keys(keyPrefix + "*"));
    }
}
