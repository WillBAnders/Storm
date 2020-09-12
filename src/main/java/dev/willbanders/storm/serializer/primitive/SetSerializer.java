package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serializes a {@link Node.Type#ARRAY} value into a {@link Set} of {@link T}.
 * Values in the array must be distinct. A {@link Range} may be provided to
 * require the size of the set to be contained within a range.
 */
public final class SetSerializer<T> implements Serializer<Set<T>> {

    public static final SetSerializer<Object> INSTANCE = new SetSerializer<>(null, Range.all());

    private final Serializer<T> serializer;
    private final Serializer<List<T>> delegate;

    private SetSerializer(Serializer<T> serializer, Range<Integer> size) {
        this.serializer = serializer;
        this.delegate = ListSerializer.INSTANCE.of(serializer).size(size);
    }

    @Override
    public Set<T> deserialize(Node node) throws SerializationException {
        List<T> list = node.get(delegate);
        Set<T> value = Sets.newLinkedHashSet(list);
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
    public void reserialize(Node node, Set<T> value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        }
        node.set(Lists.newArrayList(value), delegate);
    }

    /**
     * Returns a new SetSerializer that delegates to the given serializer for
     * serializing elements.
     */
    public <T> SetSerializer<T> of(Serializer<T> serializer) {
        return new SetSerializer<>(serializer, Range.all());
    }

    /**
     * Returns a new serializer requiring the size of the set to be contained
     * within the given range.
     */
    public SetSerializer<T> size(Range<Integer> size) {
        return new SetSerializer<>(serializer, size);
    }

}
