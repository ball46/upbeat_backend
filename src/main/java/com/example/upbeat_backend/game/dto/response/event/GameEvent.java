package com.example.upbeat_backend.game.dto.response.event;

import com.example.upbeat_backend.game.model.enums.EventType;
import lombok.Builder;

@Builder
public record GameEvent(
        EventType eventType,
        EventData data,
        long timestamp
) {}
