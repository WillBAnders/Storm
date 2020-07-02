package dev.willbanders.storm.serializer;

import dev.willbanders.storm.config.Node;

/**
 * Deserializes a value from a config node. The interface exists for narrowing
 * operations where deserialization is well defined, but the inverse function of
 * serialization is not well defined as the original input cannot be recovered.
 *
 * <p>For example, {@code INTEGER.optional(0)} could re-serialize zero as either
 * an integer or undefined, and it is indeterminate which value of the original
 * config produced that result. Therefore, the intended re-serialization
 * behavior must be explicitly specified prior to using it as a serializer.</p>
 */
@FunctionalInterface
public interface Deserializer<T> {

    /**
     * Deserializes a value from the given config node. Deserialization is a
     * narrowing conversion, and thus different representations in the config
     * may serialize to the same value, loosing information.
     *
     * @throws SerializationException if the config could not be deserialized
     */
    T deserialize(Node node) throws SerializationException;

}
