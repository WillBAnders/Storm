package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.Deserializer;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;
import dev.willbanders.storm.serializer.combinator.OptionalDeserializer;

import java.util.Optional;

public final class OptionalSerializer<T> {

    public static final OptionalSerializerImpl<Object> INSTANCE = new OptionalSerializerImpl<>(null);

    public interface OptionalDeserializer<T> extends Deserializer<Optional<T>> {

        OptionalDefaultDeserializer<T> def(T def);

    }

    public static class OptionalSerializerImpl<T> implements OptionalDeserializer<T>, Serializer<Optional<T>> {

        private final Serializer<T> serializer;

        private OptionalSerializerImpl(Serializer<T> serializer) {
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
        public void serialize(Node node, Optional<T> value) throws SerializationException {
            if (value.isPresent()) {
                node.set(value.get(), serializer);
            } else if (node.getType() != Node.Type.UNDEFINED) {
                node.detach();
            }
        }

        public <T> OptionalDeserializer<T> of(Deserializer<T> deserializer) {
            return new OptionalSerializerImpl<>((Serializer<T>) deserializer);
        }

        public <T> OptionalSerializerImpl<T> of(Serializer<T> serializer) {
            return new OptionalSerializerImpl<>(serializer);
        }

        @Override
        public OptionalDefaultDeserializer<T> def(T def) {
            return new OptionalDefaultSerializer<>(serializer, def, false);
        }

    }

    public interface OptionalDefaultDeserializer<T> extends Deserializer<T> {

        OptionalDefaultSerializer<T> toSerializer(boolean convertDefault);

    }

    public static class OptionalDefaultSerializer<T> implements OptionalDefaultDeserializer<T>, Serializer<T> {

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
        public void serialize(Node node, T value) throws SerializationException {
            if (value == null) {
                throw new SerializationException(node, "Expected a non-null value.");
            } else if (convertDef && value.equals(def)) {
                if (node.getType() != Node.Type.UNDEFINED) {
                    node.detach();
                }
            } else {
                node.set(value, serializer);
            }
        }

        @Override
        public OptionalDefaultSerializer<T> toSerializer(boolean convertDefault) {
            return new OptionalDefaultSerializer<>(serializer, def, convertDefault);
        }

    }

}
