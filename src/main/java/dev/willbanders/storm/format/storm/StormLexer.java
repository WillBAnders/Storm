package dev.willbanders.storm.format.storm;

import com.google.common.base.Preconditions;
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
        } else {
            throw new IllegalStateException("Broken lexer invariant.");
        }
        return StormTokenType.NEWLINE;
    }

    private StormTokenType lexIdentifier() {
        Preconditions.checkState(match("[a-z]"), "Broken lexer invariant.");
        while (match("[a-z_-]")) {}
        return StormTokenType.IDENTIFIER;
    }

    private StormTokenType lexNumber() {
        match("[+-]");
        Preconditions.checkState(match("[0-9]"), "Broken lexer invariant.");
        while (match("[0-9]")) {}
        if (match('.', "[0-9]")) {
            while (match("[0-9]")) {}
            return StormTokenType.DECIMAL;
        }
        return StormTokenType.INTEGER;
    }

    private StormTokenType lexCharacter() throws ParseException {
        Preconditions.checkState(match('\''), "Broken lexer invariant.");
        require(peek("[^\']"), "Empty character literal.");
        lexEscape();
        require(match('\''), "Unterminated character literal.");
        return StormTokenType.CHARACTER;
    }

    private StormTokenType lexString() throws ParseException {
        Preconditions.checkState(match('\"'), "Broken lexer invariant.");
        while (peek("[^\"]")) {
            lexEscape();
        }
        require(match('\"'), "Unterminated string literal.");
        return StormTokenType.STRING;
    }

    private void lexEscape() throws ParseException {
        if (match('\\')) {
            if (match('u')) {
                for (int i = 0; i < 4; i++) {
                    require(match("[0-9A-F]"), "Invalid unicode escape character.");
                }
            } else {
                require(match("[bfnrt\'\"\\\\]"), "Invalid escape character.");
            }
        } else {
            chars.advance();
        }
    }

    private StormTokenType lexOperator() {
        chars.advance();
        return StormTokenType.OPERATOR;
    }

}
