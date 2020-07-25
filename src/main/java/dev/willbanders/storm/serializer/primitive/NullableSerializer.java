package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Objects;

/**
 * Serializes values which may be {@link Node.Type#NULL}, delegating to another
 * serializer for other types. This serializer may return {@code null} or define
 * another default value to be used instead.
 *
 * The default behavior for reserialization is to keep the default value as-is.
 * For example, a nullable serializer with a default of {@code 0} will serialize
 * {@code 0} to the value itself, not {@code null}. This behavior can be changed
 * by setting {@link #convertDef(boolean)} to {@code true}.
 */
public final class NullableSerializer<T> implements Serializer<T> {

    public static final NullableSerializer<Object> INSTANCE = new NullableSerializer<>(null, null, false);

    private final Serializer<T> serializer;
    private final T def;
    private final boolean convertDef;

    private NullableSerializer(Serializer<T> serializer, T def, boolean convertDef) {
        this.serializer = serializer;
        this.def = def;
        this.convertDef = convertDef;
    }

    @Override
    public T deserialize(Node node) throws SerializationException {
        if (node.getType() == Node.Type.NULL) {
            return def;
        }
        return node.get(serializer);
    }

    @Override
    public void reserialize(Node node, T value) throws SerializationException {
        if (value == null && def != null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (value == null || convertDef && Objects.equals(value, def)) {
            node.attach().setValue(null);
        } else {
            node.set(value, serializer);
        }
    }

    /**
     * Returns a new NullableSerializer that delegates to the given serializer
     * if the node value is not {@link Node.Type#NULL}.
     */
    public <T> NullableSerializer<T> of(Serializer<T> serializer) {
        return new NullableSerializer<>(serializer, null, false);
    }

    /**
     * Sets the default value for when the node is {@link Node.Type#NULL}. By
     * default, this will be reserialized to the corresponding value and not
     * converted to {@code null}.
     *
     * @see #convertDef(boolean)
     */
    public NullableSerializer<T> def(T def) {
        return new NullableSerializer<>(serializer, def, false);
    }

    /**
     * True if the default value should be reserialized to {@code null} instead
     * of it's normal value. The default behavior is {@code false}.
     */
    public NullableSerializer<T> convertDef(boolean convertDef) {
        return new NullableSerializer<>(serializer, def, convertDef);
    }

}
