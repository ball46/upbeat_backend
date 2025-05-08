package com.example.upbeat_backend.game.plans.parser.ast.plan;

import com.example.upbeat_backend.game.plans.parser.ast.Node;
import com.example.upbeat_backend.game.plans.parser.ast.Statement;
import com.example.upbeat_backend.game.runtime.Environment;

import java.util.List;

public record PlanNode(List<Statement> statements) implements Node {
    @Override
    public Object evaluate(Environment env) {
        Object lastResult = null;
        for (Statement statement : statements) {
            lastResult = statement.evaluate(env);
        }
        return lastResult;
    }
}
