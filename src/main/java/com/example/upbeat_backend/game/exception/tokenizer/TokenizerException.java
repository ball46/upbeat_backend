package com.example.upbeat_backend.game.exception.tokenizer;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class TokenizerException {
    public static class NextNull extends BaseException {
        public NextNull() {
            super("No more tokens", HttpStatus.BAD_REQUEST, "NO_MORE_TOKENS");
        }
    }

    public static class UnknownWord extends BaseException {
        public UnknownWord(String token, int position) {
            super("Invalid token '" + token + "' at position " + position,
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TOKEN");
        }
    }
}
