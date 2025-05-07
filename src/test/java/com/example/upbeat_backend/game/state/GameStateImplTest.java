package com.example.upbeat_backend.game.state;

import com.example.upbeat_backend.game.dto.reids.CurrentStateDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.player.PlayerImpl;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.game.state.region.RegionImpl;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import com.example.upbeat_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

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

    @BeforeEach
    void setUp() {
        gameState = new GameStateImpl(GAME_ID, repository, userService);
        currentState = CurrentStateDTO.builder()
                .currentPlayerId(PLAYER_ID)
                .currentRow(5)
                .currentCol(5)
                .build();
        player = new PlayerImpl(PLAYER_ID, "Player1", 100, 1, 1);
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
}