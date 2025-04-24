package com.example.upbeat_backend.game.dto.reids;

import com.example.upbeat_backend.game.model.enums.GameStatus;
import lombok.Builder;

import java.sql.Timestamp;

@Builder
public class GameInfoDTO {
    private GameStatus gameStatus;
    private Timestamp createAt;
    private long maxPlayers;
    private int currentTurn;
    private Timestamp lastUpdateAt;
}
