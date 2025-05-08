package com.example.upbeat_backend.game.plans.parser.ast.expressions.info;

import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import com.example.upbeat_backend.game.runtime.GameEnvironment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NearbyExpression implements Expression {
    private final Keyword keyword;

    @Override
    public long evaluateNumber(Environment env) {
        if (env instanceof GameEnvironment gameEnv) {
            return gameEnv.nearby(keyword);
        }
        return -1;
    }
}
