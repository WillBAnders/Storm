package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Objects;
import java.util.Optional;

/**
 * Serializes values which may be {@link Node.Type#UNDEFINED}, delegating to
 * another serializer for other types. This serializer may return an {@link
 * Optional} or define another default value to be used instead.
 *
 * The default behavior for reserialization is to keep the default value as-is.
 * For example, an optional serializer with a default of {@code 0} will
 * serialize {@code 0} to the value itself, not undefined. This behavior can be
 * changed by setting {@link OptionalDefaultSerializer#convertDef(boolean)} to
 * {@code true}.
 */
public final class OptionalSerializer<T> implements Serializer<Optional<T>> {

    public static final OptionalSerializer<Object> INSTANCE = new OptionalSerializer<>(AnySerializer.INSTANCE);

    private final Serializer<T> serializer;

    private OptionalSerializer(Serializer<T> serializer) {
        this.serializer = serializer;
    }

    @Override
    public Optional<T> deserialize(Node node) throws SerializationException {
        if (node.getType() == Node.Type.UNDEFINED) {
            return Optional.empty();
        }
        return Optional.ofNullable(node.get(serializer));
    }

    @Override
    public void reserialize(Node node, Optional<T> value) throws SerializationException {
        if (value.isPresent()) {
            node.set(value.get(), serializer);
        } else {
            node.detach();
        }
    }

    /**
     * Returns a new serializer that delegates to the given serializer if the
     * node value is not {@link Node.Type#UNDEFINED}.
     */
    public <T> OptionalSerializer<T> of(Serializer<T> serializer) {
        return new OptionalSerializer<>(serializer);
    }

    /**
     * Returns a new serializer with a default value for when the node is {@link
     * Node.Type#UNDEFINED}. By default, this will be reserialized to the
     * corresponding config value and not converted to {@code undefined}.
     *
     * @see OptionalDefaultSerializer#convertDef(boolean)
     */
    public OptionalDefaultSerializer<T> def(T def) {
        return new OptionalDefaultSerializer<>(serializer, def, false);
    }

    public static class OptionalDefaultSerializer<T> implements Serializer<T> {

        private final Serializer<T> serializer;
        private final T def;
        private final boolean convertDef;

        private OptionalDefaultSerializer(Serializer<T> serializer, T def, boolean convertDef) {
            this.serializer = serializer;
            this.def = def;
            this.convertDef = convertDef;
        }

        @Override
        public T deserialize(Node node) throws SerializationException {
            if (node.getType() == Node.Type.UNDEFINED) {
                return def;
            }
            return node.get(serializer);
        }

        @Override
        public void reserialize(Node node, T value) throws SerializationException {
            if (value == null && def != null) {
                throw new SerializationException(node, "Expected a non-null value.");
            } else if (convertDef && Objects.equals(value, def)) {
                node.detach();
            } else {
                node.set(value, serializer);
            }
        }

        /**
         * Returns a new serializer that reserializes the default value to
         * {@code undefined} if {@code true} else to the corresponding config
         * value. The default behavior is {@code false}.
         */
        public OptionalDefaultSerializer<T> convertDef(boolean convertDef) {
            return new OptionalDefaultSerializer<>(serializer, def, convertDef);
        }

    }

}
