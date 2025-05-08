package com.example.upbeat_backend.game.dto.reids;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameConfigDTO {
    private int rows;
    private int cols;
    private int initPlanMin;
    private int initPlanSec;
    private long initBudget;
    private long initCenterDep;
    private int planRevMin;
    private int planRevSec;
    private long revCost;
    private long maxDep;
    private int interestPct;
}
