package com.example.upbeat_backend.game.state.region;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegionImplTest {

    @Test
    void constructor_shouldInitializeFieldsCorrectly() {
        RegionImpl region = new RegionImpl(5000, 3, 4);

        assertThat(region.getMaxDeposit()).isEqualTo(5000);
        assertThat(region.getRow()).isEqualTo(3);
        assertThat(region.getCol()).isEqualTo(4);
        assertThat(region.getDeposit()).isEqualTo(0);
        assertThat(region.getOwner()).isNull();
    }

    @Test
    void updateDeposit_shouldAddAmount_whenPositiveValue() {
        RegionImpl region = new RegionImpl(5000, 3, 4);

        region.updateDeposit(1000);

        assertThat(region.getDeposit()).isEqualTo(1000);
    }

    @Test
    void updateDeposit_shouldSubtractAmount_whenNegativeValue() {
        RegionImpl region = new RegionImpl(5000, 3, 4);
        region.updateDeposit(1000); // Set initial deposit

        region.updateDeposit(-300);

        assertThat(region.getDeposit()).isEqualTo(700);
    }

    @Test
    void updateDeposit_shouldSetToZero_whenResultWouldBeNegative() {
        RegionImpl region = new RegionImpl(5000, 3, 4);

        region.updateDeposit(500);

        region.updateDeposit(-800);

        assertThat(region.getDeposit()).isEqualTo(0);
    }

    @Test
    void updateDeposit_shouldCapAtMaxDeposit_whenExceedingMaximum() {
        RegionImpl region = new RegionImpl(5000, 3, 4);

        region.updateDeposit(7000);

        assertThat(region.getDeposit()).isEqualTo(5000);
    }

    @Test
    void updateOwner_shouldChangeOwner() {
        RegionImpl region = new RegionImpl(5000, 3, 4);

        region.updateOwner("player1");

        assertThat(region.getOwner()).isEqualTo("player1");
    }

    @Test
    void isSameRegion_shouldReturnTrue_whenCoordinatesMatch() {
        RegionImpl region = new RegionImpl(5000, 3, 4);

        assertThat(region.isSameRegion(3, 4)).isTrue();
    }

    @Test
    void isSameRegion_shouldReturnFalse_whenCoordinatesDiffer() {
        RegionImpl region = new RegionImpl(5000, 3, 4);

        assertThat(region.isSameRegion(3, 5)).isFalse(); // Different column
        assertThat(region.isSameRegion(2, 4)).isFalse(); // Different row
        assertThat(region.isSameRegion(2, 5)).isFalse(); // Both different
    }
}