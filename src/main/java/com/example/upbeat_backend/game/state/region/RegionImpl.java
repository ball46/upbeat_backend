package com.example.upbeat_backend.game.state.region;

import com.example.upbeat_backend.game.state.player.Player;

public class RegionImpl implements Region {
    private long deposit;
    private final long maxDeposit;
    private final int row;
    private final int col;
    private Player owner;

    public RegionImpl(long maxDeposit, int row, int col) {
        this.maxDeposit = maxDeposit;
        this.row = row;
        this.col = col;
        this.deposit = 0;
        this.owner = null;
    }

    @Override
    public long getDeposit() {
        return deposit;
    }

    @Override
    public void updateDeposit(long amount) {
        this.deposit = Math.max(0, this.deposit + amount);
        this.deposit = Math.min(this.deposit, maxDeposit);
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getCol() {
        return col;
    }

    @Override
    public Player getOwner() {
        return owner;
    }

    @Override
    public void updateOwner(Player owner) {
        this.owner = owner;
    }
}
