package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.dto.response.event.ExecutionResult;
import com.example.upbeat_backend.game.plans.parser.ast.Statement;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class PlanInterpreter {
    private final GameEnvironment env;

    public ExecutionResult execute(List<Statement> statements) {
        for (Statement stmt : statements) {
            stmt.evaluate(env);
        }

        return ExecutionResult.builder()
                .gameId(env.getGameId())
                .playerId(env.getPlayerId())
                .events(env.getEvents())
                .startState(env.getGameState().getTerritory())
                .finalState(env.getGameState().getTerritory())
                .build();
    }
}