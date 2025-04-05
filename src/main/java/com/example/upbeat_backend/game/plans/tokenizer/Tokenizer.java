package com.example.upbeat_backend.game.plans.tokenizer;

import com.example.upbeat_backend.game.model.enums.Type;

public interface Tokenizer {

    boolean hasNextToken();

    Token peekToken();

    String peekValue();

    Type peekType();

    Token consumeToken();

    String consume();

    boolean consume(String value);

    boolean consumeType(Type type);

    int getPosition();
}
