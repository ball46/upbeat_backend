package com.example.upbeat_backend.game.dto.response.event;

import com.example.upbeat_backend.game.model.Position;
import com.example.upbeat_backend.game.model.enums.Keyword;
import lombok.Builder;

public sealed interface EventData permits
        EventData.Relocate, EventData.Move,
        EventData.Invest, EventData.Collect,
        EventData.Shoot, EventData.Opponent,
        EventData.Nearby, EventData.Done
{
    @Builder
    record Relocate(
            long cost,
            boolean success,
            Position position
    ) implements EventData {}

    @Builder
    record Move(
            Keyword direction,
            boolean success,
            Position position
    ) implements EventData {}

    @Builder
    record Invest(
            long amount,
            boolean success,
            Position position
    ) implements EventData {}

    @Builder
    record Collect(
            long amount,
            boolean success,
            Position position
    ) implements EventData {}

    @Builder
    record Shoot(
            Keyword direction,
            long money,
            boolean success,
            Position position
    ) implements EventData {}

    @Builder
    record Opponent(
            long result,
            Integer distance,
            boolean success,
            Position position
    ) implements EventData {}

    @Builder
    record Nearby(
            Keyword direction,
            long result,
            Integer distance,
            boolean success,
            Position position
    ) implements EventData {}

    @Builder
    record Done(
            boolean success,
            Position position
    ) implements EventData {}
}
