package com.example.upbeat_backend.game.plans.parser.ast.statements.commands.action;

import com.example.upbeat_backend.game.plans.parser.ast.Command;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InvestCommand implements Command {
    private final Expression expression;

    @Override
    public Object evaluate(Environment env) {
        return null;
    }
}
