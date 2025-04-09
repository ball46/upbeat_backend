package com.example.upbeat_backend.game.plans.parser.ast.statements.commands;

import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.plans.parser.ast.Statement;
import com.example.upbeat_backend.game.runtime.Environment;

public record AssignmentStatement(String variableName, Expression expression) implements Statement {
    @Override
    public Object evaluate(Environment env) {
        long value = expression.evaluateNumber(env);
        env.setVariable(variableName, value);
        return value;
    }
}
