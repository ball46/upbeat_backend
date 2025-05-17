package com.example.upbeat_backend.game.dto.response.game;

import lombok.Builder;
import lombok.Data;
import com.example.upbeat_backend.game.model.enums.GameStatus;

import java.util.List;

@Data
@Builder
public class GameStartResponseDTO {
    private String gameId;
    private List<String> players;
    private GameStatus status;
}
