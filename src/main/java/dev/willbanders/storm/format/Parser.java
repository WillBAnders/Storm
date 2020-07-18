package dev.willbanders.storm.format;

import com.google.common.base.Preconditions;
import dev.willbanders.storm.config.Node;

import java.util.List;

public abstract class Parser<T extends Token.Type> {

    protected final TokenStream<T> tokens;

    protected Parser(Lexer<T> lexer) throws ParseException {
        tokens = new TokenStream<>(lexer.lex());
    }

    protected abstract Node parse() throws ParseException;

    protected boolean peek(Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            if (!tokens.has(i) || !test(objects[i], tokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    protected boolean match(Object... objects) {
        boolean peek = peek(objects);
        if (peek) {
            for (int i = 0; i < objects.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private boolean test(Object object, Token token) {
        if (object instanceof Token.Type) {
            return object == token.getType();
        } else if (object instanceof String) {
            return object.equals(token.getLiteral());
        } else if (object instanceof List) {
            return ((List<?>) object).stream().anyMatch(o -> test(o, token));
        } else {
            throw new AssertionError();
        }
    }

    protected void require(boolean condition, String message) throws ParseException {
        if (!condition) {
            throw new ParseException(message);
        }
    }

    protected static final class TokenStream<T extends Token.Type> {

        private List<Token<T>> tokens;
        private int index = 0;

        private TokenStream(List<Token<T>> tokens) throws ParseException {
            this.tokens = tokens;
        }

        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        public Token<T> get(int offset) {
            Preconditions.checkState(has(offset));
            return tokens.get(index + offset);
        }

        public void advance() {
            index++;
        }

    }

}
