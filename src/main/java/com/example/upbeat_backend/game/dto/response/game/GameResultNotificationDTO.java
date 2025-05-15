package com.example.upbeat_backend.game.dto.response.game;

import com.example.upbeat_backend.game.model.enums.GameStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameResultNotificationDTO {
    private GameStatus gameStatus;
    private String gameId;
    private boolean isDraw;
    private String winnerId;
    private boolean isWinner;
}
