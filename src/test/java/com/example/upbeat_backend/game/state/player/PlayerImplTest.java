package com.example.upbeat_backend.game.state.player;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerImplTest {

    @Test
    void constructor_shouldInitializeFieldsCorrectly() {
        PlayerImpl player = new PlayerImpl("p1", "Player 1", 1000, 5, 7);

        assertThat(player.getId()).isEqualTo("p1");
        assertThat(player.getName()).isEqualTo("Player 1");
        assertThat(player.getBudget()).isEqualTo(1000);
        assertThat(player.getCityCenterRow()).isEqualTo(5);
        assertThat(player.getCityCenterCol()).isEqualTo(7);
    }

    @Test
    void updateBudget_shouldAddAmount_whenPositiveValue() {
        PlayerImpl player = new PlayerImpl("p1", "Player 1", 1000, 5, 7);

        player.updateBudget(500);

        assertThat(player.getBudget()).isEqualTo(1500);
    }

    @Test
    void updateBudget_shouldSubtractAmount_whenNegativeValue() {
        PlayerImpl player = new PlayerImpl("p1", "Player 1", 1000, 5, 7);

        player.updateBudget(-300);

        assertThat(player.getBudget()).isEqualTo(700);
    }

    @Test
    void updateBudget_shouldSetToZero_whenResultWouldBeNegative() {
        PlayerImpl player = new PlayerImpl("p1", "Player 1", 500, 5, 7);

        player.updateBudget(-800);

        assertThat(player.getBudget()).isEqualTo(0);
    }

    @Test
    void updateCityCenter_shouldUpdateCityCenterCoordinates() {
        PlayerImpl player = new PlayerImpl("p1", "Player 1", 1000, 5, 7);

        player.updateCityCenter(9, 3);

        assertThat(player.getCityCenterCol()).isEqualTo(9);
        assertThat(player.getCityCenterRow()).isEqualTo(3);
    }
}