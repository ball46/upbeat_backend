package com.example.upbeat_backend.game.model.enums;

public enum GameStatus {
    WAITING_FOR_PLAYERS,
    IN_PROGRESS,
    FINISHED,
    ABORTED,
    ERROR;

    public static GameStatus fromString(String status) {
        try {
            return GameStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
