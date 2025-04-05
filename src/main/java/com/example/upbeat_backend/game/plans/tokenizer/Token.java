package com.example.upbeat_backend.game.plans.tokenizer;

import com.example.upbeat_backend.game.model.enums.Type;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Token(String value, Type type, int position) {
    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return type + ": '" + value + "' at " + position;
    }
}
