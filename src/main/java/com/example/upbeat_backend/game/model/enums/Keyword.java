package com.example.upbeat_backend.game.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

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

    public static @Nullable Keyword fromString(String lexeme) {
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
}