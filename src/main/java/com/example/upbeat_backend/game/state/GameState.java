package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.model.enums.Keyword;

import java.util.List;

public interface GameState {
    boolean move(Keyword direction);

    void invest(long amount);

    void collect(long amount);

    boolean shoot(Keyword direction, long damage);

    long getNearbyInfo(Keyword direction);

    boolean isOpponentInDirection(Keyword direction);

    long getRow();

    long getCol();

    List<Object> getTerritory();

}
