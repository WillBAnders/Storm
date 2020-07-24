package dev.willbanders.storm.serializer;

import dev.willbanders.storm.config.Node;

/**
 * Serializes values to and from a config node. All serializers support
 * deserialization, but reserialization may not be supported.
 */
@FunctionalInterface
public interface Serializer<T> {

    /**
     * Deserializes a value from the given config node. This is a narrowing
     * conversion, and thus different representations in the config
     * may serialize to the same value, loosing information.
     *
     * @throws SerializationException if the config could not be deserialized
     */
    T deserialize(Node node) throws SerializationException;

    /**
     * Reserializes a value to the given config node. This is a widening
     * conversion, and thus the representation of a value in the config may be
     * different from what it was originally deserialized from.
     *
     * @throws SerializationException if the value could not be reserialized
     */
    default void reserialize(Node node, T value) throws SerializationException {
        throw new SerializationException(node, "Reserialization is not supported for this serializer.");
    }

}
