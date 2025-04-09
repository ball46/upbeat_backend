package com.example.upbeat_backend.game.plans.parser.ast.statements;

import com.example.upbeat_backend.game.plans.parser.ast.Statement;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class BlockStatement implements Statement {
    private final List<Statement> statements;

    @Override
    public Object evaluate(Environment env) {
        Object result = null;
        for (Statement statement : statements) {
            result = statement.evaluate(env);
        }
        return result;
    }
}
