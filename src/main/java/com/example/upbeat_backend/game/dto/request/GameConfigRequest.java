package com.example.upbeat_backend.game.dto.request;

import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameConfigRequest {
    @NotNull(message = "Game configuration is required")
    @Valid
    GameConfigDTO gameConfig;

    @Min(value = 2, message = "At least 2 players required")
    int maxPlayers;
}
