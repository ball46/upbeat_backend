package com.example.upbeat_backend.game.plans.parser.ast.expressions.info;

import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;

public class OpponentExpression implements Expression {
    @Override
    public long evaluateNumber(Environment env) {
        return 0;
    }
}
