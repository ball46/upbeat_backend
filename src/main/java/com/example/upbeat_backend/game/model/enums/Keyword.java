package com.example.upbeat_backend.game.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum Keyword {
    // Control flow
    IF("if"),
    ELSE("else"),
    THEN("then"),
    WHILE("while"),

    // Actions
    COLLECT("collect"),
    INVEST("invest"),
    MOVE("move"),
    RELOCATE("relocate"),
    SHOOT("shoot"),
    DONE("done"),

    // Directions
    UP("up"),
    UPLEFT("upleft"),
    UPRIGHT("upright"),
    DOWN("down"),
    DOWNLEFT("downleft"),
    DOWNRIGHT("downright"),

    // Others
    NEARBY("nearby"),
    OPPONENT("opponent");

    private final String lexeme;

    public static Keyword fromString(String lexeme) {
        for (Keyword keyword : values()) {
            if (keyword.lexeme.equals(lexeme)) {
                return keyword;
            }
        }
        return null;
    }

    public static boolean isKeyword(String lexeme) {
        return fromString(lexeme) != null;
    }

    public boolean isDirection() {
        return this == UP || this == DOWN || this == UPLEFT ||
                this == UPRIGHT || this == DOWNLEFT || this == DOWNRIGHT;
    }

    public static List<Keyword> directions() {
        return List.of(UP, DOWN, UPLEFT, UPRIGHT, DOWNLEFT, DOWNRIGHT);
    }

    public boolean isAction() {
        return this == COLLECT || this == INVEST || this == MOVE ||
                this == RELOCATE || this == SHOOT || this == DONE;
    }

    public boolean isControlFlow() {
        return this == IF || this == ELSE || this == THEN || this == WHILE;
    }

    public boolean isInfo() {
        return this == NEARBY || this == OPPONENT;
    }
}