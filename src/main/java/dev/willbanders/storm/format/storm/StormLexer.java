package dev.willbanders.storm.format.storm;

import com.google.common.base.Preconditions;
import dev.willbanders.storm.format.Diagnostic;
import dev.willbanders.storm.format.Lexer;
import dev.willbanders.storm.format.ParseException;
import dev.willbanders.storm.format.Token;

public final class StormLexer extends Lexer<StormTokenType> {

    StormLexer(String input) {
        super(input);
    }

    @Override
    public Token<StormTokenType> lexToken() throws ParseException {
        while (match("[ \t]")) {}
        chars.emit(StormTokenType.OPERATOR);
        if (!chars.has(0)) {
            return null;
        } else if (peek('/', '/')) {
            return lexComment();
        } else if (peek("[\n\r]")) {
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

    private Token<StormTokenType> lexComment() {
        Preconditions.checkState(match('/', '/'), "Broken lexer invariant.");
        while (match("[^\n\r]")) {}
        Token<StormTokenType> token = chars.emit(StormTokenType.COMMENT);
        if (chars.has(0)) {
            lexNewline();
        }
        return token;
    }

    private Token<StormTokenType> lexNewline() {
        if (match('\n')) {
            match('\r');
        } else if (match('\r')) {
            match('\n');
        } else {
            throw new IllegalStateException("Broken lexer invariant.");
        }
        Token<StormTokenType> token = chars.emit(StormTokenType.NEWLINE);
        chars.newline();
        return token;
    }

    private Token<StormTokenType> lexIdentifier() {
        Preconditions.checkState(match("[a-z]"), "Broken lexer invariant.");
        while (match("[a-z_-]")) {}
        return chars.emit(StormTokenType.IDENTIFIER);
    }

    private Token<StormTokenType> lexNumber() {
        match("[+-]");
        Preconditions.checkState(match("[0-9]"), "Broken lexer invariant.");
        while (match("[0-9]")) {}
        if (match('.', "[0-9]")) {
            while (match("[0-9]")) {}
            return chars.emit(StormTokenType.DECIMAL);
        }
        return chars.emit(StormTokenType.INTEGER);
    }

    private Token<StormTokenType> lexCharacter() throws ParseException {
        Preconditions.checkState(match('\''), "Broken lexer invariant.");
        require(chars.has(0) && !match('\''), () -> Diagnostic.builder()
                .summary("Empty character literal.")
                .details("A character literal must contain a single character, such as \'c\'. If a literal single-quote is desired, use an escape as in \'\\\'\'."));
        lexEscape();
        if (!match('\'')) {
            while (peek("[^\'\n\r]")) {
                chars.advance();
            }
            if (match('\'')) {
                throw error(Diagnostic.builder()
                        .summary("Character literal contains multiple characters.")
                        .details("A character literal must contain a single character, such as \'c\'. If multiple characters is desired, use a string as in \"abc\"")
                        .range(chars.getRange()));
            } else {
                throw error(Diagnostic.builder()
                        .summary("Unterminated character literal.")
                        .details("A character literal must be surrounded by single quotes and contain a single character, such as \'c\'.")
                        .range(chars.getRange()));
            }
        }
        return chars.emit(StormTokenType.CHARACTER);
    }

    private Token<StormTokenType> lexString() throws ParseException {
        Preconditions.checkState(match('\"'), "Broken lexer invariant.");
        while (peek("[^\"\n\r]")) {
            lexEscape();
        }
        require(match('\"'), () -> Diagnostic.builder()
                .summary("Unterminated string literal.")
                .details("A string literal must be surrounded by double quotes, such as \"abc\". If a literal double-quote is desired, use an escape as in \"abc\\\"123\".")
                .range(chars.getRange()));
        return chars.emit(StormTokenType.STRING);
    }

    private void lexEscape() throws ParseException {
        if (match('\\')) {
            Diagnostic.Range range = chars.getRange();
            if (match('u')) {
                for (int i = 0; i < 4; i++) {
                    if (!chars.has(0)) {
                        return;
                    } else if (!match("[0-9A-F]")) {
                        throw error(Diagnostic.builder()
                                .summary("Invalid unicode escape character.")
                                .details("A unicode escape is in the form \\uXXXX, where X is a hexadecimal digit (0-9 & A-F). If a literal backslash is desired, use an escape as in \"abc\\\\123\".")
                                .range(Diagnostic.range(range.getIndex() + range.getLength() - 1, range.getLine(), range.getColumn() + range.getLength() - 1, i + 3)));
                    }
                }
            } else if (!match("[bfnrt\'\"\\\\]") && chars.has(0)) {
                throw error(Diagnostic.builder()
                        .summary("Invalid escape character.")
                        .details("An escape is in the form \\char, where char is one of b, f, n, r, t, \', \", and \\. If a literal backslash is desired, use an escape as in \"abc\\\\123\".")
                        .range(Diagnostic.range(range.getIndex() + range.getLength() - 1, range.getLine(), range.getColumn() + range.getLength() - 1, 2)));
            }
        } else {
            chars.advance();
        }
    }

    private Token<StormTokenType> lexOperator() {
        chars.advance();
        return chars.emit(StormTokenType.OPERATOR);
    }

}
