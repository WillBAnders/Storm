package dev.willbanders.storm.format;

public final class Token<T extends Token.Type> {

    public interface Type {}

    private final T type;
    private final String literal;

    public Token(T type, String literal) {
        this.type = type;
        this.literal = literal;
    }

    public T getType() {
        return type;
    }

    public String getLiteral() {
        return literal;
    }

}
