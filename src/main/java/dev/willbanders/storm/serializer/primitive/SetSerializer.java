package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Lists;
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

public final class SetSerializer {

    public static final SetSerializerImpl<Object> INSTANCE = new SetSerializerImpl<>(null, Range.all());

    public interface SetDeserializer<T> extends Deserializer<Set<T>> {

        SetDeserializer<T> size(Range<Integer> size);

        SetSerializerImpl<T> toSerializer();

    }

    public static final class SetSerializerImpl<T> implements SetDeserializer<T>, Serializer<Set<T>> {

        private final Serializer<T> serializer;
        private final Serializer<List<T>> delegate;

        private SetSerializerImpl(Serializer<T> serializer, Range<Integer> size) {
            this.serializer = serializer;
            this.delegate = ListSerializer.INSTANCE.of(serializer).size(size);
        }

        @Override
        public Set<T> deserialize(Node node) throws SerializationException {
            List<T> list = node.get(delegate);
            Set<T> value = Sets.newHashSet(list);
            if (value.size() != list.size()) {
                Map<T, Integer> map = Maps.newHashMap();
                for (int i = 0; i < list.size(); i++) {
                    if (map.containsKey(list.get(i))) {
                        throw new SerializationException(node, "Expected set to contain unique elements, " +
                                "found duplicates at indices " + map.get(list.get(i)) + " and " + i + ".");
                    }
                    map.put(list.get(i), i);
                }
            }
            return value;
        }

        @Override
        public void serialize(Node node, Set<T> value) throws SerializationException {
            if (value == null) {
                throw new SerializationException(node, "Expected a non-null value.");
            }
            node.set(Lists.newArrayList(value), delegate);
        }

        public <T> SetDeserializer<T> of(Deserializer<T> deserializer) {
            return new SetSerializerImpl<>((Serializer<T>) serializer, Range.all());
        }

        public <T> SetSerializerImpl<T> of(Serializer<T> serializer) {
            return new SetSerializerImpl<>(serializer, Range.all());
        }

        @Override
        public SetSerializerImpl<T> size(Range<Integer> size) {
            return new SetSerializerImpl<>(serializer, size);
        }

        @Override
        public SetSerializerImpl<T> toSerializer() {
            return this;
        }

    }

}
