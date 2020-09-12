package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

/**
 * Serializes a value of any defined, non-null type.
 *
 * @see NullableSerializer#INSTANCE for nullable values
 * @see OptionalSerializer#INSTANCE for undefined values
 */
public final class AnySerializer implements Serializer<Object> {

    public static final AnySerializer INSTANCE = new AnySerializer();

    private AnySerializer() {}

    @Override
    public Object deserialize(Node node) throws SerializationException {
        if (node.getType() == Node.Type.UNDEFINED) {
            throw new SerializationException(node, "Expected a defined value.");
        } else if (node.getType() == Node.Type.NULL) {
            throw new SerializationException(node, "Expected a non-null value.");
        }
        return node.getValue();
    }

    @Override
    public void reserialize(Node node, Object value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        }
        node.attach().setValue(value);
    }

}
