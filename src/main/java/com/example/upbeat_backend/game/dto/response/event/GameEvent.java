package com.example.upbeat_backend.game.dto.response.event;

import lombok.Builder;

import java.util.Map;

@Builder
public record GameEvent(
        String eventType,
        Map<String, Object> data,
        long timestamp
) {}
