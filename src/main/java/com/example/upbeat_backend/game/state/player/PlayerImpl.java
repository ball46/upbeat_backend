package com.example.upbeat_backend.game.state.player;

import com.example.upbeat_backend.game.state.region.Region;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PlayerImpl implements Player {
    private String id;
    private String name;
    private long budget;
    private Region cityCenter;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getBudget() {
        return budget;
    }

    @Override
    public void updateBudget(long amount) {
        this.budget = Math.max(0, this.budget + amount);
    }

    @Override
    public Region getCityCenter() {
        return cityCenter;
    }

    @Override
    public void updateCityCenter(Region to) {
        if (cityCenter != null) {
            cityCenter.updateOwner(null);
        }
        cityCenter = to;
        if (cityCenter != null) {
            cityCenter.updateOwner(this);
        }
    }
}
