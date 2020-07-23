package dev.willbanders.storm.format;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public abstract class Lexer<T extends Token.Type> {

    protected final String input;
    protected final CharStream chars = new CharStream();
    protected final List<Token<T>> tokens = Lists.newArrayList();
    protected final Deque<Diagnostic.Range> context = new ArrayDeque<>();

    protected Lexer(String input) {
        this.input = input;
    }

    public abstract void lex() throws ParseException;

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
                .context(ImmutableList.copyOf(context))
                .build());
    }

    protected final class CharStream {

        private int index = 0;
        private int line = 1;
        private int column = 1;
        private int length = 0;
        private final StringBuilder builder = new StringBuilder();

        private CharStream() {}

        public Diagnostic.Range getRange() {
            return Diagnostic.range(index - length, line, column, length);
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
            length++;
        }

        public Token<T> emit(T type) {
            String literal = builder.toString();
            Diagnostic.Range range = getRange();
            builder.setLength(0);
            column += length;
            length = 0;
            return new Token<>(type, literal, range);
        }

        public void newline() {
            line++;
            column = 1;
        }

    }

}
