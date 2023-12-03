package dev.willbanders.storm.format.json;

import com.google.common.base.Preconditions;
import dev.willbanders.storm.format.Diagnostic;
import dev.willbanders.storm.format.Lexer;
import dev.willbanders.storm.format.ParseException;
import dev.willbanders.storm.format.Token;

public final class JsonLexer extends Lexer<JsonTokenType> {

    JsonLexer(String input) {
        super(input);
    }

    @Override
    public Token<JsonTokenType> lexToken() throws ParseException {
        while (match("[ \n\r\t]")) {
            if (chars.get(-1) == '\n' || chars.get(-1) == '\r') {
                match(chars.get(-1) == '\n' ? '\r' : '\n');
                chars.emit(JsonTokenType.OPERATOR);
                chars.newline();
            }
        }
        chars.emit(JsonTokenType.OPERATOR);
        if (!chars.has(0)) {
            return null;
        } else if (peek("[A-Za-z]")) {
            return lexIdentifier();
        } else if (peek("[0-9]") || peek('-', "[0-9]")) {
            return lexNumber();
        } else if (peek('\"')) {
            return lexString();
        } else {
            return lexOperator();
        }
    }

    private Token<JsonTokenType> lexIdentifier() {
        Preconditions.checkState(match("[A-Za-z]"), "Broken lexer invariant.");
        while (match("[A-Za-z]")) {}
        return chars.emit(JsonTokenType.IDENTIFIER);
    }

    private Token<JsonTokenType> lexNumber() {
        match('-');
        Preconditions.checkState(match("[0-9]"), "Broken lexer invariant.");
        if (chars.get(-1) != '0') {
            while (match("[0-9]")) {}
        }
        boolean decimal = match('.');
        if (decimal) {
            require(match("[0-9]"), () -> Diagnostic.builder()
                    .summary("Invalid decimal.")
                    .details("A decimal point must be followed by a digit, such as \'1.0\'."));
            while (match("[0-9]")) {}
        }
        if (match("[eE]")) {
            // While exponents can be used to represent integer values, most
            // languages (including Java and Storm) only allow exponents for
            // decimals. Hence, numbers with exponents are considered decimals
            // rather than determining the type using the numerical value.
            decimal = true;
            match("[+\\-]");
            require(match("[0-9]"), () -> Diagnostic.builder()
                    .summary("Invalid exponent.")
                    .details("An exponent must be followed by a digit, such as \'1e6\' or \'1.0E-6\'."));
            while (match("[0-9]")) {}
        }
        return chars.emit(decimal ? JsonTokenType.DECIMAL : JsonTokenType.INTEGER);
    }

    private Token<JsonTokenType> lexString() throws ParseException {
        Preconditions.checkState(match('\"'), "Broken lexer invariant.");
        while (peek("[^\"\\p{Cntrl}]")) {
            lexEscape();
        }
        require(match('\"'), () -> Diagnostic.builder()
                .summary("Unterminated string literal.")
                .details("A string literal must be surrounded by double quotes, such as \"abc\". If a literal double-quote is desired, use an escape as in \"abc\\\"123\"."));
        return chars.emit(JsonTokenType.STRING);
    }

    private void lexEscape() throws ParseException {
        if (match('\\')) {
            Diagnostic.Range range = chars.getRange();
            if (match('u')) {
                for (int i = 0; i < 4; i++) {
                    if (!chars.has(0)) {
                        return;
                    } else if (!match("[0-9A-Fa-f]")) {
                        throw error(Diagnostic.builder()
                                .summary("Invalid unicode escape character.")
                                .details("A unicode escape is in the form \\uXXXX, where X is a hexadecimal digit (0-9 & A-F/a-f). If a literal backslash is desired, use an escape as in \"abc\\\\123\".")
                                .range(Diagnostic.range(range.getIndex() + range.getLength() - 1, range.getLine(), range.getColumn() + range.getLength() - 1, i + 3)));
                    }
                }
            } else if (!match("[bfnrt\"\\\\/]") && chars.has(0)) {
                throw error(Diagnostic.builder()
                        .summary("Invalid escape character.")
                        .details("An escape is in the form \\char, where char is one of b, f, n, r, t, \", \\, and /. If a literal backslash is desired, use an escape as in \"abc\\\\123\".")
                        .range(Diagnostic.range(range.getIndex() + range.getLength() - 1, range.getLine(), range.getColumn() + range.getLength() - 1, 2)));
            }
        } else {
            chars.advance();
        }
    }

    private Token<JsonTokenType> lexOperator() {
        chars.advance();
        return chars.emit(JsonTokenType.OPERATOR);
    }

}
