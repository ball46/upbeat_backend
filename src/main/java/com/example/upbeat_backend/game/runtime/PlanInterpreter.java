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
                .gameId(((GameEnvironmentImpl)env).getGameId())
                .playerId(((GameEnvironmentImpl)env).getPlayerId())
                .events(((GameEnvironmentImpl)env).getEvents())
                .startState(((GameEnvironmentImpl)env).getGameState().getTerritory())
                .finalState(((GameEnvironmentImpl)env).getGameState().getTerritory())
                .build();
    }
}