package com.example.upbeat_backend.game.service;

import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.response.game.GamePlayerResponseDTO;
import com.example.upbeat_backend.game.dto.response.game.GameResultNotificationDTO;
import com.example.upbeat_backend.game.dto.response.game.GameStartResponseDTO;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class GameNotificationService {
    private final SimpMessagingTemplate template;

    public void playerJoined(GamePlayerResponseDTO data) {
        template.convertAndSend("/topic/game/" + data.getGameId() + "/player-joined", data);
    }

    public void gameStarted(GameStartResponseDTO data) {
        template.convertAndSend("/topic/game/" + data.getGameId() + "/game-started", data);
    }

    public void gameFinished(String gameId, GameInfoDTO gameInfo, List<String> players) {
        boolean isDraw = "draw".equals(gameInfo.getWinner());

        GameResultNotificationDTO notification = GameResultNotificationDTO.builder()
                .gameStatus(gameInfo.getGameStatus())
                .gameId(gameId)
                .isDraw(isDraw)
                .winnerId(gameInfo.getWinner())
                .build();

        for (String playerId : players) {
            boolean isWinner = !isDraw && playerId.equals(gameInfo.getWinner());
            notification.setWinner(isWinner);
            template.convertAndSendToUser(playerId, "/queue/game.result", notification);
        }
    }
}
