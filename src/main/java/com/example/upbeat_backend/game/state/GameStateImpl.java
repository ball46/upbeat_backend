package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.dto.reids.CurrentStateDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.game.state.territory.Territory;
import com.example.upbeat_backend.game.state.territory.TerritoryImpl;
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
    public boolean relocate() {
        CurrentStateDTO currentState = payForCommand();
        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());
        Territory territory = new TerritoryImpl(gameId, repository);

        Region cityCenter = territory.getRegion(player.getCityCenterRow(), player.getCityCenterCol());
        Region region = territory.getRegion(currentState.getCurrentRow(), currentState.getCurrentCol());

        if (!territory.isMyRegion(currentState.getCurrentRow(), currentState.getCurrentCol(), player.getId())) return false;

        int distance = calculateHexDistance(cityCenter.getRow(), cityCenter.getCol(), region.getRow(), region.getCol());

        long cost = (5L * distance) + 10;

        if (player.getBudget() < cost) return false;

        player.updateBudget(-cost);
        player.updateCityCenter(currentState.getCurrentRow(), currentState.getCurrentCol());

        cityCenter.updateOwner(null);
        region.updateOwner(player.getId());

        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());
        repository.updateRegion(gameId, cityCenter.getRow(), cityCenter.getCol(), cityCenter.getDeposit(), cityCenter.getOwner());
        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());

        return true;
    }

    @Override
    public boolean move(Keyword direction) {
        CurrentStateDTO currentState = payForCommand();
        Territory territory = new TerritoryImpl(gameId, repository);

        int[] newPosition = calculateNewPosition(currentState.getCurrentRow(), currentState.getCurrentCol(), direction);

        if(isValidPosition(newPosition[0], newPosition[1]) &&
                (territory.isWasteland(newPosition[0], newPosition[1]) ||
                        territory.isMyRegion(newPosition[0], newPosition[1], currentState.getCurrentPlayerId()))) {
            repository.updateCurrentPosition(gameId, newPosition[0], newPosition[1]);
            return true;
        }

        return false;
    }

    @Override
    public void invest(long amount) {
        if (amount <= 0) return;

        CurrentStateDTO currentState = payForCommand();

        if (!RegionSurrounding(currentState)) return;

        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());

        if (player.getBudget() < amount) return;

        Territory territory = new TerritoryImpl(gameId, repository);
        Region region = territory.getRegion(currentState.getCurrentRow(), currentState.getCurrentCol());

        amount = Math.min(amount, region.getMaxDeposit());
        player.updateBudget(-amount);
        region.updateDeposit(amount);
        region.updateOwner(player.getId());

        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());
        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());
    }

    @Override
    public void collect(long amount) {
        if (amount <= 0) return;

        CurrentStateDTO currentState = payForCommand();

        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());

        Territory territory = new TerritoryImpl(gameId, repository);
        Region region = territory.getRegion(currentState.getCurrentRow(), currentState.getCurrentCol());

        if (!territory.isMyRegion(region, player.getId())) return;

        if (region.getDeposit() < amount) return;
        else if (region.getDeposit() == amount) region.updateOwner(null);

        player.updateBudget(amount);
        region.updateDeposit(-amount);

        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());
        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());
    }

    @Override
    public boolean shoot(Keyword direction, long money) {
        if (money <= 0) return false;

        CurrentStateDTO currentState = payForCommand();
        Territory territory = new TerritoryImpl(gameId, repository);
        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());

        if (money > player.getBudget()) return false;

        int[] newPosition = calculateNewPosition(currentState.getCurrentRow(), currentState.getCurrentCol(), direction);
        Region region = territory.getRegion(newPosition[0], newPosition[1]);

        player.updateBudget(-money);
        region.updateDeposit(-money);

        if (region.getDeposit() <= 0) {
            Player owner = repository.getPlayer(gameId, region.getOwner());
            if (region.isSameRegion(owner.getCityCenterRow(), owner.getCityCenterCol())) {
                owner.updateCityCenter(-1, -1);
                // TODO: need to change function to know this player is lose
                repository.removePlayerFromGame(gameId, owner.getId());
            }
            region.updateOwner(null);
        }

        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());
        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());

        return true;
    }

    @Override
    public long opponent() {
        return 0;
    }

    @Override
    public long nearby(Keyword direction) {
        return 0;
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
    public Map<String, Region> getTerritory() {
        Territory territory = new TerritoryImpl(gameId, repository);
        return territory.getRegionMap();
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

    private CurrentStateDTO payForCommand() {
        CurrentStateDTO currentState = repository.getCurrentState(gameId);
        Player currentPlayer = repository.getPlayer(gameId, currentState.getCurrentPlayerId());

        if (currentPlayer.getBudget() <= 0) {
            throw new IllegalStateException("Not enough budget to pay for command");
        }

        repository.incrementPlayerBudget(gameId, currentState.getCurrentPlayerId(), - 1);

        return currentState;
    }

    private boolean RegionSurrounding(CurrentStateDTO currentState) {
        List<Keyword> directions = Keyword.directions();

        for (Keyword direction : directions) {
            int[] newPosition = calculateNewPosition(currentState.getCurrentRow(), currentState.getCurrentCol(), direction);

            Territory territory = new TerritoryImpl(gameId, repository);

            if(isValidPosition(newPosition[0], newPosition[1]) &&
                    territory.isMyRegion(newPosition[0], newPosition[1], currentState.getCurrentPlayerId())) {
                return true;
            }
        }
        return false;
    }

    private int calculateHexDistance(int row1, int col1, int row2, int col2) {
        double z1 = row1 - (col1 - (col1 & 1)) / 2.0;
        double y1 = -(double) col1 - z1;

        double z2 = row2 - (col2 - (col2 & 1)) / 2.0;
        double y2 = -(double) col2 - z2;

        return (int) (Math.max(Math.max(
                Math.abs((double) col2 - (double) col1),
                Math.abs(y2 - y1)),
                Math.abs(z2 - z1)));
    }
}
