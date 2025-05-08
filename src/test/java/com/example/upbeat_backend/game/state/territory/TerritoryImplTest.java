package com.example.upbeat_backend.game.state.territory;

import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.TerritorySizeDTO;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.game.state.region.RegionImpl;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerritoryImplTest {

    private static final String GAME_ID = "test-game";

    @Mock
    private RedisGameStateRepository repository;

    private TerritoryImpl territory;

    @BeforeEach
    void setUp() {
        territory = new TerritoryImpl(GAME_ID, repository);
    }

    @Test
    void createTerritory_shouldCreateCorrectRegions() {
        GameConfigDTO config = GameConfigDTO.builder()
                .rows(2)
                .cols(3)
                .maxDep(1000L)
                .build();

        Map<String, Region> result = territory.createTerritory(config);

        assertThat(result).hasSize(6);
        assertThat(result).containsKeys("1:1", "1:2", "1:3", "2:1", "2:2", "2:3");

        Region region = result.get("1:2");
        assertThat(region.getRow()).isEqualTo(1);
        assertThat(region.getCol()).isEqualTo(2);
        assertThat(region.getMaxDeposit()).isEqualTo(1000L);
        assertThat(region.getDeposit()).isEqualTo(0L);
        assertThat(region.getOwner()).isNull();
    }

    @Test
    void getRegionMap_shouldReturnRegionsFromRepository() {
        Map<String, Region> regions = new HashMap<>();
        regions.put("1:1", new RegionImpl(1000L, 1, 1));

        when(repository.getAllRegions(GAME_ID)).thenReturn(regions);

        Map<String, Region> result = territory.getRegionMap();

        assertThat(result).isEqualTo(regions);
        verify(repository).getAllRegions(GAME_ID);
    }

    @Test
    void getRegion_shouldReturnRegionFromRepository() {
        Region region = new RegionImpl(1000L, 3, 4);
        when(repository.getRegion(GAME_ID, 3, 4)).thenReturn(region);

        Region result = territory.getRegion(3, 4);

        assertThat(result).isEqualTo(region);
        verify(repository).getRegion(GAME_ID, 3, 4);
    }

    @Test
    void getRegion_withMap_shouldReturnRegionFromMap() {
        Map<String, Region> regions = new HashMap<>();
        Region region = new RegionImpl(1000L, 3, 4);
        regions.put("3:4", region);

        Region result = territory.getRegion(3, 4, regions);

        assertThat(result).isEqualTo(region);
    }

    @Test
    void isMyRegion_shouldReturnTrue_whenPlayerOwnsRegion() {
        Region region = mock(Region.class);
        when(region.getOwner()).thenReturn("player1");
        when(repository.getRegion(GAME_ID, 3, 4)).thenReturn(region);

        assertThat(territory.isMyRegion(3, 4, "player1")).isTrue();
        assertThat(territory.isMyRegion(region, "player1")).isTrue();
    }

    @Test
    void isMyRegion_shouldReturnFalse_whenPlayerDoesNotOwnRegion() {
        Region region = mock(Region.class);
        when(region.getOwner()).thenReturn("player2");
        when(repository.getRegion(GAME_ID, 3, 4)).thenReturn(region);

        assertThat(territory.isMyRegion(3, 4, "player1")).isFalse();
        assertThat(territory.isMyRegion(region, "player1")).isFalse();
    }

    @Test
    void isWasteland_shouldReturnTrue_whenRegionHasNoOwner() {
        Region region = mock(Region.class);
        when(region.getOwner()).thenReturn(null);
        when(repository.getRegion(GAME_ID, 3, 4)).thenReturn(region);

        assertThat(territory.isWasteland(3, 4)).isTrue();
        assertThat(territory.isWasteland(region)).isTrue();
    }

    @Test
    void isWasteland_shouldReturnFalse_whenRegionHasOwner() {
        Region region = mock(Region.class);
        when(region.getOwner()).thenReturn("player1");
        when(repository.getRegion(GAME_ID, 3, 4)).thenReturn(region);

        assertThat(territory.isWasteland(3, 4)).isFalse();
        assertThat(territory.isWasteland(region)).isFalse();
    }

    @Test
    void isRivalLand_shouldReturnTrue_whenRegionOwnedByDifferentPlayer() {
        Region region = mock(Region.class);
        when(region.getOwner()).thenReturn("player2");

        assertThat(territory.isRivalLand(region, "player1")).isTrue();
    }

    @Test
    void isRivalLand_shouldReturnFalse_whenRegionOwnedBySamePlayer() {
        Region region = mock(Region.class);
        when(region.getOwner()).thenReturn("player1");

        assertThat(territory.isRivalLand(region, "player1")).isFalse();
    }

    @Test
    void isRivalLand_shouldReturnFalse_whenRegionHasNoOwner() {
        Region region = mock(Region.class);
        when(region.getOwner()).thenReturn(null);

        assertThat(territory.isRivalLand(region, "player1")).isFalse();
    }

    @Test
    void isValidPosition_shouldReturnTrue_whenPositionWithinBounds() {
        TerritorySizeDTO size = TerritorySizeDTO.builder().rows(10).cols(10).build();
        when(repository.getTerritorySize(GAME_ID)).thenReturn(size);

        assertThat(territory.isValidPosition(5, 5)).isTrue();
        assertThat(territory.isValidPosition(5, 5, size)).isTrue();
    }

    @Test
    void isValidPosition_shouldReturnFalse_whenPositionOutOfBounds() {
        TerritorySizeDTO size = TerritorySizeDTO.builder().rows(10).cols(10).build();
        when(repository.getTerritorySize(GAME_ID)).thenReturn(size);

        assertThat(territory.isValidPosition(-1, 5)).isFalse();
        assertThat(territory.isValidPosition(5, 11)).isFalse();
        assertThat(territory.isValidPosition(11, 5)).isFalse();

        assertThat(territory.isValidPosition(-1, 5, size)).isFalse();
        assertThat(territory.isValidPosition(5, 11, size)).isFalse();
        assertThat(territory.isValidPosition(11, 5, size)).isFalse();
    }

    @Test
    void clearPlayerOwnership_shouldUpdateRegionsAndRepository() {
        String playerId = "player1";
        Map<String, Region> regions = new HashMap<>();

        Region playerRegion1 = mock(Region.class);
        when(playerRegion1.getOwner()).thenReturn(playerId);
        when(playerRegion1.getRow()).thenReturn(1);
        when(playerRegion1.getCol()).thenReturn(1);
        when(playerRegion1.getDeposit()).thenReturn(500L);

        Region playerRegion2 = mock(Region.class);
        when(playerRegion2.getOwner()).thenReturn(playerId);
        when(playerRegion2.getRow()).thenReturn(2);
        when(playerRegion2.getCol()).thenReturn(2);
        when(playerRegion2.getDeposit()).thenReturn(300L);

        Region otherRegion = mock(Region.class);
        when(otherRegion.getOwner()).thenReturn("player2");

        regions.put("1:1", playerRegion1);
        regions.put("2:2", playerRegion2);
        regions.put("3:3", otherRegion);

        when(repository.getAllRegions(GAME_ID)).thenReturn(regions);

        territory.clearPlayerOwnership(GAME_ID, playerId);

        verify(playerRegion1).updateOwner(null);
        verify(playerRegion2).updateOwner(null);
        verify(otherRegion, never()).updateOwner(any());

        verify(repository).updateRegion(GAME_ID, 1, 1, 500L, null);
        verify(repository).updateRegion(GAME_ID, 2, 2, 300L, null);
        verify(repository, never()).updateRegion(eq(GAME_ID), eq(3), eq(3), anyLong(), any());
    }
}