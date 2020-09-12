package dev.willbanders.storm.format;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.Supplier;

public abstract class Lexer<T extends Token.Type> {

    protected final String input;
    protected final CharStream chars = new CharStream();

    protected Lexer(String input) {
        this.input = input;
    }

    public abstract Token<T> lexToken() throws ParseException;

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

    protected void require(boolean condition, Supplier<Diagnostic.Builder> supplier) throws ParseException {
        if (!condition) {
            throw error(supplier.get().range(chars.getRange()));
        }
    }

    protected ParseException error(Diagnostic.Builder builder) {
        return new ParseException(builder
                .input(input)
                .context(ImmutableList.of())
                .build());
    }

    protected final class CharStream {

        private int index = 0;
        private int line = 1;
        private int column = 1;
        private int length = 0;

        private CharStream() {}

        public Diagnostic.Range getRange() {
            return Diagnostic.range(index, line, column, length);
        }

        public boolean has(int offset) {
            return index + length + offset < input.length();
        }

        public char get(int offset) {
            Preconditions.checkState(has(offset), "Broken lexer invariant.");
            return input.charAt(index + length + offset);
        }

        public void advance() {
            Preconditions.checkState(has(0), "Broken lexer invariant.");
            length++;
        }

        public Token<T> emit(T type) {
            Diagnostic.Range range = getRange();
            index += length;
            column += length;
            length = 0;
            return new Token<>(type, input.substring(range.getIndex(), index), range);
        }

        public void newline() {
            Preconditions.checkState(length == 0);
            line++;
            column = 1;
        }

    }

}
