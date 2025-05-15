package com.example.upbeat_backend.game.exception.parser;

import com.example.upbeat_backend.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class ParserException {
    public static class UnexpectedToken extends BaseException {
        public UnexpectedToken(String expected, String found, int position) {
            super("Expected " + expected + " but found '" + found + "' at position " + position,
                  HttpStatus.BAD_REQUEST,
                  "UNEXPECTED_TOKEN");
        }
    }

    public static class MissingToken extends BaseException {
        public MissingToken(String expected, int position) {
            super("Expected " + expected + " but found nothing at position " + position,
                  HttpStatus.BAD_REQUEST,
                  "MISSING_TOKEN");
        }
    }

    public static class InvalidExpression extends BaseException {
        public InvalidExpression(String message, int position) {
            super("Invalid expression at position " + position + ": " + message,
                  HttpStatus.BAD_REQUEST,
                  "INVALID_EXPRESSION");
        }
    }

    public static class InvalidStatement extends BaseException {
        public InvalidStatement(String token, int position) {
            super("Invalid statement starting with '" + token + "' at position " + position,
                  HttpStatus.BAD_REQUEST,
                  "INVALID_STATEMENT");
        }
    }

    public static class InfiniteLoop extends BaseException {
        public InfiniteLoop() {
            super("Infinite loop detected in while statement",
                  HttpStatus.BAD_REQUEST,
                  "INFINITE_LOOP");
        }
    }

    public static class UndefinedVariable extends BaseException {
        public UndefinedVariable(String variableName) {
            super("Undefined variable '" + variableName + "'",
                  HttpStatus.BAD_REQUEST,
                  "UNDEFINED_VARIABLE");
        }
    }

    public static class DivisionByZero extends BaseException {
        public DivisionByZero() {
            super("Division by zero",
                  HttpStatus.BAD_REQUEST,
                  "DIVISION_BY_ZERO");
        }
    }

    public static class UnknownCommand extends BaseException {
        public UnknownCommand(String command, int position) {
            super("Unknown command '" + command + "' at position " + position,
                  HttpStatus.BAD_REQUEST,
                  "UNKNOWN_COMMAND");
        }
    }

    public static class InvalidDirection extends BaseException {
        public InvalidDirection(String direction, int position) {
            super("Invalid direction '" + direction + "' at position " + position +
                  ". Expected: up, down, up left, up right, down left, down right",
                  HttpStatus.BAD_REQUEST,
                  "INVALID_DIRECTION");
        }
    }

    public static class CannotUseOperator extends BaseException {
        public CannotUseOperator(String operator) {
            super("Cannot use operator '" + operator + "' in CalculateExpression",
                  HttpStatus.BAD_REQUEST,
                  "CANNOT_USE_OPERATOR");
        }
    }

    public static class UnknownIdentifier extends BaseException {
        public UnknownIdentifier(String identifier) {
            super("Unknown identifier '" + identifier + "'",
                  HttpStatus.BAD_REQUEST,
                  "UNKNOWN_IDENTIFIER");
        }
    }
}