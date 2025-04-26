package com.example.upbeat_backend.game.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventType {
    RELOCATE("relocate"),
    MOVE("move"),
    INVEST("invest"),
    COLLECT("collect"),
    SHOOT("shoot"),
    DONE("done"),
    OPPONENT("opponent"),
    NEARBY("nearby");

    private final String lexeme;


}
