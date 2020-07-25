package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Map;

/**
 * Serializes a {@link Node.Type#OBJECT} value into a {@link Map} with String
 * keys and values of type {@link T}. A {@link Range} may be provided to require
 * the size of the map to be contained within a range.
 */
public final class MapSerializer<T> implements Serializer<Map<String, T>> {

    public static final MapSerializer<Object> INSTANCE = new MapSerializer<>(null, Range.all());

    private final Serializer<T> serializer;
    private final Range<Integer> size;

    private MapSerializer(Serializer<T> serializer, Range<Integer> size) {
        this.serializer = serializer;
        this.size = size;
    }

    @Override
    public Map<String, T> deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.OBJECT) {
            throw new SerializationException(node, "Expected an object value.");
        } else if (!size.contains(node.getMap().size())) {
            throw new SerializationException(node, "Expected the size of the map to be in range " + size + ".");
        }
        return Maps.newHashMap(Maps.transformValues(node.getMap(), serializer::deserialize));
    }

    @Override
    public void reserialize(Node node, Map<String, T> value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (!size.contains(value.size())) {
            throw new SerializationException(node, "Expected the size of the map to be in range " + size + ".");
        }
        node.attach().setValue(ImmutableMap.of());
        for (Map.Entry<String, T> entry : value.entrySet()) {
            node.set(entry.getKey(), entry.getValue(), serializer);
        }
    }

    /**
     * Returns a new MapSerializer that delegates to the given serializer for
     * serializing values.
     */
    public <T> MapSerializer<T> of(Serializer<T> serializer) {
        return new MapSerializer<>(serializer, Range.all());
    }

    /**
     * Returns a new serializer requiring the size of the map to be contained
     * within the given range.
     */
    public MapSerializer<T> size(Range<Integer> size) {
        return new MapSerializer<>(serializer, size);
    }

}
