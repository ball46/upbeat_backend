package com.example.upbeat_backend.game.plans.parser.ast.expressions;

import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NumberExpression implements Expression {
    private final long value;

    @Override
    public long evaluateNumber(Environment env) {
        return value;
    }
}
