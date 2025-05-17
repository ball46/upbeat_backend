package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.game.dto.response.event.ExecutionResult;
import com.example.upbeat_backend.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameSocketController {
    private final GameService gameService;

    @MessageMapping("/initial/{gameId}/{playerId}")
    @SendToUser("/queue/game.update")
    public ExecutionResult initialPlan(
            @DestinationVariable String gameId,
            @DestinationVariable String playerId,
            @Payload String plan
    ) {
        return gameService.initialPlayerPlan(gameId, playerId, plan);
    }

    @MessageMapping("/execute/{gameId}/{playerId}")
    @SendToUser("/queue/game.update")
    public ExecutionResult executeNewPlan(
            @DestinationVariable String gameId,
            @DestinationVariable String playerId,
            @Payload String plan
    ) {
        return gameService.executeNewPlan(gameId, playerId, plan);
    }

    @MessageMapping("/execute/current/{gameId}/{playerId}")
    @SendToUser("/queue/game.update")
    public ExecutionResult executeCurrentPlan(
            @DestinationVariable String gameId,
            @DestinationVariable String playerId
    ) {
        return gameService.executeCurrentPlan(gameId, playerId);
    }
}
