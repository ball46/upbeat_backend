package com.example.upbeat_backend.game.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Special {
    ROWS("rows"),
    COLS("cols"),
    CURROW("currow"),
    CURCOL("curcol"),
    BUDGET("budget"),
    DEPOSIT("deposit"),
    INT("int"),
    MAXDEPOSIT("maxdeposit"),
    RANDOM("random");

    private final String lexeme;

    public static Special fromString(String lexeme) {
        for (Special special : values()) {
            if (special.lexeme.equals(lexeme)) {
                return special;
            }
        }
        return null;
    }

    public static boolean isSpecial(String lexeme) {
        return fromString(lexeme) != null;
    }
}
