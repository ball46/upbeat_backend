package com.example.upbeat_backend.game.state.territory;

import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.repository.RedisGameStateRepository;

import java.util.*;

public class TerritoryImpl implements Territory {
    private final String gameId;
    private final RedisGameStateRepository repository;

    public TerritoryImpl(String gameId, RedisGameStateRepository repository) {
        this.gameId = gameId;
        this.repository = repository;
    }

    @Override
    public Map<String, Region> getRegionMap() {
        return repository.getAllRegions(gameId);
    }

    @Override
    public Region getRegion(int row, int col) {
        return repository.getRegion(gameId, row, col);
    }

    @Override
    public Region getRegion(int row, int col, Map<String, Region> regionMap) {
        String key = row + ":" + col;
        return regionMap.get(key);
    }

    @Override
    public boolean isMyRegion(int row, int col, String playerId) {
        Region region = getRegion(row, col);
        return isMyRegion(region, playerId);
    }

    @Override
    public boolean isMyRegion(Region region, String playerId) {
        return region.getOwner().equals(playerId);
    }

    @Override
    public boolean isWasteland(int row, int col) {
        Region region = getRegion(row, col);
        return region.getOwner() == null;
    }

    @Override
    public boolean isRivalLand(Region region, String playerId) {
        return region.getOwner() != null && !region.getOwner().equals(playerId);
    }

    @Override
    public boolean isValidPosition(int row, int col) {
        TerritorySizeDTO territorySize = repository.getTerritorySize(gameId);
        return isValidPosition(row, col, territorySize);
    }

    @Override
    public boolean isValidPosition(int row, int col, TerritorySizeDTO territorySize) {
        int rows = territorySize.getRows();
        int cols = territorySize.getCols();
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}
