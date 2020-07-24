package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Objects;
import java.util.Optional;

public final class OptionalSerializer<T> implements Serializer<Optional<T>> {

    public static final OptionalSerializer<Object> INSTANCE = new OptionalSerializer<>(null);

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

    public <T> OptionalSerializer<T> of(Serializer<T> serializer) {
        return new OptionalSerializer<>(serializer);
    }

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

        public OptionalDefaultSerializer<T> convertDef(boolean convertDef) {
            return new OptionalDefaultSerializer<>(serializer, def, convertDef);
        }

    }

}
