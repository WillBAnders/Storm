package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Map;
import java.util.Set;

/**
 * Serializes a {@link Node.Type#OBJECT} value into a {@link Map} with String
 * keys and values of type {@link T}. Unlike {@link MapSerializer}, this
 * serializer supports non-homogeneous maps as in {@code {x = true, y = 1, z =
 * "string"}}.
 */
public final class ObjectSerializer<T> implements Serializer<Map<String, ? extends T>> {

    public static final ObjectSerializer<Object> INSTANCE = new ObjectSerializer<>(Maps.newHashMap());

    private final Map<String, Serializer<? extends T>> serializers;

    private ObjectSerializer(Map<String, Serializer<? extends T>> serializers) {
        this.serializers = serializers;
    }

    @Override
    public Map<String, ? extends T> deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.OBJECT) {
            throw new SerializationException(node, "Expected an object value.");
        } else if (!serializers.keySet().containsAll(node.getMap().keySet())) {
            Set<String> unexpected = Sets.difference(node.getMap().keySet(), serializers.keySet());
            throw new SerializationException(node, "Unexpected properties " + unexpected + ".");
        }
        return Maps.newHashMap(Maps.transformEntries(serializers, node::get));
    }

    @Override
    public void reserialize(Node node, Map<String, ? extends T> value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (!value.keySet().equals(serializers.keySet())) {
            Set<String> expected = Sets.difference(serializers.keySet(), value.keySet());
            Set<String> unexpected = Sets.difference(value.keySet(), serializers.keySet());
            throw new SerializationException(node, "Expected properties " + expected + " and not " + unexpected + ".");
        }
        node.attach().setValue(Maps.newHashMap());
        for (Map.Entry<String, ? extends T> entry : value.entrySet()) {
            try {
                node.set(entry.getKey(), entry.getValue(), (Serializer<T>) serializers.get(entry.getKey()));
            } catch (ClassCastException e) {
                throw new SerializationException(node.resolve(entry.getKey()), e.getMessage());
            }
        }
    }

    public <T> ObjectSerializer<T> of(Map<String, Serializer<? extends T>> serializers) {
        return new ObjectSerializer<>(serializers);
    }

}
