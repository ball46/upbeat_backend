package com.example.upbeat_backend.game.plans.parser.ast.expressions;

import com.example.upbeat_backend.game.model.enums.Special;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SpecialExpression implements Expression {
    private Special special;

    @Override
    public long evaluateNumber(Environment env) {
        return 0;
    }
}
