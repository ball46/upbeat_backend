package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.region.Region;

import java.util.Map;

public interface GameState {
    boolean relocate();

    boolean move(Keyword direction);

    void invest(long amount);

    void collect(long amount);

    boolean shoot(Keyword direction, long money);

    long opponent();

    long nearby(Keyword direction);

    long getRow();

    long getCol();

    Map<String, Region> getTerritory();

}
