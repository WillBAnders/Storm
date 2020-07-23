package dev.willbanders.storm.format;

public final class Token<T extends Token.Type> {

    public interface Type {}

    private final T type;
    private final String literal;
    private final Diagnostic.Range range;

    Token(T type, String literal, Diagnostic.Range range) {
        this.type = type;
        this.literal = literal;
        this.range = range;
    }

    public T getType() {
        return type;
    }

    public String getLiteral() {
        return literal;
    }

    public Diagnostic.Range getRange() {
        return range;
    }

}
