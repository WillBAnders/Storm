package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.List;

public final class ListSerializer<T> implements Serializer<List<T>> {

    public static final ListSerializer<Object> INSTANCE = new ListSerializer<>(null, null);

    private final Serializer<T> serializer;
    private final Range<Integer> size;

    private ListSerializer(Serializer<T> serializer, Range<Integer> size) {
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

    public <T> ListSerializer<T> of(Serializer<T> serializer) {
        return new ListSerializer<>(serializer, Range.all());
    }

    public ListSerializer<T> size(Range<Integer> size) {
        return new ListSerializer<>(serializer, size);
    }

}
