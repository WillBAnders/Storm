package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Lists;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Serializes a {@link Node.Type#ARRAY} value into a {@link List} of {@link T}.
 * Unlike {@link ListSerializer}, this serializer supports non-homogeneous lists
 * as in {@code [true, 1, "string"]}.
 */
public final class TupleSerializer<T> implements Serializer<List<T>> {

    public static final TupleSerializer<Object> INSTANCE = new TupleSerializer<>(Lists.newArrayList());

    private final List<Serializer<? extends T>> serializers;

    private TupleSerializer(List<Serializer<? extends T>> serializers) {
        this.serializers = serializers;
    }

    @Override
    public List<T> deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.ARRAY) {
            throw new SerializationException(node, "Expected a value of type array.");
        } else if (node.getList().size() != serializers.size()) {
            throw new SerializationException(node, "Expected an array with size " + serializers.size() + ".");
        }
        return IntStream.range(0, serializers.size())
                .mapToObj(i -> node.resolve(i).get(serializers.get(i)))
                .collect(Collectors.toList());
    }

    @Override
    public void reserialize(Node node, List<T> value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (value.size() != serializers.size()) {
            throw new SerializationException(node, "Expected a list with size " + serializers.size() + ".");
        }
        if (node.getType() == Node.Type.ARRAY) {
            for (int i = node.getList().size(); i > value.size(); i--) {
                node.resolve(i - 1).detach();
            }
        } else {
            node.attach().setValue(Lists.newArrayList());
        }
        for (int i = 0; i < value.size(); i++) {
            try {
                node.resolve(i).set(value.get(i), (Serializer<T>) serializers.get(i));
            } catch (ClassCastException e) {
                throw new SerializationException(node.resolve(i), e.getMessage());
            }
        }
    }

    /**
     * Returns a new serializer that delegates serialization of each tuple
     * element to the given serializers at the corresponding index.
     */
    public <T> TupleSerializer<T> of(List<Serializer<? extends T>> serializers) {
        return new TupleSerializer<>(serializers);
    }

}
