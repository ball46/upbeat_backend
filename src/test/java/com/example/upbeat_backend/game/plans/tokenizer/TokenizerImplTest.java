package com.example.upbeat_backend.game.plans.tokenizer;

import com.example.upbeat_backend.game.exception.tokenizer.TokenizerException;
import com.example.upbeat_backend.game.model.enums.Type;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenizerImplTest {
    @Test
    void testBasicTokenizing() {
        Tokenizer tokenizer = new TokenizerImpl("if x = 10 then move else { }");

        Token ifToken = tokenizer.peekToken();
        assertEquals("if", ifToken.value());
        assertEquals(Type.KEYWORD, ifToken.type());
        assertEquals(0, ifToken.position());

        assertEquals("if", tokenizer.peekValue());
        assertEquals(Type.KEYWORD, tokenizer.peekType());
        assertEquals(0, tokenizer.getPosition());

        Token consumedToken = tokenizer.consumeToken();
        assertEquals(ifToken, consumedToken);
        assertEquals(ifToken.value(), consumedToken.value());

        assertEquals("x", tokenizer.consume());

        assertEquals("=", tokenizer.peekValue());
        tokenizer.consume();

        assertEquals("10", tokenizer.consumeToken().value());

        assertTrue(tokenizer.consume("then"));

        assertTrue(tokenizer.consumeType(Type.KEYWORD));

        assertEquals("else", tokenizer.consume());
        assertEquals("{", tokenizer.consume());
        assertEquals("}", tokenizer.consume());

        assertFalse(tokenizer.hasNextToken());
    }

    @Test
    void testKeywords() {
        Tokenizer tokenizer = new TokenizerImpl("if while then else");

        assertEquals("if", tokenizer.consume());
        assertEquals("while", tokenizer.consume());
        assertEquals("then", tokenizer.consume());
        assertEquals("else", tokenizer.consume());
        assertFalse(tokenizer.hasNextToken());
    }

    @Test
    void testOperators() {
        Tokenizer tokenizer = new TokenizerImpl("+ - * / ^ = % ( ) { }");

        assertEquals("+", tokenizer.consume());
        assertEquals("-", tokenizer.consume());
        assertEquals("*", tokenizer.consume());
        assertEquals("/", tokenizer.consume());
        assertEquals("^", tokenizer.consume());
        assertEquals("=", tokenizer.consume());
        assertEquals("%", tokenizer.consume());
        assertEquals("(", tokenizer.consume());
        assertEquals(")", tokenizer.consume());
        assertEquals("{", tokenizer.consume());
        assertEquals("}", tokenizer.consume());
        assertFalse(tokenizer.hasNextToken());
    }

    @Test
    void testNumbers() {
        Tokenizer tokenizer = new TokenizerImpl("123 456 789");

        assertEquals("123", tokenizer.consume());
        assertEquals("456", tokenizer.consume());
        assertEquals("789", tokenizer.consume());
        assertFalse(tokenizer.hasNextToken());
    }

    @Test
    void testIdentifiers() {
        Tokenizer tokenizer = new TokenizerImpl("x y _var var1 abc_123");

        assertEquals("x", tokenizer.consume());
        assertEquals("y", tokenizer.consume());
        assertEquals("_var", tokenizer.consume());
        assertEquals("var1", tokenizer.consume());
        assertEquals("abc_123", tokenizer.consume());
        assertFalse(tokenizer.hasNextToken());
    }

    @Test
    void testComments() {
        Tokenizer tokenizer = new TokenizerImpl("x # This is a comment\ny");

        assertEquals("x", tokenizer.consume());
        assertEquals("y", tokenizer.consume());
        assertFalse(tokenizer.hasNextToken());
    }

    @Test
    void testInvalidToken() {
        assertThrows(TokenizerException.UnknownWord.class, () -> {
            Tokenizer tokenizer = new TokenizerImpl("@invalid");
            tokenizer.consume();
        });
    }

    @Test
    void testNoMoreTokens() {
        assertThrows(TokenizerException.NextNull.class, () -> {
            Tokenizer tokenizer = new TokenizerImpl("x");
            tokenizer.consume();
            tokenizer.consume();
        });
    }

    @Test
    void testComplexExpression() {
        Tokenizer tokenizer = new TokenizerImpl("if (x + 5) * 3 = 15 then { move } else { shoot }");

        assertEquals("if", tokenizer.consume());
        assertEquals("(", tokenizer.consume());
        assertEquals("x", tokenizer.consume());
        assertEquals("+", tokenizer.consume());
        assertEquals("5", tokenizer.consume());
        assertEquals(")", tokenizer.consume());
        assertEquals("*", tokenizer.consume());
        assertEquals("3", tokenizer.consume());
        assertEquals("=", tokenizer.consume());
        assertEquals("15", tokenizer.consume());
        assertEquals("then", tokenizer.consume());
        assertEquals("{", tokenizer.consume());
        assertEquals("move", tokenizer.consume());
        assertEquals("}", tokenizer.consume());
        assertEquals("else", tokenizer.consume());
        assertEquals("{", tokenizer.consume());
        assertEquals("shoot", tokenizer.consume());
        assertEquals("}", tokenizer.consume());
    }

    @Test
    void testKeywordLikeIdentifiers() {
        Tokenizer tokenizer = new TokenizerImpl("ifx whiley thenot");

        Token token1 = tokenizer.consumeToken();
        assertEquals("ifx", token1.value());
        assertEquals(Type.IDENTIFIER, token1.type());

        Token token2 = tokenizer.consumeToken();
        assertEquals("whiley", token2.value());
        assertEquals(Type.IDENTIFIER, token2.type());

        Token token3 = tokenizer.consumeToken();
        assertEquals("thenot", token3.value());
        assertEquals(Type.IDENTIFIER, token3.type());
    }

    @Test
    void testMultilineInput() {
        Tokenizer tokenizer = new TokenizerImpl("""
                if x = 10
                    # comment in the middle
                    then move
                else
                    shoot
                """);

        assertEquals("if", tokenizer.consume());
        assertEquals("x", tokenizer.consume());
        assertEquals("=", tokenizer.consume());
        assertEquals("10", tokenizer.consume());
        assertEquals("then", tokenizer.consume());
        assertEquals("move", tokenizer.consume());
        assertEquals("else", tokenizer.consume());
        assertEquals("shoot", tokenizer.consume());
        assertFalse(tokenizer.hasNextToken());
    }

    @Test
    void testPositionTracking() {
        Tokenizer tokenizer = new TokenizerImpl("x = 10");

        Token token1 = tokenizer.consumeToken();
        assertEquals(0, token1.position());

        Token token2 = tokenizer.consumeToken();
        assertEquals(2, token2.position());

        Token token3 = tokenizer.consumeToken();
        assertEquals(4, token3.position());
    }

    @Test
    void testAdjacentOperators() {
        Tokenizer tokenizer = new TokenizerImpl("(1+2)*3");

        assertEquals("(", tokenizer.consume());
        assertEquals("1", tokenizer.consume());
        assertEquals("+", tokenizer.consume());
        assertEquals("2", tokenizer.consume());
        assertEquals(")", tokenizer.consume());
        assertEquals("*", tokenizer.consume());
        assertEquals("3", tokenizer.consume());
    }

    @Test
    void testGameSpecificSyntax() {
        Tokenizer tokenizer = new TokenizerImpl("if nearby(up) = 0 then move(up) else shoot(down)");

        assertEquals("if", tokenizer.consume());
        assertEquals("nearby", tokenizer.consume());
        assertEquals("(", tokenizer.consume());
        assertEquals("up", tokenizer.consume());
        assertEquals(")", tokenizer.consume());
        assertEquals("=", tokenizer.consume());
        assertEquals("0", tokenizer.consume());
        assertEquals("then", tokenizer.consume());
        assertEquals("move", tokenizer.consume());
        assertEquals("(", tokenizer.consume());
        assertEquals("up", tokenizer.consume());
        assertEquals(")", tokenizer.consume());
        assertEquals("else", tokenizer.consume());
        assertEquals("shoot", tokenizer.consume());
        assertEquals("(", tokenizer.consume());
        assertEquals("down", tokenizer.consume());
        assertEquals(")", tokenizer.consume());
    }
}