package com.example.upbeat_backend.game.state.player;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PlayerImpl implements Player {
    private String id;
    private String name;
    private long budget;
    private int cityCenterRow;
    private int cityCenterCol;

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
    public int getCityCenterRow() {
        return cityCenterRow;
    }

    @Override
    public int getCityCenterCol() {
        return cityCenterCol;
    }

    @Override
    public void updateCityCenter(int col, int row) {
        this.cityCenterCol = col;
        this.cityCenterRow = row;
    }
}
