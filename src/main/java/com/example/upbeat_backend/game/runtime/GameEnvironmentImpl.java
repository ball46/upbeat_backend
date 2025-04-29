package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.dto.response.event.EventData;
import com.example.upbeat_backend.game.dto.response.event.GameEvent;
import com.example.upbeat_backend.game.model.enums.EventType;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.GameState;
import com.example.upbeat_backend.repository.RedisGameStateRepository;

import java.util.*;

public class GameEnvironmentImpl extends RedisEnvironmentImpl implements GameEnvironment {
    private final GameState gameState;
    private final List<GameEvent> events = new ArrayList<>();
    private final String playerId;

    public GameEnvironmentImpl(RedisGameStateRepository repository, String gameId, GameState gameState, String playerId) {
        super(repository, gameId, playerId);
        this.gameState = gameState;
        this.playerId = playerId;
    }

    @Override
    public GameState getGameState() {
        return gameState;
    }

    @Override
    public List<GameEvent> getEvents() {
        return events;
    }

    @Override
    public String getPlayerId() {
        return playerId;
    }

    @Override
    public boolean done() {
        EventData data = EventData.Done.builder()
                .success(true)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.DONE, data);
        events.add(event);

        return true;
    }

    @Override
    public boolean relocate() {
        long cost = gameState.relocate();

        EventData data = EventData.Relocate.builder()
                .cost(cost)
                .success(cost > 0)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.RELOCATE, data);
        events.add(event);

        return cost > 0;
    }

    @Override
    public boolean move(Keyword direction) {
        boolean result = gameState.move(direction);

        EventData data = EventData.Move.builder()
                .direction(direction)
                .success(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.MOVE, data);
        events.add(event);
        return result;
    }

    @Override
    public boolean invest(long amount) {
        long cost = gameState.invest(amount);

        EventData data = EventData.Invest.builder()
                .amount(cost)
                .success(cost > 0)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.INVEST, data);
        events.add(event);

        return cost > 0;
    }

    @Override
    public boolean collect(long amount) {
        long result = gameState.collect(amount);

        EventData data = EventData.Collect.builder()
                .amount(result)
                .success(result > 0)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.COLLECT, data);
        events.add(event);

        return result > 0;
    }

    @Override
    public boolean shoot(Keyword direction, long damage) {
        long cost = gameState.shoot(direction, damage);

        EventData data = EventData.Shoot.builder()
                .direction(direction)
                .money(cost)
                .success(cost > 0)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.SHOOT, data);
        events.add(event);

        return cost > 0;
    }

    @Override
    public long opponent() {
        long result = gameState.opponent();

        EventData data = EventData.Opponent.builder()
                .result(result)
                .success(result > 0)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.OPPONENT, data);
        events.add(event);

        return result;
    }

    @Override
    public long nearby(Keyword direction) {
        long result = gameState.nearby(direction);

        EventData data = EventData.Nearby.builder()
                .direction(direction)
                .result(result)
                .success(result > 0)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.NEARBY, data);
        events.add(event);

        return result;
    }

    @Override
    public long getRows() {
        long result = gameState.getRows();

        EventData data = EventData.Rows.builder()
                .row(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.ROWS, data);
        events.add(event);

        return result;
    }

    @Override
    public long getCols() {
        long result = gameState.getCols();

        EventData data = EventData.Cols.builder()
                .col(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.COLS, data);
        events.add(event);

        return result;
    }

    @Override
    public long getCurrentRow() {
        long result = gameState.getCurrentRow();

        EventData data = EventData.CurrentRow.builder()
                .currentRow(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.CURRENT_ROW, data);
        events.add(event);

        return result;
    }

    @Override
    public long getCurrentCol() {
        long result = gameState.getCurrentCol();

        EventData data = EventData.CurrentCol.builder()
                .currentCol(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.CURRENT_COL, data);
        events.add(event);

        return result;
    }

    @Override
    public long getBudget() {
        long result = gameState.getBudget();

        EventData data = EventData.Budget.builder()
                .budget(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.BUDGET, data);
        events.add(event);

        return result;
    }

    @Override
    public long getDeposit() {
        long result = gameState.getDeposit();

        EventData data = EventData.Deposit.builder()
                .deposit(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.DEPOSIT, data);
        events.add(event);

        return result;
    }

    @Override
    public long getInterest() {
        long result = gameState.getInterest();

        EventData data = EventData.Interest.builder()
                .interest(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.INTEREST, data);
        events.add(event);

        return result;
    }

    @Override
    public long getMaxDeposit() {
        long result = gameState.getMaxDeposit();

        EventData data = EventData.MaxDeposit.builder()
                .maxDeposit(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.MAX_DEPOSIT, data);
        events.add(event);

        return result;
    }

    @Override
    public long getRandom() {
        long result = gameState.getRandom();

        EventData data = EventData.Random.builder()
                .random(result)
                .position(gameState.getPosition())
                .build();
        GameEvent event = createEvent(EventType.RANDOM, data);
        events.add(event);

        return result;
    }

    private GameEvent createEvent(EventType eventType, EventData data) {
        return GameEvent.builder()
                .eventType(eventType)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
