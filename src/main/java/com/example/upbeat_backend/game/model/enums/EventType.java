package com.example.upbeat_backend.game.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventType {
    DONE("done"),
    RELOCATE("relocate"),
    MOVE("move"),
    INVEST("invest"),
    COLLECT("collect"),
    SHOOT("shoot"),
    OPPONENT("opponent"),
    NEARBY("nearby"),

    ROWS("rows"),
    COLS("cols"),
    CURRENT_ROW("current_row"),
    CURRENT_COL("current_col"),
    BUDGET("budget"),
    DEPOSIT("deposit"),
    INTEREST("interest"),
    MAX_DEPOSIT("max_deposit"),
    RANDOM("random");

    private final String lexeme;


}
