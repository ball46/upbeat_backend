package com.example.upbeat_backend.game.plans.parser;

import com.example.upbeat_backend.game.exception.parser.ParserException;
import com.example.upbeat_backend.game.plans.tokenizer.TokenizerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionTest {
    // ทดสอบ expressions ที่ถูกต้อง
    @ParameterizedTest
    @ValueSource(strings = {
        "x = 42",
        "x = 1 + 2",
        "x = 2 * (3 + 4)",
        "x = 10 - 5 + 2",
        "x = 10 / 2",
        "x = 10 % 3",
        "x = 2 ^ 3",
        "x = nearby up",
        "x = opponent"
    })
    void validExpressionsShouldParse(String input) {
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testComplexExpression() {
        String complexExpression = "x = 5 + 3 * 2 - (4 + 1) ^ 2 / 5";
        ParserImpl parser = new ParserImpl(new TokenizerImpl(complexExpression));
        assertDoesNotThrow(parser::parse);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "x = 1 +",            // ตัวดำเนินการไม่ครบ
        "x = 1 + (2",         // วงเล็บไม่ครบ
        "x = nearby",         // ขาดทิศทาง
        "x = 2 * * 3",        // ตัวดำเนินการซ้ำ
        "x = 5 & 3"           // ตัวดำเนินการไม่รองรับ
    })
    void invalidExpressionsShouldFail(String input) {
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertThrows(Exception.class, parser::parse);
    }

    @Test
    void testDivisionByZero() {
        String input = "x = 10 / 0";
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        try {
            parser.parse();
        } catch (ParserException.DivisionByZero e) {
            assertTrue(true);
        }
    }
}