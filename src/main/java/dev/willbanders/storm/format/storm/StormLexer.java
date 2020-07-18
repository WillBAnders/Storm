package dev.willbanders.storm.format.storm;

import dev.willbanders.storm.format.Lexer;
import dev.willbanders.storm.format.ParseException;
import dev.willbanders.storm.format.Token;

import java.util.ArrayList;
import java.util.List;

public final class StormLexer extends Lexer<StormTokenType> {

    StormLexer(String input) {
        super(input);
    }

    @Override
    protected List<Token<StormTokenType>> lex() throws ParseException {
        while (match("[ \t]")) {}
        chars.emit();
        List<Token<StormTokenType>> tokens = new ArrayList<>();
        while (chars.has(0)) {
            startIndex = chars.getIndex();
            tokens.add(new Token<>(lexToken(), chars.emit()));
            while (match("[ \t]")) {}
            chars.emit();
        }
        return tokens;
    }

    protected StormTokenType lexToken() throws ParseException {
        if (peek("[\n\r]")) {
            return lexNewline();
        } else if (peek("[a-z]")) {
            return lexIdentifier();
        } else if (peek("[0-9]") || peek("[+-]", "[0-9]")) {
            return lexNumber();
        } else if (peek('\'')) {
            return lexCharacter();
        } else if (peek('\"')) {
            return lexString();
        } else {
            return lexOperator();
        }
    }

    private StormTokenType lexNewline() {
        if (match('\n')) {
            match('\r');
        } else if (match('\r')) {
            match('\n');
        }
        return StormTokenType.NEWLINE;
    }

    private StormTokenType lexIdentifier() {
        while (match("[A-Za-z_-]")) {}
        return StormTokenType.IDENTIFIER;
    }

    private StormTokenType lexNumber() {
        match("[+-]");
        require(match("[0-9]"), "Expected a digit 0-9.");
        while (match("[0-9]"));
        if (match('.', "[0-9]")) {
            while (match("[0-9]")) {}
            return StormTokenType.DECIMAL;
        }
        return StormTokenType.INTEGER;
    }

    private StormTokenType lexCharacter() throws ParseException {
        assert peek('\'');
        match('\'');
        while (peek("[^\'\\\\]")) {
            if (match('\\')) {
                if (match('u')) {
                    for (int i = 0; i < 4; i++) {
                        require(match("[0-9A-F]"), "Invalid unicode escape.");
                    }
                } else {
                    require(match("[bfnrt\"\\/\\\\]"), "Invalid escape character.");
                }
            } else {
                chars.advance();
            }
        }
        require(match('\''), "Unterminated string literal.");
        return StormTokenType.CHARACTER;
    }

    private StormTokenType lexString() throws ParseException {
        assert peek('\"');
        match('\"');
        while (peek("[^\"\\\\]")) {
            if (match('\\')) {
                if (match('u')) {
                    for (int i = 0; i < 4; i++) {
                        require(match("[0-9A-F]"), "Invalid unicode escape.");
                    }
                } else {
                    require(match("[bfnrt\"\\/\\\\]"), "Invalid escape character.");
                }
            } else {
                chars.advance();
            }
        }
        require(match('\"'), "Unterminated string literal.");
        return StormTokenType.STRING;
    }

    private StormTokenType lexOperator() throws ParseException {
        chars.advance();
        return StormTokenType.OPERATOR;
    }

}
