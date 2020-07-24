package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.regex.Pattern;

/**
 * Serializes a {@link Node.Type#STRING} value. A {@link Pattern} may be
 * provided to require the value to match a regular expression.
 */
public final class StringSerializer implements Serializer<String> {

    public static final StringSerializer INSTANCE = new StringSerializer(null);

    private final Pattern pattern;

    private StringSerializer(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public String deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.STRING) {
            throw new SerializationException(node, "Expected a string value.");
        }
        String value = (String) node.getValue();
        if (pattern != null && !pattern.matcher(value).matches()) {
            throw new SerializationException(node, "Expected string to match " + pattern + ".");
        }
        return value;
    }

    @Override
    public void reserialize(Node node, String value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (pattern != null && !pattern.matcher(value).matches()) {
            throw new SerializationException(node, "Expected string to match " + pattern + ".");
        }
        node.attach().setValue(value);
    }

    /**
     * Returns a new serializer requiring the value to match the given regular
     * expression, provided as a String and compiled using {@link
     * Pattern#compile(String)}.
     */
    public StringSerializer matches(String regex) {
        return matches(Pattern.compile(regex));
    }

    /**
     * Returns a new serializer requiring the value to match the given regular
     * expression, provided as a {@link Pattern}.
     */
    public StringSerializer matches(Pattern pattern) {
        return new StringSerializer(pattern);
    }

}
