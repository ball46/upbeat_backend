package com.example.upbeat_backend.game.plans.parser.ast.statements;

import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.plans.parser.ast.Statement;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class IfStatement implements Statement {
    private final Expression condition;
    private final Statement trueBranch;
    private final Statement falseBranch;

    @Override
    public Object evaluate(Environment env) {
        if (condition.evaluateNumber(env) > 0) {
            return trueBranch.evaluate(env);
        } else {
            return falseBranch.evaluate(env);
        }
    }
}
