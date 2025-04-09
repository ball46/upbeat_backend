package com.example.upbeat_backend.game.plans.parser.ast;

import com.example.upbeat_backend.game.runtime.Environment;

public interface Node {
    Object evaluate(Environment env);
}
