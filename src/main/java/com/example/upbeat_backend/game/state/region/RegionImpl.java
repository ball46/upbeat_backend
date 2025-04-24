package com.example.upbeat_backend.game.state.region;

public class RegionImpl implements Region {
    private long deposit;
    private final long maxDeposit;
    private final int row;
    private final int col;
    private String ownerId;

    public RegionImpl(long maxDeposit, int row, int col) {
        this.maxDeposit = maxDeposit;
        this.row = row;
        this.col = col;
        this.deposit = 0;
        this.ownerId = null;
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
    public String getOwner() {
        return ownerId;
    }

    @Override
    public void updateOwner(String ownerId) {
        this.ownerId = ownerId;
    }
}
