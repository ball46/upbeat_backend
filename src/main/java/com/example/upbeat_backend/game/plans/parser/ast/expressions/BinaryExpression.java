package com.example.upbeat_backend.game.plans.parser.ast.expressions;

import com.example.upbeat_backend.game.exception.parser.ParserException;
import com.example.upbeat_backend.game.model.enums.Operator;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BinaryExpression implements Expression {
    private final Expression left;
    private final Expression right;
    private final Operator operator;

    @Override
    public long evaluateNumber(Environment env) {
        long leftValue = left.evaluateNumber(env);
        long rightValue = right.evaluateNumber(env);

        if ((operator == Operator.DIVIDE || operator == Operator.MOD) && rightValue == 0) {
            throw new ParserException.DivisionByZero();
        }

        return switch (operator) {
            case PLUS -> leftValue + rightValue;
            case MINUS -> leftValue - rightValue;
            case MULTIPLY -> leftValue * rightValue;
            case DIVIDE -> leftValue / rightValue;
            case MOD -> leftValue % rightValue;
            case CARET -> (long) Math.pow(leftValue, rightValue);
            default -> throw new ParserException.CannotUseOperator(operator.getSymbol());
        };
    }
}
