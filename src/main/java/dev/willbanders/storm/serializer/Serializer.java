package dev.willbanders.storm.serializer;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.primitive.NullableSerializer;
import dev.willbanders.storm.serializer.primitive.OptionalSerializer;

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

    /**
     * Returns a new serializer that delegates to the given serializer if the
     * node value is not {@link Node.Type#NULL}.
     *
     * @see NullableSerializer#of(Serializer)
     */
    default NullableSerializer<T> nullable() {
        return NullableSerializer.INSTANCE.of(this);
    }

    /**
     * Returns a new serializer with a default value for when the node is {@link
     * Node.Type#NULL}. By default, this will be reserialized to the
     * corresponding config value and not converted to {@code null}.
     *
     * @see NullableSerializer#def(Object)
     */
    default NullableSerializer<T> nullable(T def) {
        return nullable().def(def);
    }

    /**
     * Returns a new serializer that delegates to the given serializer if the
     * node value is not {@link Node.Type#UNDEFINED}.
     *
     * @see OptionalSerializer#of(Serializer)
     */
    default OptionalSerializer<T> optional() {
        return OptionalSerializer.INSTANCE.of(this);
    }

    /**
     * Returns a new serializer with a default value for when the node is {@link
     * Node.Type#UNDEFINED}. By default, this will be reserialized to the
     * corresponding config value and not converted to {@code undefined}.
     *
     * @see OptionalSerializer#def(Object)
     */
    default OptionalSerializer.OptionalDefaultSerializer<T> optional(T def) {
        return optional().def(def);
    }

}
