package dev.willbanders.storm.format;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.willbanders.storm.config.Node;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public abstract class Parser<T extends Token.Type> {

    protected final Lexer<T> lexer;
    protected final TokenStream tokens = new TokenStream();
    protected final Deque<Diagnostic.Range> context = new ArrayDeque<>();

    protected Parser(Lexer<T> lexer) throws ParseException {
        this.lexer = lexer;
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

    private boolean test(Object object, Token<T> token) {
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

    protected void require(boolean condition, Supplier<Diagnostic.Builder> supplier) throws ParseException {
        if (!condition) {
            throw error(supplier.get().range((tokens.has(0) ? tokens.get(0) : tokens.get(-1)).getRange()));
        }
    }

    protected ParseException error(Diagnostic.Builder builder) {
        return new ParseException(builder
                .input(lexer.input)
                .context(ImmutableList.copyOf(context))
                .build());
    }

    protected final class TokenStream {

        private final List<Token<T>> tokens = Lists.newArrayList();
        private int index = 0;

        private TokenStream() {}

        public boolean has(int offset) throws ParseException {
            try {
                while (index + offset >= tokens.size()) {
                    tokens.add(lexer.lexToken());
                }
                return tokens.get(index + offset) != null;
            } catch (ParseException e) {
                throw (ParseException) new ParseException(Diagnostic.builder()
                        .input(e.getDiagnostic().getInput())
                        .summary(e.getDiagnostic().getSummary())
                        .details(e.getDiagnostic().getDetails())
                        .range(e.getDiagnostic().getRange())
                        .context(ImmutableList.copyOf(context))
                        .build()).initCause(e);
            }
        }

        public Token<T> get(int offset) throws ParseException {
            Preconditions.checkState(has(offset), "Broken parser invariant.");
            return tokens.get(index + offset);
        }

        public void advance() {
            Preconditions.checkState(has(0), "Broken parser invariant.");
            index++;
        }

    }

}
