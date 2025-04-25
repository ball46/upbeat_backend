package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.region.Region;

import java.util.List;
import java.util.Map;

public interface GameState {
    boolean move(Keyword direction);

    void invest(long amount);

    void collect(long amount);

    boolean shoot(Keyword direction, long damage);

    long getNearbyInfo(Keyword direction);

    boolean isOpponentInDirection(Keyword direction);

    long getRow();

    long getCol();

    Map<String, Region> getTerritory();

}
