package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Maps;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Map;

/**
 * Serializes values by delegating to a serializer based on the type of the
 * node. Reserialization is not currently supported as it is incredibly
 * difficult to determine what serializer should be delegated to.
 */
public final class UnionSerializer<T> implements Serializer<T> {

    public static final UnionSerializer<Object> INSTANCE = new UnionSerializer<>(Maps.newHashMap());

    private final Map<Node.Type, Serializer<? extends T>> deserializers;

    private UnionSerializer(Map<Node.Type, Serializer<? extends T>> deserializers) {
        this.deserializers = deserializers;
    }

    @Override
    public T deserialize(Node node) throws SerializationException {
        if (!deserializers.containsKey(node.getType())) {
            throw new SerializationException(node, "Expected the value to have type in " + deserializers.keySet() + ", received " + node.getType() + ".");
        }
        return node.get(deserializers.get(node.getType()));
    }

    /**
     * Returns a new serializer using the given map of serializers.
     */
    public <T> UnionSerializer<T> of(Map<Node.Type, Serializer<? extends T>> deserializers) {
        return new UnionSerializer<>(deserializers);
    }

}
