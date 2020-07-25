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
    public void lex() throws ParseException {
        while (match("[ \t]")) {}
        chars.emit(StormTokenType.OPERATOR);
        while (chars.has(0)) {
            Token<StormTokenType> token = chars.emit(lexToken());
            if (token.getType() == StormTokenType.NEWLINE) {
                chars.newline();
            } else if (token.getType() == StormTokenType.OPERATOR) {
                switch (token.getLiteral()) {
                    case "{": case "[": context.addLast(token.getRange()); break;
                    case "}": case "]": context.removeLast(); break;
                }
            }
            tokens.add(token);
            while (match("[ \t]")) {}
            chars.emit(StormTokenType.OPERATOR);
        }
    }

    private StormTokenType lexToken() throws ParseException {
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
        return StormTokenType.CHARACTER;
    }

    private StormTokenType lexString() throws ParseException {
        Preconditions.checkState(match('\"'), "Broken lexer invariant.");
        while (peek("[^\"\n\r]")) {
            lexEscape();
        }
        require(match('\"'), () -> Diagnostic.builder()
                .summary("Unterminated string literal.")
                .details("A string literal must be surrounded by double quotes, such as \"abc\". If a literal double-quote is desired, use an escape as in \"abc\\\"123\".")
                .range(chars.getRange()));
        return StormTokenType.STRING;
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

    private StormTokenType lexOperator() {
        chars.advance();
        return StormTokenType.OPERATOR;
    }

}
