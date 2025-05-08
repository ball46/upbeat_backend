package com.example.upbeat_backend.game.plans.parser.ast.expressions.info;

import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import com.example.upbeat_backend.game.runtime.GameEnvironment;

public class OpponentExpression implements Expression {
    @Override
    public long evaluateNumber(Environment env) {
        if (env instanceof GameEnvironment gameEnv) {
            return gameEnv.opponent();
        }
        return -1;
    }
}
