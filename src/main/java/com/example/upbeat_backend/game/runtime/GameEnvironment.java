package com.example.upbeat_backend.game.runtime;

import com.example.upbeat_backend.game.model.enums.Keyword;

public interface GameEnvironment extends Environment {
    boolean done();

    boolean relocate();

    boolean move(Keyword direction);

    boolean invest(long amount);

    boolean collect(long amount);

    boolean shoot(Keyword direction, long damage);

    long opponent();

    long nearby(Keyword direction);

    long getRows();

    long getCols();

    long getCurrentRow();

    long getCurrentCol();

    long getBudget();

    long getDeposit();

    long getInterest();

    long getMaxDeposit();

    long getRandom();
}
