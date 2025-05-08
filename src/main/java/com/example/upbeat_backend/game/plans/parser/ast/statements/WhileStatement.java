package com.example.upbeat_backend.game.plans.parser.ast.statements;

import com.example.upbeat_backend.game.exception.parser.ParserException;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.plans.parser.ast.Statement;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class WhileStatement implements Statement {
    private final Expression condition;
    private final Statement body;

    @Override
    public Object evaluate(Environment env) {
        Object result = null;
        int iterationCount = 0;

        while (condition.evaluateNumber(env) > 0 && iterationCount < 1000) {
            iterationCount++;
            result = body.evaluate(env);
        }

        if (iterationCount >= 1000) {
            throw new ParserException.InfiniteLoop();
        }

        return result;
    }
}
