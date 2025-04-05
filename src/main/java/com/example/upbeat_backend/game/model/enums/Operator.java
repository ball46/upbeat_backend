package com.example.upbeat_backend.game.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public enum Operator {
    PLUS("+"), MINUS("-"), MULTIPLY("*"), DIVIDE("/"),
    LEFT_PAREN("("), RIGHT_PAREN(")"), LEFT_BRACE("{"), RIGHT_BRACE("}"),
    CARET("^"), EQUALS("="), PERCENT("%");

    private final String symbol;

    public static @Nullable Operator fromString(String symbol) {
        for (Operator op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        return null;
    }
}
