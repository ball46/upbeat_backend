package com.example.upbeat_backend.game.dto.reids;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TerritorySizeDTO {
    private int rows;
    private int cols;
}
