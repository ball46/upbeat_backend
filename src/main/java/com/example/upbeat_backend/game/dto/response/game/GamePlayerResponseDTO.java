package com.example.upbeat_backend.game.dto.response.game;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GamePlayerResponseDTO {
    private String gameId;
    private String playerId;
    private List<String> players;
    private long maxPlayers;
}
