package com.example.upbeat_backend.game.dto.response.event;

import com.example.upbeat_backend.game.model.Position;
import com.example.upbeat_backend.game.model.enums.Keyword;
import lombok.Builder;

public sealed interface EventData permits
        EventData.Done, EventData.Relocate,
        EventData.Move, EventData.Invest,
        EventData.Collect, EventData.Shoot,
        EventData.Opponent, EventData.Nearby,
        EventData.Rows, EventData.Cols,
        EventData.CurrentRow, EventData.CurrentCol, 
        EventData.Budget, EventData.Deposit,
        EventData.Interest, EventData.MaxDeposit, 
        EventData.Random 
{
    @Builder
    record Done(
            boolean success,
            Position position
    ) implements EventData {}

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
    record Rows(
            long row,
            Position position
    ) implements EventData {}
    
    @Builder
    record Cols(
            long col,
            Position position
    ) implements EventData {}
    
    @Builder
    record CurrentRow(
            long currentRow,
            Position position
    ) implements EventData {}
    
    @Builder
    record CurrentCol(
            long currentCol,
            Position position
    ) implements EventData {}
    
    @Builder
    record Budget(
            long budget,
            Position position
    ) implements EventData {}
    
    @Builder
    record Deposit(
            long deposit,
            Position position
    ) implements EventData {}
    
    @Builder
    record Interest(
            long interest,
            Position position
    ) implements EventData {}
    
    @Builder
    record MaxDeposit(
            long maxDeposit,
            Position position
    ) implements EventData {}
    
    @Builder
    record Random(
            long random,
            Position position
    ) implements EventData {}
}
