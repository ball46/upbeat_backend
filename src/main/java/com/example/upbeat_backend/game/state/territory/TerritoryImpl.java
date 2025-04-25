package com.example.upbeat_backend.game.state.territory;

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
    public boolean isMyRegion(int row, int col, String playerId) {
        Region region = getRegion(row, col);
        return region.getOwner().equals(playerId);
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
}
