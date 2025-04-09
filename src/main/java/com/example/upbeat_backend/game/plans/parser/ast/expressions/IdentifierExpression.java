package com.example.upbeat_backend.game.plans.parser.ast.expressions;

import com.example.upbeat_backend.game.exception.parser.ParserException;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class IdentifierExpression implements Expression {
    private final String variableName;

    @Override
    public long evaluateNumber(Environment env) {
        if (env.hasVariable(variableName)) {
            return env.getVariable(variableName);
        }
        throw new ParserException.UnknownIdentifier(variableName);
    }
}
