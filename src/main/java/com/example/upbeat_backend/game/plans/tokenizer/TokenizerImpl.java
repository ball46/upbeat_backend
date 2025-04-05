package com.example.upbeat_backend.game.plans.tokenizer;

import com.example.upbeat_backend.game.exception.tokenizer.TokenizerException;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.model.enums.Operator;
import com.example.upbeat_backend.game.model.enums.Type;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
public class TokenizerImpl implements Tokenizer {
    private final String src;
    private int pos;
    private Token currentToken;
    private static final List<String> OPERATORS = new ArrayList<>();

    static {
        for (Operator op : Operator.values()) {
            OPERATORS.add(op.getSymbol());
        }
    }

    public TokenizerImpl(@NotNull String src) {
        this.src = src;
        this.pos = 0;
        computeNext();
    }

    @Override
    public boolean hasNextToken() {
        return currentToken.type() != Type.EOF;
    }

    @Override
    public Token peekToken() {
        if (!hasNextToken()) {
            throw new TokenizerException.NextNull();
        }
        return currentToken;
    }

    @Override
    public String peekValue() {
        if (!hasNextToken()) {
            throw new TokenizerException.NextNull();
        }
        return currentToken.value();
    }

    @Override
    public Type peekType() {
        if (!hasNextToken()) {
            throw new TokenizerException.NextNull();
        }
        return currentToken.type();
    }

    @Override
    public Token consumeToken() {
        if (!hasNextToken()) {
            throw new TokenizerException.NextNull();
        }
        Token result = currentToken;
        computeNext();
        return result;
    }

    @Override
    public String consume() {
        if (!hasNextToken()) {
            throw new TokenizerException.NextNull();
        }
        String result = currentToken.value();
        computeNext();
        return result;
    }

    @Override
    public boolean consume(String value) {
        if (!hasNextToken()) {
            throw new TokenizerException.NextNull();
        }
        if (currentToken.value().equals(value)) {
            computeNext();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean consumeType(Type type) {
        if (!hasNextToken()) {
            throw new TokenizerException.NextNull();
        }
        if (currentToken.type() == type) {
            computeNext();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getPosition() {
        return currentToken.position();
    }

    private void computeNext() {
        skipWhitespaceAndComments();

        if (pos >= src.length()) {
            currentToken = new Token("", Type.EOF, pos);
            return;
        }

        if (Character.isDigit(src.charAt(pos))) {
            int start = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
            currentToken = new Token(src.substring(start, pos), Type.NUMBER, start);
            return;
        }

        if (Character.isLetter(src.charAt(pos)) || src.charAt(pos) == '_') {
            int start = pos;
            while (pos < src.length() &&
                    (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                pos++;
            }

            String identifier = src.substring(start, pos);
            Type type = Keyword.isKeyword(identifier) ? Type.KEYWORD : Type.IDENTIFIER;
            currentToken = new Token(identifier, type, start);
            return;
        }

        for (String op : OPERATORS) {
            if (pos + op.length() <= src.length() &&
                    src.startsWith(op, pos)) {
                currentToken = new Token(op, Type.OPERATOR, pos);
                pos += op.length();
                return;
            }
        }

        throw new TokenizerException.UnknownWord(Character.toString(src.charAt(pos)), pos);
    }

    private void skipWhitespaceAndComments() {
        while (pos < src.length()) {
            if (Character.isWhitespace(src.charAt(pos))) {
                pos++;
            } else if (src.charAt(pos) == '#') {
                skipComment();
            } else {
                break;
            }
        }
    }

    private void skipComment() {
        while (pos < src.length() && src.charAt(pos) != '\n') {
            pos++;
        }
        if (pos < src.length()) pos++;
    }
}
