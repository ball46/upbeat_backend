package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.model.Position;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.region.Region;

import java.util.Map;

public interface GameState {
    long relocate();

    boolean move(Keyword direction);

    long invest(long amount);

    long collect(long amount);

    long shoot(Keyword direction, long money);

    long opponent();

    long nearby(Keyword direction);

    Position getPosition();

    Map<String, Region> getTerritory();

    long getRows();

    long getCols();

    long getCurrentRow();

    long getCurrentCol();

    long getBudget();

    long getDeposit();

    long getInterest();

    long getMaxDeposit();

    long getRandom();
}
