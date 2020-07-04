package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.regex.Pattern;

/**
 * Deserializes a {@link Node.Type#CHARACTER} value. A {@link Pattern} may be
 * provided to require the value to match a regular expression.
 */
public final class CharacterSerializer implements Serializer<Character> {

    public static final CharacterSerializer INSTANCE = new CharacterSerializer(null);

    private final Pattern pattern;

    private CharacterSerializer(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public Character deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.CHARACTER) {
            throw new SerializationException(node, "Expected a character value.");
        }
        Character value = (Character) node.getValue();
        if (pattern != null && !pattern.matcher(value.toString()).matches()) {
            throw new SerializationException(node, "Expected character to match " + pattern + ".");
        }
        return value;
    }

    @Override
    public void serialize(Node node, Character value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (pattern != null && !pattern.matcher(value.toString()).matches()) {
            throw new SerializationException(node, "Expected character to match " + pattern + ".");
        }
        if (node.getType() == Node.Type.UNDEFINED) {
            node.attach();
        }
        node.setValue(value);
    }

    /**
     * Returns a new serializer requiring the value to match the given regular
     * expression, compiled using {@link Pattern#compile(String)}.
     */
    public CharacterSerializer matches(String regex) {
        return matches(Pattern.compile(regex));
    }

    /**
     * Returns a new serializer requiring the value to match the given pattern.
     */
    public CharacterSerializer matches(Pattern pattern) {
        return new CharacterSerializer(pattern);
    }

}
