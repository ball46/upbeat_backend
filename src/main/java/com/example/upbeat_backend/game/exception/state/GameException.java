package com.example.upbeat_backend.game.exception.state;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class GameException {
    public static class GameNotFound extends BaseException {
        public GameNotFound(String gameId) {
            super("Game with ID '" + gameId + "' not found",
                    HttpStatus.NOT_FOUND,
                    "GAME_NOT_FOUND");
        }
    }

    public static class PlayerNotFound extends BaseException {
        public PlayerNotFound(String playerId) {
            super("Player with ID '" + playerId + "' not found",
                    HttpStatus.NOT_FOUND,
                    "PLAYER_NOT_FOUND");
        }
    }

    public static class GameAlreadyStarted extends BaseException {
        public GameAlreadyStarted(String gameId) {
            super("Game with ID '" + gameId + "' has already started",
                    HttpStatus.BAD_REQUEST,
                    "GAME_ALREADY_STARTED");
        }
    }

    public static class GameIsFull extends BaseException {
        public GameIsFull(String gameId) {
            super("Game with ID '" + gameId + "' is full",
                    HttpStatus.BAD_REQUEST,
                    "GAME_IS_FULL");
        }
    }

    public static class NotEnoughBudget extends BaseException {
        public NotEnoughBudget(String playerId) {
            super("Player with ID '" + playerId + "' does not have enough budget",
                    HttpStatus.BAD_REQUEST,
                    "NOT_ENOUGH_BUDGET");
        }
    }

    public static class InvalidGameState extends BaseException {
        public InvalidGameState(String gameId, String currentState, String expectedState) {
            super("Game with ID '" + gameId + "' is in '" + currentState + "' state but expected '" + expectedState + "'",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GAME_STATE");
        }
    }

    public static class PlanNotFound extends BaseException {
        public PlanNotFound(String playerId) {
            super("Player with ID '" + playerId + "' does not have a plan",
                    HttpStatus.BAD_REQUEST,
                    "PLAN_NOT_FOUND");
        }
    }
}
