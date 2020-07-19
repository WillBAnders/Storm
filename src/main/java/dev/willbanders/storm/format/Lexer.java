package dev.willbanders.storm.format;

import com.google.common.base.Preconditions;

import java.util.List;

public abstract class Lexer<T extends Token.Type> {

    protected final CharStream chars;
    protected int startIndex;

    protected Lexer(String input) {
        chars = new CharStream(input);
    }

    protected abstract List<Token<T>> lex() throws ParseException;

    protected boolean peek(Object... objects) {
        for (int i = 0; i < objects.length; i++) {
            if (!chars.has(i) || !test(objects[i], chars.get(i))) {
                return false;
            }
        }
        return true;
    }

    protected boolean match(Object... objects) {
        boolean peek = peek(objects);
        if (peek) {
            for (int i = 0; i < objects.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    private boolean test(Object object, char character) {
        if (object instanceof Character) {
            return (Character) object == character;
        } else if (object instanceof String) {
            return Character.toString(character).matches((String) object);
        } else if (object instanceof List) {
            return ((List<?>) object).stream().anyMatch(o -> test(o, character));
        } else {
            throw new AssertionError();
        }
    }

    protected void require(boolean condition, String message) throws ParseException {
        if (!condition) {
            throw new ParseException(message);
        }
    }

    protected static final class CharStream {

        private final String input;
        private int index = 0;
        private final StringBuilder builder = new StringBuilder();

        private CharStream(String input) {
            this.input = input;
        }

        public int getIndex() {
            return index;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            Preconditions.checkState(has(offset), "Broken lexer invariant.");
            return input.charAt(index + offset);
        }

        public void advance() {
            Preconditions.checkState(has(0), "Broken lexer invariant.");
            builder.append(input.charAt(index++));
        }

        public String emit() {
            String literal = builder.toString();
            builder.setLength(0);
            return literal;
        }

    }

}
