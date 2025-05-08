package com.example.upbeat_backend.game.dto.reids;

import com.example.upbeat_backend.game.model.enums.GameStatus;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

@Builder
@Getter
public class GameInfoDTO {
    private GameStatus gameStatus;
    private Timestamp createAt;
    private String winner;
    private long maxPlayers;
    private int currentTurn;
    private Timestamp lastUpdateAt;
}
