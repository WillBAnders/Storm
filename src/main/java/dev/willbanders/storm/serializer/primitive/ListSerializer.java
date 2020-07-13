package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.Deserializer;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.List;

public final class ListSerializer {

    public static final ListSerializerImpl<Object> INSTANCE = new ListSerializerImpl<>(null, null);

    public interface ListDeserializer<T> extends Deserializer<List<T>> {

        ListDeserializer<T> size(Range<Integer> size);

        ListSerializerImpl<T> toSerializer();

    }

    public static final class ListSerializerImpl<T> implements ListDeserializer<T>, Serializer<List<T>> {

        private final Serializer<T> serializer;
        private final Range<Integer> size;

        private ListSerializerImpl(Serializer<T> serializer, Range<Integer> size) {
            this.serializer = serializer;
            this.size = size;
        }

        @Override
        public List<T> deserialize(Node node) throws SerializationException {
            if (node.getType() != Node.Type.ARRAY) {
                throw new SerializationException(node, "Expected a value of type array.");
            } else if (!size.contains(node.getList().size())) {
                throw new SerializationException(node, "Expected the size of the list to be in range " + size + ".");
            }
            return Lists.newArrayList(Lists.transform(node.getList(), serializer::deserialize));
        }

        @Override
        public void serialize(Node node, List<T> value) throws SerializationException {
            if (value == null) {
                throw new SerializationException(node, "Expected a non-null value.");
            } else if (!size.contains(value.size())) {
                throw new SerializationException(node, "Expected the size of the list to be in range " + size + ".");
            }
            if (node.getType() == Node.Type.UNDEFINED) {
                node.attach();
            }
            node.setValue(ImmutableList.of());
            for (int i = 0; i < value.size(); i++) {
                node.resolve(i).set(value.get(i), serializer);
            }
        }

        public <T> ListDeserializer<T> of(Deserializer<T> deserializer) {
            return new ListSerializerImpl<>((Serializer<T>) serializer, Range.all());
        }

        public <T> ListSerializerImpl<T> of(Serializer<T> serializer) {
            return new ListSerializerImpl<>(serializer, Range.all());
        }

        public ListSerializerImpl<T> size(Range<Integer> size) {
            return new ListSerializerImpl<>(serializer, size);
        }

        @Override
        public ListSerializerImpl<T> toSerializer() {
            return this;
        }

    }

}
