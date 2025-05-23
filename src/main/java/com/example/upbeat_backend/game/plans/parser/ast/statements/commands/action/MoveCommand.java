package com.example.upbeat_backend.game.plans.parser.ast.statements.commands.action;

import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.plans.parser.ast.Command;
import com.example.upbeat_backend.game.runtime.Environment;
import com.example.upbeat_backend.game.runtime.GameEnvironment;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MoveCommand implements Command {
    private Keyword direction;

    @Override
    public Object evaluate(Environment env) {
        if (env instanceof GameEnvironment gameEnv) {
            return gameEnv.move(direction);
        }
        return null;
    }
}
