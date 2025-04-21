package com.example.upbeat_backend.game.state.region;

import com.example.upbeat_backend.game.state.player.Player;

public interface Region {
    long getDeposit();
    void updateDeposit(long amount);
    int getRow();
    int getCol();
    Player getOwner();
    void updateOwner(Player owner);
}
