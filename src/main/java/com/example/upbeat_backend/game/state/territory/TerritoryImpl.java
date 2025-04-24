package com.example.upbeat_backend.game.state.territory;

import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.repository.RedisGameStateRepository;

import java.util.*;

public class TerritoryImpl implements Territory {
    private final String gameId;
    private final RedisGameStateRepository repository;
    private final Map<String, Region> regionMap;

    public TerritoryImpl(String gameId, RedisGameStateRepository repository) {
        this.gameId = gameId;
        this.repository = repository;
        regionMap = repository.getAllRegions(gameId);
    }

    @Override
    public Region getRegion(int row, int col) {
        String key = row + ":" + col;
        Region region = regionMap.get(key);

        if (region == null) {
            region = repository.getRegion(gameId, row, col);
            regionMap.put(key, region);
        }

        return region;
    }
}
