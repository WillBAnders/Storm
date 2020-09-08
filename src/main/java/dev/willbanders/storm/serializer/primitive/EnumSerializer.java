package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Sets;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Set;

/**
 * Serializes an {@link Enum} constant to its name. Deserialization is currently
 * case-insensitive, but this may change in the future.
 */
public final class EnumSerializer<T extends Enum<T>> implements Serializer<T> {

    public static final EnumSerializer<?> INSTANCE = new EnumSerializer<>(null);

    private final Set<T> constants;

    private EnumSerializer(Set<T> constants) {
        this.constants = constants;
    }

    @Override
    public T deserialize(Node node) throws SerializationException {
        String value = node.get(StringSerializer.INSTANCE);
        return constants.stream().filter(c -> c.name().equalsIgnoreCase(value)).findFirst()
                .orElseThrow(() -> new SerializationException(node, "Expected the value to be one of " + constants + "."));
    }

    @Override
    public void reserialize(Node node, T value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        }
        node.attach().setValue(value.name());
    }

    /**
     * Returns a new serializer for the given enum class.
     */
    public <T extends Enum<T>> EnumSerializer<T> of(Class<T> clazz) {
        return new EnumSerializer<>(Sets.newHashSet(clazz.getEnumConstants()));
    }

}
