package com.example.upbeat_backend.game.plans.parser.ast;

import com.example.upbeat_backend.game.runtime.Environment;

public interface Expression extends Node {
    long evaluateNumber(Environment env);

    @Override
    default Object evaluate(Environment env) {
        return evaluateNumber(env);
    }
}
