package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.dto.reids.CurrentStateDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.repository.RedisGameStateRepository;

import java.util.*;

public class GameStateImpl implements GameState {
    private final String gameId;
    private final RedisGameStateRepository repository;

    public GameStateImpl(String gameId, RedisGameStateRepository repository) {
        this.gameId = gameId;
        this.repository = repository;
    }

    @Override
    public boolean move(Keyword direction) {
        CurrentStateDTO currentState = repository.getCurrentState(gameId);
        int[] newPosition = calculateNewPosition(currentState.getCurrentRow(), currentState.getCurrentCol(), direction);

        if(isValidPosition(newPosition[0], newPosition[1])) {
            repository.updateCurrentPosition(gameId, newPosition[0], newPosition[1]);
            return true;
        }

        return false;
    }

    @Override
    public void invest(long amount) {
        if (amount <= 0) {
            return;
        }

//        Map<Object, Object> currentState = repository.getCurrentState(gameId);
//        String playerId = (String) currentState.get("currentPlayerId");
//        Player playerData = new PlayerImpl(playerId, repository, gameId);
//
//        if (playerData.getBudget() < amount) {
//            amount = playerData.getBudget();
//        }
//
//        repository.updatePlayerBudget(gameId, playerId, playerData.getBudget() - amount);
//
//        Map<Object, Object> regionData = (Map<Object, Object>) repository.getRegion(gameId, currentRow, currentCol);
//        long deposit = regionData != null ? ((Number) regionData.get("deposit")).longValue() : 0;
//
//        RegionImpl region = new RegionImpl(Long.MAX_VALUE, currentRow, currentCol);
//        region.updateDeposit(deposit + amount);
//        region.updateOwner(new PlayerImpl(playerId, "", budget - amount, null));
//        repository.saveRegion(gameId, region);
    }

    @Override
    public void collect(long amount) {

    }

    @Override
    public boolean shoot(Keyword direction, long damage) {
        return false;
    }

    @Override
    public long getNearbyInfo(Keyword direction) {
        return 0;
    }

    @Override
    public boolean isOpponentInDirection(Keyword direction) {
        return false;
    }

    @Override
    public long getRow() {
        return 0;
    }

    @Override
    public long getCol() {
        return 0;
    }

    @Override
    public List<Object> getTerritory() {
        return List.of();
    }

    private int[] calculateNewPosition(int row, int col, Keyword direction) {
        boolean isEvenCol = (col % 2 == 0);

        return switch (direction) {
            case UP -> new int[]{row - 1, col};
            case DOWN -> new int[]{row + 1, col};
            case UPLEFT -> isEvenCol ? new int[]{row - 1, col - 1} : new int[]{row, col - 1};
            case UPRIGHT -> isEvenCol ? new int[]{row - 1, col + 1} : new int[]{row, col + 1};
            case DOWNLEFT -> isEvenCol ? new int[]{row , col - 1} : new int[]{row - 1, col - 1};
            case DOWNRIGHT -> isEvenCol ? new int[]{row , col + 1} : new int[]{row + 1, col + 1};
            default -> new int[]{row, col};
        };
    }

    private boolean isValidPosition(int row, int col) {
        TerritorySizeDTO territorySize = repository.getTerritorySize(gameId);
        int rows = territorySize.getRows();
        int cols = territorySize.getCols();
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}
