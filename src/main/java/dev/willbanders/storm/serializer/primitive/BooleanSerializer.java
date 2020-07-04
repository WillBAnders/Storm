package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

/**
 * Deserializes a {@link Node.Type#BOOLEAN} value.
 */
public final class BooleanSerializer implements Serializer<Boolean> {

    public static final BooleanSerializer INSTANCE = new BooleanSerializer();

    private BooleanSerializer() {}

    @Override
    public Boolean deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.BOOLEAN) {
            throw new SerializationException(node, "Expected a boolean value.");
        }
        return (Boolean) node.getValue();
    }

    @Override
    public void serialize(Node node, Boolean value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        }
        if (node.getType() == Node.Type.UNDEFINED) {
            node.attach();
        }
        node.setValue(value);
    }

}
