package com.example.upbeat_backend.game.plans.parser.ast.expressions;

import com.example.upbeat_backend.game.model.enums.Special;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.runtime.Environment;
import com.example.upbeat_backend.game.runtime.GameEnvironment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SpecialExpression implements Expression {
    private Special special;

    @Override
    public long evaluateNumber(Environment env) {
        if (env instanceof GameEnvironment gameEnv) {
            return switch (special) {
                case ROWS -> gameEnv.getRows();
                case COLS -> gameEnv.getCols();
                case CURROW -> gameEnv.getCurrentRow();
                case CURCOL -> gameEnv.getCurrentCol();
                case BUDGET -> gameEnv.getBudget();
                case DEPOSIT -> gameEnv.getDeposit();
                case INT -> gameEnv.getInterest();
                case MAXDEPOSIT -> gameEnv.getMaxDeposit();
                case RANDOM -> gameEnv.getRandom();
            };
        }
        return -1;
    }
}
