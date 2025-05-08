package com.example.upbeat_backend.game.dto.reids;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CurrentStateDTO {
    String currentPlayerId;
    int currentRow;
    int currentCol;
}
