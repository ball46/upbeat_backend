package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.dto.reids.CurrentStateDTO;
import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.model.Position;
import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.player.PlayerImpl;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.game.state.region.RegionImpl;
import com.example.upbeat_backend.model.User;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import com.example.upbeat_backend.service.UserService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameStateImplTest {

    private static final String GAME_ID = "test-game";
    private static final String PLAYER_ID = "player-1";

    @Mock
    private RedisGameStateRepository repository;

    @Mock
    private UserService userService;

    private GameStateImpl gameState;
    private CurrentStateDTO currentState;
    private Player player;
    private GameInfoDTO gameInfo;
    private GameConfigDTO gameConfig;

    @BeforeEach
    void setUp() {
        gameState = new GameStateImpl(GAME_ID, repository, userService);
        currentState = CurrentStateDTO.builder()
                .currentPlayerId(PLAYER_ID)
                .currentRow(5)
                .currentCol(5)
                .build();
        player = new PlayerImpl(PLAYER_ID, "Player1", 100, 1, 1);
        gameInfo = GameInfoDTO.builder()
                .gameStatus(GameStatus.IN_PROGRESS)
                .currentTurn(10)
                .build();
        gameConfig = GameConfigDTO.builder().interestPct(10).build();
    }

    @Test
    void relocate_shouldSucceed_whenAllConditionsAreMet() {
        Player player = new PlayerImpl(PLAYER_ID, "Player1", 100, 1, 1);

        Region oldCenterRegion = new RegionImpl(500, 1, 1);
        oldCenterRegion.updateOwner(PLAYER_ID);

        Region newCenterRegion = new RegionImpl(300, 5, 5);
        newCenterRegion.updateOwner(PLAYER_ID);

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getRegion(eq(GAME_ID), eq(1), eq(1))).thenReturn(oldCenterRegion);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(newCenterRegion);

        long result = gameState.relocate();

        assertThat(result).isGreaterThan(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(eq(GAME_ID), eq(PLAYER_ID), anyLong());
        verify(repository).updateRegion(eq(GAME_ID), eq(1), eq(1), anyLong(), isNull());
        verify(repository).updateRegion(eq(GAME_ID), eq(5), eq(5), anyLong(), eq(PLAYER_ID));
    }

    @Test
    void relocate_shouldReturnZero_whenNotEnoughBudget() {
        Player player = new PlayerImpl(PLAYER_ID, "Player1", 10, 1, 1);

        Region oldCenterRegion = new RegionImpl(500, 1, 1);
        oldCenterRegion.updateOwner(PLAYER_ID);

        Region newCenterRegion = new RegionImpl(300, 5, 5);
        newCenterRegion.updateOwner(PLAYER_ID);

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getRegion(eq(GAME_ID), eq(1), eq(1))).thenReturn(oldCenterRegion);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(newCenterRegion);

        long result = gameState.relocate();

        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updatePlayerBudget(eq(GAME_ID), eq(PLAYER_ID), anyLong());
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void relocate_shouldReturnZero_whenRegionNotOwnedByPlayer() {
        Player player = new PlayerImpl(PLAYER_ID, "Player1", 100, 1, 1);

        Region oldCenterRegion = new RegionImpl(500, 1, 1);
        oldCenterRegion.updateOwner(PLAYER_ID);

        Region newCenterRegion = new RegionImpl(300, 5, 5);
        newCenterRegion.updateOwner("other-player");

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getRegion(eq(GAME_ID), eq(1), eq(1))).thenReturn(oldCenterRegion);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(newCenterRegion);

        long result = gameState.relocate();

        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updatePlayerBudget(eq(GAME_ID), eq(PLAYER_ID), anyLong());
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void relocate_shouldReturnZero_whenNoInitialBudget() {
        Player player = new PlayerImpl(PLAYER_ID, "Player1", 0, 1, 1);

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        long result = gameState.relocate();

        assertThat(result).isEqualTo(0);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void move_shouldReturnTrue_whenMovingToWasteland() {
        Region wasteRegion = new RegionImpl(0, 4, 5);

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(wasteRegion);

        boolean result = gameState.move(Keyword.UP);

        assertThat(result).isTrue();
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updateCurrentPosition(GAME_ID, 4, 5);
    }

    @Test
    void move_shouldReturnTrue_whenMovingToOwnRegion() {
        Region playerRegion = new RegionImpl(200, 6, 5);
        playerRegion.updateOwner(PLAYER_ID);

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());
        when(repository.getRegion(eq(GAME_ID), eq(6), eq(5))).thenReturn(playerRegion);

        boolean result = gameState.move(Keyword.DOWN);

        assertThat(result).isTrue();
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updateCurrentPosition(GAME_ID, 6, 5);
    }

    @Test
    void move_shouldReturnFalse_whenMovingToOpponentRegion() {
        Region opponentRegion = new RegionImpl(200, 6, 5);
        opponentRegion.updateOwner("opponent-player");

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());
        when(repository.getRegion(eq(GAME_ID), eq(6), eq(5))).thenReturn(opponentRegion);

        boolean result = gameState.move(Keyword.DOWN);

        assertThat(result).isFalse();
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updateCurrentPosition(any(), anyInt(), anyInt());
    }

    @Test
    void move_shouldReturnFalse_whenMovingOutsideTerritory() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(5).cols(5).build());

        boolean result = gameState.move(Keyword.DOWN);

        assertThat(result).isFalse();
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updateCurrentPosition(any(), anyInt(), anyInt());
    }

    @Test
    void move_shouldReturnFalse_whenNotEnoughBudget() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(new PlayerImpl(PLAYER_ID, "Player1", 0, 1, 1));

        boolean result = gameState.move(Keyword.UP);

        assertThat(result).isFalse();
        verify(repository, never()).updateCurrentPosition(any(), anyInt(), anyInt());
    }

    @Test
    void move_shouldCalculateCorrectPosition_whenMovingInEvenColumn() {
        CurrentStateDTO evenColState = CurrentStateDTO.builder()
                .currentPlayerId(PLAYER_ID)
                .currentRow(5)
                .currentCol(6)
                .build();

        when(repository.getCurrentState(GAME_ID)).thenReturn(evenColState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(6))).thenReturn(new RegionImpl(0, 4, 6));

        boolean result = gameState.move(Keyword.UP);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 4, 6);

        when(repository.getRegion(eq(GAME_ID), eq(6), eq(6))).thenReturn(new RegionImpl(0, 6, 6));
        result = gameState.move(Keyword.DOWN);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 6, 6);

        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(new RegionImpl(0, 4, 5));
        result = gameState.move(Keyword.UPLEFT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 4, 5);

        when(repository.getRegion(eq(GAME_ID), eq(4), eq(7))).thenReturn(new RegionImpl(0, 4, 7));
        result = gameState.move(Keyword.UPRIGHT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 4, 7);

        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(new RegionImpl(0, 5, 5));
        result = gameState.move(Keyword.DOWNLEFT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 5, 5);

        when(repository.getRegion(eq(GAME_ID), eq(5), eq(7))).thenReturn(new RegionImpl(0, 5, 7));
        result = gameState.move(Keyword.DOWNRIGHT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 5, 7);
    }

    @Test
    void move_shouldCalculateCorrectPosition_whenMovingInOddColumn() {
        CurrentStateDTO oddColState = CurrentStateDTO.builder()
                .currentPlayerId(PLAYER_ID)
                .currentRow(5)
                .currentCol(5)
                .build();

        when(repository.getCurrentState(GAME_ID)).thenReturn(oddColState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(new RegionImpl(0, 4, 5));
        boolean result = gameState.move(Keyword.UP);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 4, 5);

        when(repository.getRegion(eq(GAME_ID), eq(6), eq(5))).thenReturn(new RegionImpl(0, 6, 5));
        result = gameState.move(Keyword.DOWN);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 6, 5);

        when(repository.getRegion(eq(GAME_ID), eq(5), eq(4))).thenReturn(new RegionImpl(0, 5, 4));
        result = gameState.move(Keyword.UPLEFT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 5, 4);

        when(repository.getRegion(eq(GAME_ID), eq(5), eq(6))).thenReturn(new RegionImpl(0, 5, 6));
        result = gameState.move(Keyword.UPRIGHT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 5, 6);

        when(repository.getRegion(eq(GAME_ID), eq(6), eq(4))).thenReturn(new RegionImpl(0, 6, 4));
        result = gameState.move(Keyword.DOWNLEFT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 6, 4);

        when(repository.getRegion(eq(GAME_ID), eq(6), eq(6))).thenReturn(new RegionImpl(0, 6, 6));
        result = gameState.move(Keyword.DOWNRIGHT);
        assertThat(result).isTrue();
        verify(repository).updateCurrentPosition(GAME_ID, 6, 6);
    }

    @Test
    void invest_shouldReturnZero_whenAmountIsZeroOrNegative() {
        long result = gameState.invest(0);
        assertThat(result).isEqualTo(0);

        result = gameState.invest(-100);
        assertThat(result).isEqualTo(0);

        verify(repository, never()).incrementPlayerBudget(any(), any(), anyLong());
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void invest_shouldReturnZero_whenPlayerHasNoInitialBudget() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(new PlayerImpl(PLAYER_ID, "Player1", 0, 1, 1));

        long result = gameState.invest(100);
        assertThat(result).isEqualTo(0);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void invest_shouldReturnZero_whenPlayerHasInsufficientBudget() {
        PlayerImpl playerWithLowBudget = new PlayerImpl(PLAYER_ID, "Player1", 30, 1, 1);

        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(playerWithLowBudget);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Region adjacentRegion = new RegionImpl(100, 4, 5);
        adjacentRegion.updateOwner(PLAYER_ID);
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(adjacentRegion);

        long result = gameState.invest(50);

        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void invest_shouldReturnZero_whenRegionNotSurrounded() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(new RegionImpl(0, 4, 5));
        when(repository.getRegion(eq(GAME_ID), eq(6), eq(5))).thenReturn(new RegionImpl(0, 6, 5));
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(4))).thenReturn(new RegionImpl(0, 5, 4));
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(6))).thenReturn(new RegionImpl(0, 5, 6));
        when(repository.getRegion(eq(GAME_ID), eq(6), eq(4))).thenReturn(new RegionImpl(0, 6, 4));
        when(repository.getRegion(eq(GAME_ID), eq(6), eq(6))).thenReturn(new RegionImpl(0, 6, 6));

        long result = gameState.invest(50);

        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void invest_shouldSucceed_whenAllConditionsAreMet() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Region currentRegion = new RegionImpl(1000, 5, 5);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(currentRegion);

        Region adjacentRegion = new RegionImpl(100, 4, 5);
        adjacentRegion.updateOwner(PLAYER_ID);
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(adjacentRegion);

        long investAmount = 50;
        long result = gameState.invest(investAmount);

        assertThat(result).isEqualTo(investAmount);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(GAME_ID, PLAYER_ID, player.getBudget());
        verify(repository).updateRegion(eq(GAME_ID), eq(5), eq(5), eq(investAmount), eq(PLAYER_ID));
    }

    @Test
    void invest_shouldLimitAmount_toMaxDeposit() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Region currentRegion = new RegionImpl(50, 5, 5);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(currentRegion);

        Region adjacentRegion = new RegionImpl(100, 4, 5);
        adjacentRegion.updateOwner(PLAYER_ID);
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(adjacentRegion);

        long result = gameState.invest(60);

        assertThat(result).isEqualTo(50);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(GAME_ID, PLAYER_ID, player.getBudget());
        verify(repository).updateRegion(eq(GAME_ID), eq(5), eq(5), eq(50L), eq(PLAYER_ID));
    }

    @Test
    void collect_shouldReturnZero_whenAmountIsZeroOrNegative() {
        long result = gameState.collect(0);
        assertThat(result).isEqualTo(0);

        result = gameState.collect(-100);
        assertThat(result).isEqualTo(0);

        verify(repository, never()).incrementPlayerBudget(any(), any(), anyLong());
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void collect_shouldReturnZero_whenPlayerHasNoInitialBudget() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(new PlayerImpl(PLAYER_ID, "Player1", 0, 1, 1));

        long result = gameState.collect(100);
        assertThat(result).isEqualTo(0);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void collect_shouldReturnZero_whenRegionIsNotOwnedByPlayer() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        Region region = new RegionImpl(500, 5, 5);
        region.updateOwner("other-player");
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(region);

        long result = gameState.collect(100);
        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updatePlayerBudget(eq(GAME_ID), eq(PLAYER_ID), anyLong());
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void collect_shouldReturnZero_whenDepositIsLessThanRequestedAmount() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        Region region = new RegionImpl(50, 5, 5);
        region.updateOwner(PLAYER_ID);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(region);

        long result = gameState.collect(100);
        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updatePlayerBudget(eq(GAME_ID), eq(PLAYER_ID), anyLong());
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void collect_shouldSucceed_whenAllConditionsAreMet() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        Region region = new RegionImpl(500, 5, 5);
        region.updateOwner(PLAYER_ID);
        region.updateDeposit(101);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(region);

        long collectAmount = 100;
        long result = gameState.collect(collectAmount);

        assertThat(result).isEqualTo(collectAmount);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(GAME_ID, PLAYER_ID, player.getBudget());
        verify(repository).updateRegion(eq(GAME_ID), eq(5), eq(5), eq(1L), eq(PLAYER_ID));
    }

    @Test
    void collect_shouldRemoveOwnership_whenCollectingEntireDeposit() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        long depositAmount = 100;
        Region region = new RegionImpl(depositAmount, 5, 5);
        region.updateOwner(PLAYER_ID);
        region.updateDeposit(depositAmount);
        when(repository.getRegion(eq(GAME_ID), eq(5), eq(5))).thenReturn(region);

        long result = gameState.collect(depositAmount);

        assertThat(result).isEqualTo(depositAmount);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(GAME_ID, PLAYER_ID, player.getBudget());
        verify(repository).updateRegion(eq(GAME_ID), eq(5), eq(5), eq(0L), isNull());
    }

    @Test
    void shoot_shouldReturnZero_whenMoneyIsZeroOrNegative() {
        long result = gameState.shoot(Keyword.UP, 0);
        assertThat(result).isEqualTo(0);

        result = gameState.shoot(Keyword.UP, -100);
        assertThat(result).isEqualTo(0);

        verify(repository, never()).incrementPlayerBudget(any(), any(), anyLong());
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void shoot_shouldReturnZero_whenPlayerHasNoInitialBudget() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(new PlayerImpl(PLAYER_ID, "Player1", 0, 1, 1));

        long result = gameState.shoot(Keyword.UP, 50);
        assertThat(result).isEqualTo(0);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void shoot_shouldReturnZero_whenPlayerHasInsufficientBudget() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(new PlayerImpl(PLAYER_ID, "Player1", 30, 1, 1));

        long result = gameState.shoot(Keyword.UP, 50);
        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void shoot_shouldSucceed_whenShootingAtEmptyRegion() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        Region emptyRegion = new RegionImpl(100, 4, 5);
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(emptyRegion);

        long shootAmount = 50;
        long result = gameState.shoot(Keyword.UP, shootAmount);

        assertThat(result).isEqualTo(shootAmount);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(GAME_ID, PLAYER_ID, player.getBudget());
        verify(repository).updateRegion(eq(GAME_ID), eq(4), eq(5), eq(0L), isNull());
    }

    @Test
    void shoot_shouldSucceed_whenShootingAtOpponentRegion() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        String opponentId = "opponent-1";
        Region opponentRegion = new RegionImpl(100, 4, 5);
        opponentRegion.updateOwner(opponentId);
        opponentRegion.updateDeposit(100);
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(opponentRegion);

        long shootAmount = 50;
        long result = gameState.shoot(Keyword.UP, shootAmount);

        assertThat(result).isEqualTo(shootAmount);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(GAME_ID, PLAYER_ID, player.getBudget());
        verify(repository).updateRegion(eq(GAME_ID), eq(4), eq(5), eq(50L), eq(opponentId));
    }

    @Test
    void shoot_shouldRemoveOwnership_whenDepositReducedToZero() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        String opponentId = "opponent-1";
        Region opponentRegion = new RegionImpl(50, 4, 5);
        opponentRegion.updateOwner(opponentId);
        opponentRegion.updateDeposit(50);
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(opponentRegion);

        Player opponent = new PlayerImpl(opponentId, "Opponent", 100, 3, 3);
        when(repository.getPlayer(GAME_ID, opponentId)).thenReturn(opponent);

        long result = gameState.shoot(Keyword.UP, 50);

        assertThat(result).isEqualTo(50);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
        verify(repository).updatePlayerBudget(GAME_ID, PLAYER_ID, player.getBudget());
        verify(repository).updateRegion(eq(GAME_ID), eq(4), eq(5), eq(0L), isNull());
    }

    @Test
    void shoot_shouldDestroyCityCenter_whenShootingAtOpponentCityCenter() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);

        String opponentId = "opponent-1";
        Region cityCenterRegion = spy(new RegionImpl(50, 4, 5));
        cityCenterRegion.updateOwner(opponentId);
        cityCenterRegion.updateDeposit(50);

        doReturn(true).when(cityCenterRegion).isSameRegion(4, 5);
        when(repository.getRegion(eq(GAME_ID), eq(4), eq(5))).thenReturn(cityCenterRegion);

        Player opponent = new PlayerImpl(opponentId, "Opponent", 100, 4, 5);
        when(repository.getPlayer(GAME_ID, opponentId)).thenReturn(opponent);

        long result = gameState.shoot(Keyword.UP, 50);

        assertThat(result).isEqualTo(50);
        verify(repository).savePlayer(eq(GAME_ID), any(Player.class));
        verify(repository).updateRegion(eq(GAME_ID), eq(4), eq(5), eq(0L), isNull());
    }

    @Test
    void opponent_shouldReturnZero_whenPlayerHasNoInitialBudget() {
        Player playerWithNoBudget = new PlayerImpl(PLAYER_ID, "Player1", 0, 1, 1);
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(playerWithNoBudget);

        long result = gameState.opponent();

        assertThat(result).isEqualTo(0);
        verify(repository, never()).updateRegion(any(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void opponent_shouldReturnZero_whenNoOpponentsFound() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Map<String, Region> emptyRegions = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                String key = i + ":" + j;
                emptyRegions.put(key, new RegionImpl(100, i, j));
            }
        }
        when(repository.getAllRegions(GAME_ID)).thenReturn(emptyRegions);

        long result = gameState.opponent();

        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
    }

    @Test
    void opponent_shouldReturnCorrectValue_whenOpponentFoundInOneDirection() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        String opponentId = "opponent-1";

        Map<String, Region> regionMap = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                String key = i + ":" + j;
                Region region = new RegionImpl(100, i, j);
                if (i == 4 && j == 5) region.updateOwner(opponentId);
                regionMap.put(key, region);
            }
        }

        when(repository.getAllRegions(GAME_ID)).thenReturn(regionMap);

        long result = gameState.opponent();

        assertThat(result).isEqualTo(11);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
    }

    @Test
    void opponent_shouldReturnSmallestValue_whenOpponentsFoundInMultipleDirections() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        String opponentId = "opponent-1";
        Map<String, Region> regionMap = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                String key = i + ":" + j;
                Region region = new RegionImpl(100, i, j);
                if (i == 2 && j == 5) region.updateOwner(opponentId);
                else if (i == 7 && j == 5) region.updateOwner(opponentId);
                regionMap.put(key, region);
            }
        }

        when(repository.getAllRegions(GAME_ID)).thenReturn(regionMap);

        long result = gameState.opponent();

        assertThat(result).isEqualTo(13);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
    }

    @Test
    void nearby_shouldReturnZero_whenPlayerHasNoInitialBudget() {
        Player playerWithNoBudget = new PlayerImpl(PLAYER_ID, "Player1", 0, 1, 1);
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(playerWithNoBudget);

        long result = gameState.nearby(Keyword.UP);

        assertThat(result).isEqualTo(0);
        verify(repository, never()).getRegion(any(), anyInt(), anyInt());
    }

    @Test
    void nearby_shouldReturnZero_whenNoOpponentFoundInDirection() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Map<String, Region> emptyRegions = createEmptyRegionMap();
        when(repository.getAllRegions(GAME_ID)).thenReturn(emptyRegions);

        long result = gameState.nearby(Keyword.UP);

        assertThat(result).isEqualTo(0);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
    }

    @Test
    void nearby_shouldCalculateCorrectValue_whenOpponentFoundInDirection() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Map<String, Region> regionMap = createEmptyRegionMap();

        String opponentId = "opponent-1";
        Region opponentRegion = new RegionImpl(100, 3, 5);
        opponentRegion.updateOwner(opponentId);
        opponentRegion.updateDeposit(5);
        regionMap.put("3:5", opponentRegion);

        when(repository.getAllRegions(GAME_ID)).thenReturn(regionMap);

        long result = gameState.nearby(Keyword.UP);

        assertThat(result).isEqualTo(205);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
    }

    @Test
    void nearby_shouldCalculateValueWithDepositModulo_whenDepositIsLargerThanTen() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Map<String, Region> regionMap = createEmptyRegionMap();

        String opponentId = "opponent-1";
        Region opponentRegion = new RegionImpl(100, 3, 5);
        opponentRegion.updateOwner(opponentId);
        opponentRegion.updateDeposit(47);
        regionMap.put("3:5", opponentRegion);

        when(repository.getAllRegions(GAME_ID)).thenReturn(regionMap);

        long result = gameState.nearby(Keyword.UP);

        assertThat(result).isEqualTo(207);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
    }

    @Test
    void nearby_shouldWorkCorrectly_forDifferentDirections() {
        when(repository.getCurrentState(GAME_ID)).thenReturn(currentState);
        when(repository.getPlayer(GAME_ID, PLAYER_ID)).thenReturn(player);
        when(repository.getTerritorySize(GAME_ID)).thenReturn(TerritorySizeDTO.builder().rows(10).cols(10).build());

        Map<String, Region> regionMap = createEmptyRegionMap();

        String opponentId = "opponent-1";
        Region opponentRegion = new RegionImpl(100, 6, 5);
        opponentRegion.updateOwner(opponentId);
        opponentRegion.updateDeposit(3);
        regionMap.put("6:5", opponentRegion);

        when(repository.getAllRegions(GAME_ID)).thenReturn(regionMap);

        long result = gameState.nearby(Keyword.DOWN);

        assertThat(result).isEqualTo(103);
        verify(repository).incrementPlayerBudget(GAME_ID, PLAYER_ID, -1);
    }

    private Map<String, Region> createEmptyRegionMap() {
        Map<String, Region> regionMap = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            for (int j = 1; j <= 10; j++) {
                String key = i + ":" + j;
                regionMap.put(key, new RegionImpl(100, i, j));
            }
        }
        return regionMap;
    }

    @Test
    void getRandom_shouldReturnValueInRange() {
        long result = gameState.getRandom();
        assertThat(result).isGreaterThanOrEqualTo(0);
        assertThat(result).isLessThan(1000);
    }

    @Test
    void calculateInterest_shouldAddInterestToOwnedRegions() {
        when(repository.getGameInfo(GAME_ID)).thenReturn(gameInfo);
        when(repository.getGameConfig(GAME_ID)).thenReturn(gameConfig);

        Map<String, Region> regionMap = getStringRegionMap();

        when(repository.getAllRegions(GAME_ID)).thenReturn(regionMap);

        gameState.calculateInterest();

        verify(repository).updateRegion(GAME_ID, 2, 3, 146L, PLAYER_ID); // 100 + (100 * 10 / 100) = 110
        verify(repository).updateRegion(GAME_ID, 4, 5, 306L, PLAYER_ID); // 200 + (200 * 10 / 100) = 220
        verify(repository).updateRegion(GAME_ID, 8, 9, 471L, "another-player");
    }

    private static @NotNull Map<String, Region> getStringRegionMap() {
        Map<String, Region> regionMap = new HashMap<>();

        Region playerRegion1 = new RegionImpl(500, 2, 3);
        playerRegion1.updateOwner(PLAYER_ID);
        playerRegion1.updateDeposit(100);
        regionMap.put("2:3", playerRegion1);

        Region playerRegion2 = new RegionImpl(500, 4, 5);
        playerRegion2.updateOwner(PLAYER_ID);
        playerRegion2.updateDeposit(200);
        regionMap.put("4:5", playerRegion2);

        Region emptyRegion = new RegionImpl(500, 6, 7);
        emptyRegion.updateDeposit(50);
        regionMap.put("6:7", emptyRegion);

        Region opponentRegion = new RegionImpl(500, 8, 9);
        opponentRegion.updateOwner("another-player");
        opponentRegion.updateDeposit(300);
        regionMap.put("8:9", opponentRegion);
        return regionMap;
    }

    @Test
    void calculateInterest_shouldHandleMaxDeposit() {
        when(repository.getGameInfo(GAME_ID)).thenReturn(gameInfo);
        when(repository.getGameConfig(GAME_ID)).thenReturn(gameConfig);

        Map<String, Region> regionMap = new HashMap<>();

        Region nearMaxRegion = new RegionImpl(500, 2, 3);
        nearMaxRegion.updateOwner(PLAYER_ID);
        nearMaxRegion.updateDeposit(490);
        regionMap.put("2:3", nearMaxRegion);

        when(repository.getAllRegions(GAME_ID)).thenReturn(regionMap);

        gameState.calculateInterest();

        verify(repository).updateRegion(GAME_ID, 2, 3, 500, PLAYER_ID);
    }

    @Test
    void calculateInterest_shouldDoNothing_whenNoOwnedRegions() {
        Map<String, Region> emptyRegionMap = new HashMap<>();
        when(repository.getAllRegions(GAME_ID)).thenReturn(emptyRegionMap);

        gameState.calculateInterest();

        verify(repository, never()).updateRegion(anyString(), anyInt(), anyInt(), anyLong(), any());
    }

    @Test
    void initialize_shouldCreateTerritoryAndAssignCityCenters() {
        GameConfigDTO gameConfig = GameConfigDTO.builder()
                .rows(10)
                .cols(10)
                .initBudget(100)
                .build();
        when(repository.getGameConfig(GAME_ID)).thenReturn(gameConfig);

        List<String> playerIds = Arrays.asList("player-1", "player-2");
        when(repository.getGamePlayers(GAME_ID)).thenReturn(playerIds);

        User mockUser1 = mock(User.class);
        when(mockUser1.getUsername()).thenReturn("Player1Name");
        when(userService.getUserById("player-1")).thenReturn(mockUser1);

        User mockUser2 = mock(User.class);
        when(mockUser2.getUsername()).thenReturn("Player2Name");
        when(userService.getUserById("player-2")).thenReturn(mockUser2);

        gameState.initialize();

        verify(repository, times(playerIds.size())).savePlayer(eq(GAME_ID), any(Player.class));
        verify(repository, times(playerIds.size())).saveRegion(eq(GAME_ID), any(Region.class));
        verify(repository).saveCurrentState(eq(GAME_ID), eq("player-1"), anyInt(), anyInt());
    }

    @Test
    void initialize_shouldEnsureCityCentersAreInWasteland() {
        GameConfigDTO gameConfig = GameConfigDTO.builder()
                .rows(10)
                .cols(10)
                .initBudget(100)
                .build();
        when(repository.getGameConfig(GAME_ID)).thenReturn(gameConfig);

        List<String> playerIds = Collections.singletonList("player-1");
        when(repository.getGamePlayers(GAME_ID)).thenReturn(playerIds);

        User mockUser1 = mock(User.class);
        when(mockUser1.getUsername()).thenReturn("Player1Name");
        when(userService.getUserById("player-1")).thenReturn(mockUser1);

        gameState = spy(gameState);
        doAnswer(new Answer<Position>() {
            private int callCount = 0;

            @Override
            public Position answer(InvocationOnMock invocation) {
                callCount++;
                if (callCount <= 3) {
                    return new Position(1, 1);
                } else {
                    return new Position(5, 5);
                }
            }
        }).when(gameState).randomCityCenter(anyInt(), anyInt());

        gameState.initialize();

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(repository).savePlayer(eq(GAME_ID), playerCaptor.capture());

        Player savedPlayer = playerCaptor.getValue();
        assertThat(savedPlayer.getCityCenterRow()).isEqualTo(1);
        assertThat(savedPlayer.getCityCenterCol()).isEqualTo(1);

        verify(gameState).randomCityCenter(anyInt(), anyInt());
    }

    @Test
    void initialize_shouldSetupCorrectPlayerDataWithInitialBudget() {
        int initialBudget = 150;
        GameConfigDTO gameConfig = GameConfigDTO.builder()
                .rows(10)
                .cols(10)
                .initBudget(initialBudget)
                .build();
        when(repository.getGameConfig(GAME_ID)).thenReturn(gameConfig);

        List<String> playerIds = Arrays.asList("player-1", "player-2");
        when(repository.getGamePlayers(GAME_ID)).thenReturn(playerIds);

        gameState = spy(gameState);
        doReturn(new Position(3, 4), new Position(7, 8))
                .when(gameState).randomCityCenter(anyInt(), anyInt());

        User mockUser1 = mock(User.class);
        when(mockUser1.getUsername()).thenReturn("Player1Name");
        when(userService.getUserById("player-1")).thenReturn(mockUser1);

        User mockUser2 = mock(User.class);
        when(mockUser2.getUsername()).thenReturn("Player2Name");
        when(userService.getUserById("player-2")).thenReturn(mockUser2);

        gameState.initialize();

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        verify(repository, times(2)).savePlayer(eq(GAME_ID), playerCaptor.capture());

        List<Player> savedPlayers = playerCaptor.getAllValues();

        Player player1 = savedPlayers.getFirst();
        assertThat(player1.getId()).isEqualTo("player-1");
        assertThat(player1.getName()).isEqualTo("Player1Name");
        assertThat(player1.getBudget()).isEqualTo(initialBudget);
        assertThat(player1.getCityCenterRow()).isEqualTo(3);
        assertThat(player1.getCityCenterCol()).isEqualTo(4);

        Player player2 = savedPlayers.get(1);
        assertThat(player2.getId()).isEqualTo("player-2");
        assertThat(player2.getName()).isEqualTo("Player2Name");
        assertThat(player2.getBudget()).isEqualTo(initialBudget);
        assertThat(player2.getCityCenterRow()).isEqualTo(7);
        assertThat(player2.getCityCenterCol()).isEqualTo(8);
    }

    @Test
    void initialize_shouldHandleNoPlayersCase() {
        when(repository.getGamePlayers(GAME_ID)).thenReturn(Collections.emptyList());

        gameState.initialize();

        verify(repository, never()).savePlayer(any(), any());
        verify(repository, never()).saveRegion(any(), any());
        verify(repository, never()).saveCurrentState(any(), any(), anyInt(), anyInt());
    }
}