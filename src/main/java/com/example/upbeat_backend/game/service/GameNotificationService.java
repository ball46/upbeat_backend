package com.example.upbeat_backend.game.service;

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

    public void gameFinished(GameResultNotificationDTO data, List<String> players) {
        template.convertAndSend("/topic/game/" + data.getGameId() + "/game-finished", data);

        for (String playerId : players) {
            boolean isWinner = !data.isDraw() && playerId.equals(data.getWinnerId());

            GameResultNotificationDTO playerNotification = GameResultNotificationDTO.builder()
                    .gameStatus(data.getGameStatus())
                    .gameId(data.getGameId())
                    .isDraw(data.isDraw())
                    .winnerId(data.getWinnerId())
                    .isWinner(isWinner)
                    .build();

            template.convertAndSendToUser(playerId, "/queue/game.result", playerNotification);
        }
    }
}
