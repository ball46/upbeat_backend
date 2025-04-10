package com.example.upbeat_backend.game.plans.parser;

import com.example.upbeat_backend.game.plans.tokenizer.TokenizerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class StatementTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "done",
        "relocate",
        "move up",
        "invest 100",
        "collect 50",
        "shoot up 100",
        "x = 42",
        "if (x) then done else done",
        "while (opponent) done",
        "{ done move up }"
    })
    void validStatementsShouldParse(String input) {
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testIfStatement() {
        String input = "if (opponent) then shoot up 100 else move down";
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testNestedIfStatements() {
        String input = "if (opponent) then if (nearby up) then shoot up 100 else move up else done";
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testWhileStatement() {
        String input = "while (nearby up) { move up }";
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testBlockStatement() {
        String input = "{ done move up shoot up 100 }";
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertDoesNotThrow(parser::parse);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "if opponent then done else done",   // ขาดวงเล็บ
        "if (opponent) done else done",      // ขาด then
        "if (opponent) then done",           // ขาด else
        "while opponent done",               // ขาดวงเล็บ
        "move",                              // ขาดทิศทาง
        "shoot up",                          // ขาดจำนวนพลังงาน
        "{ done",                            // วงเล็บปีกกาไม่ครบ
        "invest",                            // ขาดจำนวนเงินลงทุน
        "collect"                            // ขาดจำนวนเงินที่เก็บ
    })
    void invalidStatementsShouldFail(String input) {
        ParserImpl parser = new ParserImpl(new TokenizerImpl(input));
        assertThrows(Exception.class, parser::parse);
    }
}