package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.List;

/**
 * Serializes a {@link Node.Type#ARRAY} value into a {@link List} of {@link T}.
 * A {@link Range} may be provided to require the size of the list to be
 * contained within a range.
 *
 * @see TupleSerializer for non-homogeneous lists
 */
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
    public void reserialize(Node node, List<T> value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (!size.contains(value.size())) {
            throw new SerializationException(node, "Expected the size of the list to be in range " + size + ".");
        }
        if (node.getType() == Node.Type.ARRAY) {
            for (int i = node.getList().size(); i > value.size(); i--) {
                node.resolve(i - 1).detach();
            }
        } else {
            node.attach().setValue(Lists.newArrayList());
        }
        for (int i = 0; i < value.size(); i++) {
            node.resolve(i).set(value.get(i), serializer);
        }
    }

    /**
     * Returns a new ListSerializer that delegates to the given serializer for
     * serializing elements.
     */
    public <T> ListSerializer<T> of(Serializer<T> serializer) {
        return new ListSerializer<>(serializer, Range.all());
    }

    /**
     * Returns a new serializer requiring the size of the list to be contained
     * within the given range.
     */
    public ListSerializer<T> size(Range<Integer> size) {
        return new ListSerializer<>(serializer, size);
    }

}
