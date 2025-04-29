package com.example.upbeat_backend.game.state.territory;

import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.state.region.Region;

import java.util.Map;

public interface Territory {
    Map<String, Region> createTerritory(GameConfigDTO config);

    Map<String, Region> getRegionMap();

    Region getRegion(int row, int col);

    Region getRegion(int row, int col, Map<String, Region> regionMap);

    boolean isMyRegion(int row, int col, String playerId);

    boolean isMyRegion(Region region, String playerId);

    boolean isWasteland(int row, int col);

    boolean isWasteland(Region region);

    boolean isRivalLand(Region region, String playerId);

    boolean isValidPosition(int row, int col);

    boolean isValidPosition(int row, int col, TerritorySizeDTO territorySize);

    void clearPlayerOwnership(String gameId, String playerId);
}
