package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.model.enums.Keyword;

public interface GameEnvironment extends Environment {
    boolean move(Keyword direction);
    void invest(long amount);
    void collect(long amount);
    boolean shoot(Keyword direction, long damage);
    long getNearbyInfo(Keyword direction);
    boolean isOpponentInDirection(Keyword direction);
}
