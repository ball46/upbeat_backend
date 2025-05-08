package com.example.upbeat_backend.game.state.player;

public interface Player {
    String getId();
    String getName();
    long getBudget();
    void updateBudget(long amount);
    int getCityCenterRow();
    int getCityCenterCol();
    void updateCityCenter(int col, int row);
}
