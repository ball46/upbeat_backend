package com.example.upbeat_backend.game.plans.parser;

import com.example.upbeat_backend.game.exception.parser.ParserException;
import com.example.upbeat_backend.game.model.enums.Keyword;
import com.example.upbeat_backend.game.model.enums.Operator;
import com.example.upbeat_backend.game.model.enums.Special;
import com.example.upbeat_backend.game.model.enums.Type;
import com.example.upbeat_backend.game.plans.parser.ast.Expression;
import com.example.upbeat_backend.game.plans.parser.ast.Node;
import com.example.upbeat_backend.game.plans.parser.ast.Statement;
import com.example.upbeat_backend.game.plans.parser.ast.expressions.BinaryExpression;
import com.example.upbeat_backend.game.plans.parser.ast.expressions.IdentifierExpression;
import com.example.upbeat_backend.game.plans.parser.ast.expressions.NumberExpression;
import com.example.upbeat_backend.game.plans.parser.ast.expressions.SpecialExpression;
import com.example.upbeat_backend.game.plans.parser.ast.expressions.info.NearbyExpression;
import com.example.upbeat_backend.game.plans.parser.ast.expressions.info.OpponentExpression;
import com.example.upbeat_backend.game.plans.parser.ast.plan.PlanNode;
import com.example.upbeat_backend.game.plans.parser.ast.statements.BlockStatement;
import com.example.upbeat_backend.game.plans.parser.ast.statements.IfStatement;
import com.example.upbeat_backend.game.plans.parser.ast.statements.WhileStatement;
import com.example.upbeat_backend.game.plans.parser.ast.statements.commands.AssignmentStatement;
import com.example.upbeat_backend.game.plans.parser.ast.statements.commands.action.*;
import com.example.upbeat_backend.game.plans.tokenizer.Token;
import com.example.upbeat_backend.game.plans.tokenizer.Tokenizer;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class ParserImpl implements Parser {
    private Tokenizer tokenizer;

    @Override
    public Node parse() {
        if (!tokenizer.hasNextToken()) {
            throw new ParserException.MissingToken("No input provided", tokenizer.getPosition());
        }
        return parsePlan();
    }

    private PlanNode parsePlan() {
        List<Statement> statements = new ArrayList<>();
        while (tokenizer.hasNextToken() && tokenizer.peekType() != Type.EOF) {
            statements.add(parseStatement());
        }

        if (statements.isEmpty()) {
            throw new ParserException.InvalidStatement("empty plan", tokenizer.getPosition());
        }

        return new PlanNode(statements);
    }

    private Statement parseStatement() {
        if (!tokenizer.hasNextToken()) {
            throw new ParserException.MissingToken("statement", tokenizer.getPosition());
        }

        if (tokenizer.peekType() == Type.KEYWORD) {
            Keyword keyword = Keyword.fromString(tokenizer.peekValue());

            if (keyword == null) {
                throw new ParserException.InvalidStatement("Unknown keyword: " + tokenizer.peekValue(), tokenizer.getPosition());
            }

            if (keyword.isControlFlow()) {
                if (keyword == Keyword.IF) {
                    return parseIfStatement();
                } else if (keyword == Keyword.WHILE) {
                    return parseWhileStatement();
                }
            } else if (keyword.isAction()) {
                return parseActionStatement();
            }
        } else if (tokenizer.peekType() == Type.IDENTIFIER) {
            return parseAssignmentStatement();
        } else if (tokenizer.peekValue().equals(Operator.LEFT_BRACE.getSymbol())) {
            return parseBlockStatement();
        }

        throw new ParserException.InvalidStatement(tokenizer.peekValue(), tokenizer.getPosition());
    }

    private IfStatement parseIfStatement() {
        expectKeyword(Keyword.IF); // Consume 'if'
        expectOperator(Operator.LEFT_PAREN); // Consume '('

        Expression condition = parseExpression();
        if (condition == null) {
            throw new ParserException.InvalidExpression("null expression in if condition", tokenizer.getPosition());
        }

        expectOperator(Operator.RIGHT_PAREN); // Consume ')'

        expectKeyword(Keyword.THEN); // Consume 'then'
        Statement trueBranch = parseStatement();

        expectKeyword(Keyword.ELSE); // Consume 'else'
        Statement falseBranch = parseStatement();

        return new IfStatement(condition, trueBranch, falseBranch);
    }

    private WhileStatement parseWhileStatement() {
        expectKeyword(Keyword.WHILE); // Consume 'while'
        expectOperator(Operator.LEFT_PAREN); // Consume '('

        Expression condition = parseExpression();
        if (condition == null) {
            throw new ParserException.InvalidExpression("null expression in while condition", tokenizer.getPosition());
        }

        expectOperator(Operator.RIGHT_PAREN); // Consume ')'

        Statement body = parseStatement();

        return new WhileStatement(condition, body);
    }

    private BlockStatement parseBlockStatement() {
        expectOperator(Operator.LEFT_BRACE); // Consume '{'

        List<Statement> blockStatements = new ArrayList<>();
        while (tokenizer.hasNextToken() && !tokenizer.peekValue().equals(Operator.RIGHT_BRACE.getSymbol())) {
            blockStatements.add(parseStatement());
        }

        expectOperator(Operator.RIGHT_BRACE); // Consume '}'
        return new BlockStatement(blockStatements);
    }

    private AssignmentStatement parseAssignmentStatement() {
        String variableName = tokenizer.consume(); // Consume identifier
        expectOperator(Operator.EQUALS); // Consume '='
        Expression expression = parseExpression();

        return new AssignmentStatement(variableName, expression);
    }

    private Statement parseActionStatement() {
        Token actionToken = tokenizer.consumeToken(); // Consume action token
        Keyword actionKeyword = Keyword.fromString(actionToken.value());

        if (actionKeyword == null || !actionKeyword.isAction()) {
            throw new ParserException.InvalidStatement("Not a valid action: " + actionToken.value(), tokenizer.getPosition());
        }

        return switch (actionKeyword) {
            case DONE -> new DoneCommand();
            case RELOCATE -> new RelocateCommand();
            case MOVE -> parseMoveCommand();
            case INVEST -> parseInvestCommand();
            case COLLECT -> parseCollectCommand();
            case SHOOT -> parseShootCommand();
            default -> throw new ParserException.UnknownCommand(actionToken.value(), tokenizer.getPosition());
        };
    }

    private MoveCommand parseMoveCommand() {
        Keyword direction = parseDirection();
        return new MoveCommand(direction);
    }

    private InvestCommand parseInvestCommand() {
        Expression expression = parseExpression();
        if (expression == null) {
            throw new ParserException.InvalidExpression("null expression in invest command", tokenizer.getPosition());
        }

        return new InvestCommand(expression);
    }

    private CollectCommand parseCollectCommand() {
        Expression expression = parseExpression();
        if (expression == null) {
            throw new ParserException.InvalidExpression("null expression in collect command", tokenizer.getPosition());
        }

        return new CollectCommand(expression);
    }

    private ShootCommand parseShootCommand() {
        Keyword direction = parseDirection();

        Expression expression = parseExpression();
        if (expression == null) {
            throw new ParserException.InvalidExpression("null expression in shoot command", tokenizer.getPosition());
        }

        return new ShootCommand(direction, expression);
    }

    private Expression parseExpression() {
        Expression left = parseTerm();
        while (tokenizer.hasNextToken() && tokenizer.peekType().equals(Type.OPERATOR) &&
                (tokenizer.peekValue().equals(Operator.PLUS.getSymbol()) ||
                        tokenizer.peekValue().equals(Operator.MINUS.getSymbol()))) {

            Operator operator = Operator.fromString(tokenizer.consume());

            Expression right = parseTerm();

            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression parseTerm() {
        Expression left = parseFactor();
        while (tokenizer.hasNextToken() && tokenizer.peekType().equals(Type.OPERATOR) &&
                (tokenizer.peekValue().equals(Operator.MULTIPLY.getSymbol()) ||
                        tokenizer.peekValue().equals(Operator.DIVIDE.getSymbol()) ||
                        tokenizer.peekValue().equals(Operator.MOD.getSymbol()))) {

            Operator operator = Operator.fromString(tokenizer.consume());

            Expression right = parseFactor();

            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression parseFactor() {
        Expression left = parsePower();
        while (tokenizer.hasNextToken() && tokenizer.peekType().equals(Type.OPERATOR) &&
                tokenizer.peekValue().equals(Operator.CARET.getSymbol())) {

            Operator operator = Operator.fromString(tokenizer.consume());

            Expression right = parseFactor();

            left = new BinaryExpression(left, right, operator);
        }
        return left;
    }

    private Expression parsePower() {
        if (tokenizer.peekType() == Type.NUMBER) {
            return new NumberExpression(Long.parseLong(tokenizer.consume()));
        } else if (tokenizer.peekType() == Type.SPECIAL) {
            return new SpecialExpression(Special.fromString(tokenizer.consume()));
        } else if (tokenizer.peekType() == Type.IDENTIFIER) {
            return new IdentifierExpression(tokenizer.consume());
        } else if (tokenizer.peekValue().equals(Operator.LEFT_PAREN.getSymbol())) {
            expectOperator(Operator.LEFT_PAREN); // Consume '('
            Expression expression = parseExpression();
            expectOperator(Operator.RIGHT_PAREN); // Consume ')'
            return expression;
        } else if (tokenizer.peekType() == Type.KEYWORD ) {
            Keyword keyword = Keyword.fromString(tokenizer.peekValue());
            if (keyword != null && keyword.isInfo()) {
                return parseInfoExpression();
            }
        }
        throw new ParserException.InvalidExpression(
                "Unexpected token in expression: " + tokenizer.peekValue(),
                tokenizer.getPosition()
        );
    }

    private Expression parseInfoExpression() {
        if (tokenizer.peekValue().equals(Keyword.OPPONENT.getLexeme())) {
            expectKeyword(Keyword.OPPONENT);
            return new OpponentExpression();
        } else if (tokenizer.peekValue().equals(Keyword.NEARBY.getLexeme())) {
            expectKeyword(Keyword.NEARBY);
            Keyword direction = parseDirection();
            return new NearbyExpression(direction);
        }
        return null;
    }

    private void expectAndConsume(String expected) {
        if (!tokenizer.consume(expected)) {
            throw new ParserException.UnexpectedToken(
                expected,
                tokenizer.peekValue(),
                tokenizer.getPosition()
            );
        }
    }

    private void expectKeyword(Keyword keyword) {
        expectAndConsume(keyword.getLexeme());
    }

    private void expectOperator(Operator operator) {
        expectAndConsume(operator.getSymbol());
    }

    private Keyword parseDirection() {
        Token directionToken = tokenizer.consumeToken(); // Consume direction token
        Keyword direction = Keyword.fromString(directionToken.value());
        if (direction == null || !direction.isDirection()) {
            throw new ParserException.InvalidDirection(directionToken.value(), directionToken.position());
        }
        return direction;
    }
}
