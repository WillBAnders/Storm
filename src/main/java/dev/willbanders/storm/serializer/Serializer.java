package dev.willbanders.storm.serializer;

import dev.willbanders.storm.config.Node;

/**
 * Serializes values to and from a config node. This interface also supports
 * deserialization as all serializable values should be deserializable, but
 * deserialization itself is not strictly invertible.
 *
 * @see Deserializer
 */
public interface Serializer<T> extends Deserializer<T> {

    /**
     * Serializes a value to the given config node. Serialization is a widening
     * conversion, and thus the representation of a value in the config may be
     * different from what it was originally deserialized from.
     *
     * @throws SerializationException if the value could not be serialized
     */
    void serialize(Node node, T value) throws SerializationException;

}
