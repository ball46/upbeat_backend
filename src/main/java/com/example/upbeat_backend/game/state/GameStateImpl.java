package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.dto.reids.CurrentStateDTO;
import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.model.Position;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.player.PlayerImpl;
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
    public long relocate() {
        CurrentStateDTO currentState = payForCommand();
        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());
        Territory territory = new TerritoryImpl(gameId, repository);

        Region cityCenter = territory.getRegion(player.getCityCenterRow(), player.getCityCenterCol());
        Region region = territory.getRegion(currentState.getCurrentRow(), currentState.getCurrentCol());

        if (!territory.isMyRegion(currentState.getCurrentRow(), currentState.getCurrentCol(), player.getId())) return 0;

        int distance = calculateHexDistance(cityCenter.getRow(), cityCenter.getCol(), region.getRow(), region.getCol());

        long cost = (5L * distance) + 10;

        if (player.getBudget() < cost) return 0;

        player.updateBudget(-cost);
        player.updateCityCenter(currentState.getCurrentRow(), currentState.getCurrentCol());

        cityCenter.updateOwner(null);
        region.updateOwner(player.getId());

        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());
        repository.updateRegion(gameId, cityCenter.getRow(), cityCenter.getCol(), cityCenter.getDeposit(), cityCenter.getOwner());
        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());

        return cost;
    }

    @Override
    public boolean move(Keyword direction) {
        CurrentStateDTO currentState = payForCommand();
        Territory territory = new TerritoryImpl(gameId, repository);

        Position newPosition = calculateNewPosition(currentState.getCurrentRow(), currentState.getCurrentCol(), direction);

        if(territory.isValidPosition(newPosition.row(), newPosition.col()) &&
                (territory.isWasteland(newPosition.row(), newPosition.col()) ||
                        territory.isMyRegion(newPosition.row(), newPosition.col(), currentState.getCurrentPlayerId()))) {
            repository.updateCurrentPosition(gameId, newPosition.row(), newPosition.col());
            return true;
        }

        return false;
    }

    @Override
    public long invest(long amount) {
        if (amount <= 0) return 0;

        CurrentStateDTO currentState = payForCommand();

        if (!RegionSurrounding(currentState)) return 0;

        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());

        if (player.getBudget() < amount) return 0;

        Territory territory = new TerritoryImpl(gameId, repository);
        Region region = territory.getRegion(currentState.getCurrentRow(), currentState.getCurrentCol());

        amount = Math.min(amount, region.getMaxDeposit());
        player.updateBudget(-amount);
        region.updateDeposit(amount);
        region.updateOwner(player.getId());

        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());
        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());
        return amount;
    }

    @Override
    public long collect(long amount) {
        if (amount <= 0) return 0;

        CurrentStateDTO currentState = payForCommand();

        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());

        Territory territory = new TerritoryImpl(gameId, repository);
        Region region = territory.getRegion(currentState.getCurrentRow(), currentState.getCurrentCol());

        if (!territory.isMyRegion(region, player.getId())) return 0;

        if (region.getDeposit() < amount) return 0;
        else if (region.getDeposit() == amount) region.updateOwner(null);

        player.updateBudget(amount);
        region.updateDeposit(-amount);

        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());
        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());
        return amount;
    }

    @Override
    public long shoot(Keyword direction, long money) {
        if (money <= 0) return 0;

        CurrentStateDTO currentState = payForCommand();
        Territory territory = new TerritoryImpl(gameId, repository);
        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());

        if (money > player.getBudget()) return 0;

        Position newPosition = calculateNewPosition(currentState.getCurrentRow(), currentState.getCurrentCol(), direction);
        Region region = territory.getRegion(newPosition.row(), newPosition.col());

        player.updateBudget(-money);
        region.updateDeposit(-money);

        if (region.getDeposit() <= 0) {
            String ownerId = region.getOwner();
            if (ownerId != null) {
                Player owner = repository.getPlayer(gameId, region.getOwner());
                if (region.isSameRegion(owner.getCityCenterRow(), owner.getCityCenterCol())) {
                    owner.updateCityCenter(-1, -1);
                    repository.removePlayerFromGame(gameId, owner.getId());
                    territory.clearPlayerOwnership(gameId, ownerId);
                }
                region.updateOwner(null);
            }
        }

        repository.updatePlayerBudget(gameId, player.getId(), player.getBudget());
        repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());

        return money;
    }

    @Override
    public long opponent() {
        CurrentStateDTO currentState = payForCommand();
        TerritorySizeDTO territorySize = repository.getTerritorySize(gameId);
        Territory territory = new TerritoryImpl(gameId, repository);
        Map<String, Region> regionMap = territory.getRegionMap();

        List<Keyword> directions = Keyword.directions();
        Map<Keyword, Integer> directionValues = new HashMap<>();
        Map<Long, Keyword> results = new TreeMap<>();

        for (int i = 0; i < directions.size(); i++) {
            directionValues.put(directions.get(i), i + 1);
        }

        for (Keyword direction : directions) {
            int crewRow = currentState.getCurrentRow();
            int crewCol = currentState.getCurrentCol();

            for (int distance = 1; territory.isValidPosition(crewRow, crewCol, territorySize); distance++) {
                Position newPosition = calculateNewPosition(crewRow, crewCol, direction);

                if (!territory.isValidPosition(newPosition.row(), newPosition.col(), territorySize)) break;

                Region searchRegion = territory.getRegion(newPosition.row(), newPosition.col(), regionMap);

                if (territory.isRivalLand(searchRegion, currentState.getCurrentPlayerId())) {
                    long result = (long) distance * 10 + directionValues.get(direction);
                    results.put(result, direction);
                    break;
                }

                crewRow = newPosition.row();
                crewCol = newPosition.col();
            }
        }

        return results.isEmpty() ? 0 : results.keySet().iterator().next();
    }

    @Override
    public long nearby(Keyword direction) {
        CurrentStateDTO currentState = payForCommand();
        TerritorySizeDTO territorySize = repository.getTerritorySize(gameId);
        Territory territory = new TerritoryImpl(gameId, repository);
        Map<String, Region> regionMap = territory.getRegionMap();

        int crewRow = currentState.getCurrentRow();
        int crewCol = currentState.getCurrentCol();

        for (int distance = 1; territory.isValidPosition(crewRow, crewCol, territorySize); distance++) {
            Position newPosition = calculateNewPosition(crewRow, crewCol, direction);

            if (!territory.isValidPosition(newPosition.row(), newPosition.col(), territorySize)) break;

            Region searchRegion = territory.getRegion(newPosition.row(), newPosition.col(), regionMap);

            if (territory.isRivalLand(searchRegion, currentState.getCurrentPlayerId())) {
                return (long) 100 * distance + (searchRegion.getDeposit() % 10);
            }

            crewRow = newPosition.row();
            crewCol = newPosition.col();
        }

        return 0;
    }

    @Override
    public Position getPosition() {
        CurrentStateDTO currentState = repository.getCurrentState(gameId);
        return new Position(currentState.getCurrentRow(), currentState.getCurrentCol());
    }

    @Override
    public Map<String, Region> getTerritory() {
        Territory territory = new TerritoryImpl(gameId, repository);
        return territory.getRegionMap();
    }

    @Override
    public long getRows() {
        TerritorySizeDTO territorySize = repository.getTerritorySize(gameId);
        return territorySize.getRows();
    }

    @Override
    public long getCols() {
        TerritorySizeDTO territorySize = repository.getTerritorySize(gameId);
        return territorySize.getCols();
    }

    @Override
    public long getCurrentRow() {
        CurrentStateDTO currentState = repository.getCurrentState(gameId);
        return currentState.getCurrentRow();
    }

    @Override
    public long getCurrentCol() {
        CurrentStateDTO currentState = repository.getCurrentState(gameId);
        return currentState.getCurrentCol();
    }

    @Override
    public long getBudget() {
        CurrentStateDTO currentState = repository.getCurrentState(gameId);
        Player player = repository.getPlayer(gameId, currentState.getCurrentPlayerId());
        return player.getBudget();
    }

    @Override
    public long getDeposit() {
        CurrentStateDTO currentState = repository.getCurrentState(gameId);
        Territory territory = new TerritoryImpl(gameId, repository);
        Region region = territory.getRegion(currentState.getCurrentRow(), currentState.getCurrentCol());
        return territory.isWasteland(region) ? - region.getDeposit() : region.getDeposit();
    }

    @Override
    public long getInterest() {
        GameConfigDTO gameConfig = repository.getGameConfig(gameId);
        return gameConfig.getInterestPct();
    }

    @Override
    public long getMaxDeposit() {
        GameConfigDTO gameConfig = repository.getGameConfig(gameId);
        return gameConfig.getMaxDep();
    }

    @Override
    public long getRandom() {
        Random random = new Random();
        return random.nextInt(1000);
    }

    public void calculateInterest() {
        Territory territory = new TerritoryImpl(gameId, repository);
        Map<String, Region> regions = territory.getRegionMap();

        for (Region region : regions.values()) {
            if (territory.isWasteland(region)) continue;
            double percent = calculateBaseInterestPercent(region.getDeposit());
            double interest = region.getDeposit() * percent / 100.0;
            region.updateDeposit(Math.round(interest));
            repository.updateRegion(gameId, region.getRow(), region.getCol(), region.getDeposit(), region.getOwner());
        }
    }

    private double calculateBaseInterestPercent(long deposit) {
        GameConfigDTO gameConfig = repository.getGameConfig(gameId);
        long interestRate = gameConfig.getInterestPct();
        GameInfoDTO gameInfo = repository.getGameInfo(gameId);
        int turn = gameInfo.getCurrentTurn();
        return interestRate * Math.log10(deposit) * Math.log(turn);
    }

    private Position calculateNewPosition(int row, int col, Keyword direction) {
        boolean isEvenCol = (col % 2 == 0);

        return switch (direction) {
            case UP -> new Position(row - 1, col);
            case DOWN -> new Position(row + 1, col);
            case UPLEFT -> isEvenCol ? new Position(row - 1, col - 1) : new Position(row, col - 1);
            case UPRIGHT -> isEvenCol ? new Position(row - 1, col + 1) : new Position(row, col + 1);
            case DOWNLEFT -> isEvenCol ? new Position(row , col - 1) : new Position(row - 1, col - 1);
            case DOWNRIGHT -> isEvenCol ? new Position(row , col + 1) : new Position(row + 1, col + 1);
            default -> new Position(row, col);
        };
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
            Position newPosition = calculateNewPosition(currentState.getCurrentRow(), currentState.getCurrentCol(), direction);

            Territory territory = new TerritoryImpl(gameId, repository);

            if(territory.isValidPosition(newPosition.row(), newPosition.col()) &&
                    territory.isMyRegion(newPosition.row(), newPosition.col(), currentState.getCurrentPlayerId())) {
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

    public void initialize() {
        GameConfigDTO config = repository.getGameConfig(gameId);
        List<String> players = repository.getGamePlayers(gameId);
        Territory territory = new TerritoryImpl(gameId, repository);
        Map<String, Region> regionMap = territory.createTerritory(config);
        String firstPlayerId = players.getFirst();

        for(String playerId : players) {
            Position cityCenter;
            Region region;
            do {
                cityCenter = randomCityCenter(config.getRows(), config.getCols());
                region = territory.getRegion(cityCenter.row(), cityCenter.col(), regionMap);
            } while(!territory.isWasteland(region));

            Player player = new PlayerImpl(playerId, "", config.getInitBudget(), cityCenter.row(), cityCenter.col());
            region.updateOwner(player.getId());

            repository.savePlayer(gameId, player);
            repository.saveRegion(gameId, region);

            if (playerId.equals(firstPlayerId)) {
                repository.saveCurrentState(gameId, player.getId(), player.getCityCenterRow(), player.getCityCenterCol());
            }
        }
    }

    private Position randomCityCenter(int rows, int cols) {
        Random random = new Random();
        int row = random.nextInt(rows) + 1;
        int col = random.nextInt(cols) + 1;
        return new Position(row, col);
    }
}
