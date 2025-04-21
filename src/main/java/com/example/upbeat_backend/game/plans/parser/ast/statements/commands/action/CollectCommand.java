package com.example.upbeat_backend.game.plans.parser.ast.statements.commands.action;

import com.example.upbeat_backend.game.plans.parser.ast.Command;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import com.example.upbeat_backend.game.runtime.GameEnvironment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CollectCommand implements Command {
    private final Expression expression;
    @Override
    public Object evaluate(Environment env) {
        if (env instanceof GameEnvironment gameEnv) {
            long amount = (long) expression.evaluate(env);
            gameEnv.collect(amount);
            return null;
        }
        return null;
    }
}
