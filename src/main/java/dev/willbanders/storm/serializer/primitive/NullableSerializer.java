package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Objects;

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

    public <T> NullableSerializer<T> of(Serializer<T> serializer) {
        return new NullableSerializer<>(serializer, null, false);
    }

    public NullableSerializer<T> def(T def) {
        return new NullableSerializer<>(serializer, def, false);
    }

    public NullableSerializer<T> convertDef(boolean convertDef) {
        return new NullableSerializer<>(serializer, def, convertDef);
    }

}
