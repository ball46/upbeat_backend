package com.example.upbeat_backend.game.state.player;

import com.example.upbeat_backend.game.state.region.Region;

public interface Player {
    String getId();
    String getName();
    long getBudget();
    void updateBudget(long amount);
    Region getCityCenter();
    void updateCityCenter(Region to);
}
