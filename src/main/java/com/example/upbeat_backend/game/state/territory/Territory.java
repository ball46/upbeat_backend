package com.example.upbeat_backend.game.state.territory;

import com.example.upbeat_backend.game.state.region.Region;

public interface Territory {
    Region getRegion(int row, int col);
}
