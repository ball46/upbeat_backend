package com.example.upbeat_backend.game.state.region;

public interface Region {
    long getMaxDeposit();

    long getDeposit();

    void updateDeposit(long amount);

    int getRow();

    int getCol();

    String getOwner();

    void updateOwner(String ownerId);

    boolean isSameRegion(int row, int col);
}
