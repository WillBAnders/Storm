package dev.willbanders.storm.serializer.primitive;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.Deserializer;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

public final class NullableSerializer {

    public static final NullableSerializerImpl<Object> INSTANCE = new NullableSerializerImpl<>(null, null, false);

    public interface NullableDeserializer<T> extends Deserializer<T> {

        NullableDeserializer<T> def(T def);

        NullableSerializerImpl<T> toSerializer(boolean convertDef);

    }

    public static class NullableSerializerImpl<T> implements NullableDeserializer<T>, Serializer<T> {

        private final Serializer<T> serializer;
        private final T def;
        private final boolean convertDef;

        private NullableSerializerImpl(Serializer<T> serializer, T def, boolean convertDef) {
            this.serializer = serializer;
            this.def = def;
            this.convertDef = convertDef;
        }

        @Override
        public T deserialize(Node node) throws SerializationException {
            if (node.getType() == Node.Type.NULL) {
                return def;
            }
            return node.get(serializer);
        }

        @Override
        public void serialize(Node node, T value) throws SerializationException {
            if (value == null && def != null) {
                throw new SerializationException(node, "Expected a non-null value.");
            }
            if (value == null || convertDef && value.equals(def)) {
                node.attach().setValue(null);
            } else {
                node.set(value, serializer);
            }
        }

        public <T> NullableDeserializer<T> of(Deserializer<T> deserializer) {
            return new NullableSerializerImpl<>((Serializer<T>) deserializer, null, false);
        }

        public <T> NullableSerializerImpl<T> of(Serializer<T> serializer) {
            return new NullableSerializerImpl<>(serializer, null, false);
        }

        @Override
        public NullableDeserializer<T> def(T def) {
            return new NullableSerializerImpl<>(serializer, def, false);
        }

        @Override
        public NullableSerializerImpl<T> toSerializer(boolean convertDefault) {
            return new NullableSerializerImpl<>(serializer, def, convertDefault);
        }

    }

}
