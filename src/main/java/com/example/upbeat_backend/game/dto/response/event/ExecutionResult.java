package com.example.upbeat_backend.game.dto.response.event;

import lombok.Builder;

import java.util.List;

@Builder
public record ExecutionResult(
        String gameId,
        String playerId,
        List<GameEvent> events,
        List<Object> finalState
) {}
