package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.dto.response.event.EventData;
import com.example.upbeat_backend.game.dto.response.event.GameEvent;
import com.example.upbeat_backend.game.model.Position;
import com.example.upbeat_backend.game.model.enums.EventType;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.GameState;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameEnvironmentImplTest {

    private static final String GAME_ID = "game-123";
    private static final String PLAYER_ID = "player-1";

    @Mock
    private RedisGameStateRepository repository;

    @Mock
    private GameState gameState;

    private GameEnvironmentImpl gameEnvironment;
    private Position position;

    @BeforeEach
    void setUp() {
        position = new Position(5, 5);
        gameEnvironment = new GameEnvironmentImpl(repository, GAME_ID, gameState, PLAYER_ID);
    }

    @Test
    void getGameState_shouldReturnGameState() {
        assertThat(gameEnvironment.getGameState()).isEqualTo(gameState);
    }

    @Test
    void getEvents_shouldReturnEmptyListInitially() {
        assertThat(gameEnvironment.getEvents()).isEmpty();
    }

    @Test
    void getPlayerId_shouldReturnPlayerId() {
        assertThat(gameEnvironment.getPlayerId()).isEqualTo(PLAYER_ID);
    }

    @Test
    void done_shouldCreateEventAndReturnTrue() {
        when(gameState.getPosition()).thenReturn(position);

        boolean result = gameEnvironment.done();

        assertThat(result).isTrue();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.DONE);
        assertThat(event.timestamp()).isNotZero();

        EventData.Done doneData = (EventData.Done) event.data();
        assertThat(doneData.success()).isTrue();
        assertThat(doneData.position()).isEqualTo(position);
    }

    @Test
    void relocate_shouldCallGameStateAndCreateEvent_whenSuccessful() {
        when(gameState.getPosition()).thenReturn(position);

        when(gameState.relocate()).thenReturn(100L);

        boolean result = gameEnvironment.relocate();

        assertThat(result).isTrue();
        verify(gameState).relocate();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.RELOCATE);

        EventData.Relocate relocateData = (EventData.Relocate) event.data();
        assertThat(relocateData.cost()).isEqualTo(100L);
        assertThat(relocateData.success()).isTrue();
        assertThat(relocateData.position()).isEqualTo(position);
    }

    @Test
    void relocate_shouldCallGameStateAndCreateEvent_whenFailed() {
        when(gameState.getPosition()).thenReturn(position);

        when(gameState.relocate()).thenReturn(0L);

        boolean result = gameEnvironment.relocate();

        assertThat(result).isFalse();
        verify(gameState).relocate();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.RELOCATE);

        EventData.Relocate relocateData = (EventData.Relocate) event.data();
        assertThat(relocateData.cost()).isEqualTo(0L);
        assertThat(relocateData.success()).isFalse();
        assertThat(relocateData.position()).isEqualTo(position);
    }

    @Test
    void move_shouldCallGameStateAndCreateEvent_whenSuccessful() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.move(Keyword.UP)).thenReturn(true);

        boolean result = gameEnvironment.move(Keyword.UP);

        assertThat(result).isTrue();
        verify(gameState).move(Keyword.UP);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.MOVE);

        EventData.Move moveData = (EventData.Move) event.data();
        assertThat(moveData.direction()).isEqualTo(Keyword.UP);
        assertThat(moveData.success()).isTrue();
        assertThat(moveData.position()).isEqualTo(position);
    }

    @Test
    void move_shouldCallGameStateAndCreateEvent_whenFailed() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.move(Keyword.DOWN)).thenReturn(false);

        boolean result = gameEnvironment.move(Keyword.DOWN);

        assertThat(result).isFalse();
        verify(gameState).move(Keyword.DOWN);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.MOVE);

        EventData.Move moveData = (EventData.Move) event.data();
        assertThat(moveData.direction()).isEqualTo(Keyword.DOWN);
        assertThat(moveData.success()).isFalse();
        assertThat(moveData.position()).isEqualTo(position);
    }

    @Test
    void invest_shouldCallGameStateAndCreateEvent_whenSuccessful() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.invest(100L)).thenReturn(100L);

        boolean result = gameEnvironment.invest(100L);

        assertThat(result).isTrue();
        verify(gameState).invest(100L);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.INVEST);

        EventData.Invest investData = (EventData.Invest) event.data();
        assertThat(investData.amount()).isEqualTo(100L);
        assertThat(investData.success()).isTrue();
        assertThat(investData.position()).isEqualTo(position);
    }

    @Test
    void invest_shouldCallGameStateAndCreateEvent_whenFailed() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.invest(100L)).thenReturn(0L);

        boolean result = gameEnvironment.invest(100L);

        assertThat(result).isFalse();
        verify(gameState).invest(100L);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.INVEST);

        EventData.Invest investData = (EventData.Invest) event.data();
        assertThat(investData.amount()).isEqualTo(0L);
        assertThat(investData.success()).isFalse();
        assertThat(investData.position()).isEqualTo(position);
    }

    @Test
    void collect_shouldCallGameStateAndCreateEvent_whenSuccessful() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.collect(50L)).thenReturn(50L);

        boolean result = gameEnvironment.collect(50L);

        assertThat(result).isTrue();
        verify(gameState).collect(50L);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.COLLECT);

        EventData.Collect collectData = (EventData.Collect) event.data();
        assertThat(collectData.amount()).isEqualTo(50L);
        assertThat(collectData.success()).isTrue();
        assertThat(collectData.position()).isEqualTo(position);
    }

    @Test
    void collect_shouldCallGameStateAndCreateEvent_whenFailed() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.collect(50L)).thenReturn(0L);

        boolean result = gameEnvironment.collect(50L);

        assertThat(result).isFalse();
        verify(gameState).collect(50L);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.COLLECT);

        EventData.Collect collectData = (EventData.Collect) event.data();
        assertThat(collectData.amount()).isEqualTo(0L);
        assertThat(collectData.success()).isFalse();
        assertThat(collectData.position()).isEqualTo(position);
    }

    @Test
    void shoot_shouldCallGameStateAndCreateEvent_whenSuccessful() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.shoot(Keyword.UPRIGHT, 200L)).thenReturn(200L);

        boolean result = gameEnvironment.shoot(Keyword.UPRIGHT, 200L);

        assertThat(result).isTrue();
        verify(gameState).shoot(Keyword.UPRIGHT, 200L);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.SHOOT);

        EventData.Shoot shootData = (EventData.Shoot) event.data();
        assertThat(shootData.direction()).isEqualTo(Keyword.UPRIGHT);
        assertThat(shootData.money()).isEqualTo(200L);
        assertThat(shootData.success()).isTrue();
        assertThat(shootData.position()).isEqualTo(position);
    }

    @Test
    void shoot_shouldCallGameStateAndCreateEvent_whenFailed() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.shoot(Keyword.UPLEFT, 200L)).thenReturn(0L);

        boolean result = gameEnvironment.shoot(Keyword.UPLEFT, 200L);

        assertThat(result).isFalse();
        verify(gameState).shoot(Keyword.UPLEFT, 200L);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.SHOOT);

        EventData.Shoot shootData = (EventData.Shoot) event.data();
        assertThat(shootData.direction()).isEqualTo(Keyword.UPLEFT);
        assertThat(shootData.money()).isEqualTo(0L);
        assertThat(shootData.success()).isFalse();
        assertThat(shootData.position()).isEqualTo(position);
    }

    @Test
    void opponent_shouldCallGameStateAndCreateEvent_whenOpponentExists() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.opponent()).thenReturn(3L);

        long result = gameEnvironment.opponent();

        assertThat(result).isEqualTo(3L);
        verify(gameState).opponent();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.OPPONENT);

        EventData.Opponent opponentData = (EventData.Opponent) event.data();
        assertThat(opponentData.result()).isEqualTo(3L);
        assertThat(opponentData.success()).isTrue();
        assertThat(opponentData.position()).isEqualTo(position);
    }

    @Test
    void opponent_shouldCallGameStateAndCreateEvent_whenNoOpponentExists() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.opponent()).thenReturn(0L);

        long result = gameEnvironment.opponent();

        assertThat(result).isEqualTo(0L);
        verify(gameState).opponent();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.OPPONENT);

        EventData.Opponent opponentData = (EventData.Opponent) event.data();
        assertThat(opponentData.result()).isEqualTo(0L);
        assertThat(opponentData.success()).isFalse();
        assertThat(opponentData.position()).isEqualTo(position);
    }

    @Test
    void nearby_shouldCallGameStateAndCreateEvent_whenOpponentNearby() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.nearby(Keyword.UP)).thenReturn(2L);

        long result = gameEnvironment.nearby(Keyword.UP);

        assertThat(result).isEqualTo(2L);
        verify(gameState).nearby(Keyword.UP);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.NEARBY);

        EventData.Nearby nearbyData = (EventData.Nearby) event.data();
        assertThat(nearbyData.direction()).isEqualTo(Keyword.UP);
        assertThat(nearbyData.result()).isEqualTo(2L);
        assertThat(nearbyData.success()).isTrue();
        assertThat(nearbyData.position()).isEqualTo(position);
    }

    @Test
    void nearby_shouldCallGameStateAndCreateEvent_whenNoOpponentNearby() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.nearby(Keyword.DOWN)).thenReturn(0L);

        long result = gameEnvironment.nearby(Keyword.DOWN);

        assertThat(result).isEqualTo(0L);
        verify(gameState).nearby(Keyword.DOWN);

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.NEARBY);

        EventData.Nearby nearbyData = (EventData.Nearby) event.data();
        assertThat(nearbyData.direction()).isEqualTo(Keyword.DOWN);
        assertThat(nearbyData.result()).isEqualTo(0L);
        assertThat(nearbyData.success()).isFalse();
        assertThat(nearbyData.position()).isEqualTo(position);
    }

    @Test
    void getRows_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getRows()).thenReturn(10L);

        long result = gameEnvironment.getRows();

        assertThat(result).isEqualTo(10L);
        verify(gameState).getRows();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.ROWS);

        EventData.Rows rowsData = (EventData.Rows) event.data();
        assertThat(rowsData.row()).isEqualTo(10L);
        assertThat(rowsData.position()).isEqualTo(position);
    }

    @Test
    void getCols_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getCols()).thenReturn(10L);

        long result = gameEnvironment.getCols();

        assertThat(result).isEqualTo(10L);
        verify(gameState).getCols();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.COLS);

        EventData.Cols colsData = (EventData.Cols) event.data();
        assertThat(colsData.col()).isEqualTo(10L);
        assertThat(colsData.position()).isEqualTo(position);
    }

    @Test
    void getCurrentRow_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getCurrentRow()).thenReturn(5L);

        long result = gameEnvironment.getCurrentRow();

        assertThat(result).isEqualTo(5L);
        verify(gameState).getCurrentRow();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.CURRENT_ROW);

        EventData.CurrentRow currentRowData = (EventData.CurrentRow) event.data();
        assertThat(currentRowData.currentRow()).isEqualTo(5L);
        assertThat(currentRowData.position()).isEqualTo(position);
    }

    @Test
    void getCurrentCol_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getCurrentCol()).thenReturn(5L);

        long result = gameEnvironment.getCurrentCol();

        assertThat(result).isEqualTo(5L);
        verify(gameState).getCurrentCol();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.CURRENT_COL);

        EventData.CurrentCol currentColData = (EventData.CurrentCol) event.data();
        assertThat(currentColData.currentCol()).isEqualTo(5L);
        assertThat(currentColData.position()).isEqualTo(position);
    }

    @Test
    void getBudget_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getBudget()).thenReturn(500L);

        long result = gameEnvironment.getBudget();

        assertThat(result).isEqualTo(500L);
        verify(gameState).getBudget();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.BUDGET);

        EventData.Budget budgetData = (EventData.Budget) event.data();
        assertThat(budgetData.budget()).isEqualTo(500L);
        assertThat(budgetData.position()).isEqualTo(position);
    }

    @Test
    void getDeposit_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getDeposit()).thenReturn(200L);

        long result = gameEnvironment.getDeposit();

        assertThat(result).isEqualTo(200L);
        verify(gameState).getDeposit();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.DEPOSIT);

        EventData.Deposit depositData = (EventData.Deposit) event.data();
        assertThat(depositData.deposit()).isEqualTo(200L);
        assertThat(depositData.position()).isEqualTo(position);
    }

    @Test
    void getInterest_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getInterest()).thenReturn(20L);

        long result = gameEnvironment.getInterest();

        assertThat(result).isEqualTo(20L);
        verify(gameState).getInterest();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.INTEREST);

        EventData.Interest interestData = (EventData.Interest) event.data();
        assertThat(interestData.interest()).isEqualTo(20L);
        assertThat(interestData.position()).isEqualTo(position);
    }

    @Test
    void getMaxDeposit_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getMaxDeposit()).thenReturn(1000L);

        long result = gameEnvironment.getMaxDeposit();

        assertThat(result).isEqualTo(1000L);
        verify(gameState).getMaxDeposit();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.MAX_DEPOSIT);

        EventData.MaxDeposit maxDepositData = (EventData.MaxDeposit) event.data();
        assertThat(maxDepositData.maxDeposit()).isEqualTo(1000L);
        assertThat(maxDepositData.position()).isEqualTo(position);
    }

    @Test
    void getRandom_shouldCallGameStateAndCreateEvent() {
        when(gameState.getPosition()).thenReturn(position);
        when(gameState.getRandom()).thenReturn(42L);

        long result = gameEnvironment.getRandom();

        assertThat(result).isEqualTo(42L);
        verify(gameState).getRandom();

        List<GameEvent> events = gameEnvironment.getEvents();
        assertThat(events).hasSize(1);

        GameEvent event = events.getFirst();
        assertThat(event.eventType()).isEqualTo(EventType.RANDOM);

        EventData.Random randomData = (EventData.Random) event.data();
        assertThat(randomData.random()).isEqualTo(42L);
        assertThat(randomData.position()).isEqualTo(position);
    }
}