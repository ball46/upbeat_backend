package com.example.upbeat_backend.game.dto.response.player;

import com.example.upbeat_backend.game.state.player.Player;

public record PlayerDTO(
        String id,
        String name,
        long budget,
        int cityCenterRow,
        int cityCenterCol
) {

    public static PlayerDTO from(Player player) {
        return new PlayerDTO(
                player.getId(),
                player.getName(),
                player.getBudget(),
                player.getCityCenterRow(),
                player.getCityCenterCol()
        );
    }
}
