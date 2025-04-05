package com.example.upbeat_backend.game.plans.tokenizer;

import com.example.upbeat_backend.game.model.enums.Type;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenizerImplTest {
    @Test
    void testBasicTokenizing() {
        Tokenizer tokenizer = new TokenizerImpl("if x = 10 then move else {}");

        assertEquals("if", tokenizer.peekValue());
        assertEquals(Type.KEYWORD, tokenizer.peekType());
        tokenizer.consume();

        assertEquals("x", tokenizer.consume());
        assertEquals("=", tokenizer.consume());
        assertEquals("10", tokenizer.consume());

        assertTrue(tokenizer.consume("then"));
        assertTrue(tokenizer.consumeType(Type.KEYWORD));

        assertFalse(tokenizer.hasNextToken());
    }
}