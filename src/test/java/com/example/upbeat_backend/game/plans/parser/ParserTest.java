package com.example.upbeat_backend.game.plans.parser;

import com.example.upbeat_backend.game.plans.tokenizer.TokenizerImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    @Test
    void emptyPlanShouldFail() {
        ParserImpl parser = new ParserImpl(new TokenizerImpl(""));
        assertThrows(Exception.class, parser::parse);
    }

    @Test
    void testComplexPlan() {
        String plan = """
            x = 100
            y = 200
            while (x) {
                if (opponent) then {
                    shoot up x
                    x = x - 10
                } else {
                    if (nearby up) then {
                        move down
                    } else {
                        move up
                        invest y
                    }
                }
            }
            collect 100
            done
            """;

        ParserImpl parser = new ParserImpl(new TokenizerImpl(plan));
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testWhitespaceAndComments() {
        String plan = """
            # This is a comment
            x = 10  # This is a comment too
            
            # Empty line above
            y = 20
            # Final comment
            """;

        ParserImpl parser = new ParserImpl(new TokenizerImpl(plan));
        assertDoesNotThrow(parser::parse);
    }

    @Test
    void testSyntaxError() {
        String plan = """
            x = 10
            if opponent then move up else done
            """;

        ParserImpl parser = new ParserImpl(new TokenizerImpl(plan));
        assertThrows(Exception.class, parser::parse);
    }

    @Test
    void testMultiplePlans() {
        String[] plans = {
            "move up",
            "done",
            "shoot up 100",
            "while (opponent) { move up }"
        };

        for (String plan : plans) {
            ParserImpl parser = new ParserImpl(new TokenizerImpl(plan));
            assertDoesNotThrow(parser::parse);
        }
    }
}