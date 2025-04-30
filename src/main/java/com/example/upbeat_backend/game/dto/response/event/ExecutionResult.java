package com.example.upbeat_backend.game.dto.response.event;

import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.state.region.Region;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ExecutionResult(
        String gameId,
        String playerId,
        String nextPlayerId,
        GameStatus gameStatus,
        List<GameEvent> events,
        Map<String, Region> startState,
        Map<String, Region> finalState
) {}
