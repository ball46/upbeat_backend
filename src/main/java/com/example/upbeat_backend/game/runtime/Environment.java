package com.example.upbeat_backend.game.runtime;

public interface Environment {
    String getGameId();

    void setVariable(String name, long value);

    long getVariable(String name);

    boolean hasVariable(String name);

    void reset();
}