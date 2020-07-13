package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.Deserializer;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public final class MapSerializer {

    public static final MapSerializerImpl<Object> INSTANCE = new MapSerializerImpl<>(null, Range.all());

    public interface MapDeserializer<T> extends Deserializer<Map<String, T>> {

        MapDeserializer<T> size(Range<Integer> size);

        MapSerializerImpl<T> toSerializer();

    }

    public static final class MapSerializerImpl<T> implements MapDeserializer<T>, Serializer<Map<String, T>> {

        private final Serializer<T> serializer;
        private final Range<Integer> size;

        private MapSerializerImpl(Serializer<T> serializer, Range<Integer> size) {
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
        public void serialize(Node node, Map<String, T> value) throws SerializationException {
            if (value == null) {
                throw new SerializationException(node, "Expected a non-null value.");
            } else if (!size.contains(value.size())) {
                throw new SerializationException(node, "Expected the size of the map to be in range " + size + ".");
            }
            if (node.getType() == Node.Type.UNDEFINED) {
                node.attach();
            }
            node.setValue(ImmutableMap.of());
            for (Map.Entry<String, T> entry : value.entrySet()) {
                node.set(entry.getKey(), entry.getValue(), serializer);
            }
        }

        public <T> MapDeserializer<T> of(Deserializer<T> deserializer) {
            return new MapSerializerImpl<>((Serializer<T>) deserializer, Range.all());
        }

        public <T> MapSerializerImpl<T> of(Serializer<T> serializer) {
            return new MapSerializerImpl<>(serializer, Range.all());
        }

        public MapSerializerImpl<T> size(Range<Integer> size) {
            return new MapSerializerImpl<>(serializer, size);
        }

        public MapSerializerImpl<T> toSerializer() {
            return this;
        }

    }

}
