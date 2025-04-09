package com.example.upbeat_backend.game.plans.parser.ast.statements.commands.action;

import com.example.upbeat_backend.game.plans.parser.ast.Command;
import com.example.upbeat_backend.game.runtime.Environment;

public class DoneCommand implements Command {
    @Override
    public Object evaluate(Environment env) {
        return null;
    }
}
