package dev.willbanders.storm.serializer.primitive;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dev.willbanders.storm.Storm;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassSerializer<T> implements Serializer<T> {

    public static final ClassSerializer<Object> INSTANCE = new ClassSerializer<>(Object.class);

    private final Class<T> clazz;

    private ClassSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T deserialize(Node node) throws SerializationException {
        return node.get(clazz);
    }

    @Override
    public void reserialize(Node node, T value) throws SerializationException {
        node.set(value);
    }

    /**
     * Returns a new serializer for instances of the given class. The class must
     * be annotated with {@link Storm.Serialized}, and uses either method-based
     * or field-based serialization as defined below:
     *
     * Method-based serialization is used if a {@code deserialize(Node)} method
     * exists. This method must be static and returns the given class. If a
     * {@code reserialize(Node, GivenClass)} method exists, reserialization is
     * supported. This method must also be static and returns void.
     *
     * Field-based serialization is used if a {@code deserialize(Node)} method
     * does not exist. The class must have a constructor taking zero arguments
     * (which uses reflection to initialize fields) or a constructor taking the
     * types of each field in the order they are defined in the given class.
     * Reserialization is always supported through reflection on each field.
     */
    public <T> Serializer<T> of(Class<T> clazz) {
        Preconditions.checkArgument(clazz.getDeclaredAnnotation(Storm.Serialized.class) != null, "Missing @Storm.Serialized annotation for class %s.", clazz.getName());
        Method deserialize = getMethod(clazz, clazz, "deserialize", Node.class).orElse(null);
        if (deserialize == null) {
            Map<String, Field> fields = Arrays.stream(clazz.getDeclaredFields())
                    .peek(f -> f.setAccessible(true))
                    .collect(Collectors.toMap(Field::getName, f -> f));
            Constructor<T> constructor = getConstructor(clazz)
                    .orElseGet(() -> getConstructor(clazz, fields.values().stream()
                            .map(Field::getType)
                            .toArray(Class[]::new))
                            .orElseThrow(() -> new IllegalArgumentException("No applicable constructor for field serialization in class " + clazz.getName() + ".")));
            return new Fields<>(clazz, fields, constructor);
        } else {
            Optional<Method> reserialize = getMethod(clazz, void.class, "reserialize", Node.class, clazz);
            return new Methods<>(clazz, deserialize, reserialize);
        }
    }

    private static <T> Optional<Constructor<T>> getConstructor(Class<T> clazz, Class<?>... parameters) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(parameters);
            constructor.setAccessible(true);
            return Optional.of(constructor);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    private static Optional<Method> getMethod(Class<?> clazz, Class<?> returns, String name, Class<?>... parameters) {
        try {
            Method method = clazz.getDeclaredMethod(name, parameters);
            Preconditions.checkArgument(Modifier.isStatic(method.getModifiers()), "Serialization methods must be static for method %s.", method);
            Preconditions.checkArgument(method.getReturnType().equals(returns), "Return type must be %s for method %s.", returns.getName(), method);
            method.setAccessible(true);
            return Optional.of(method);
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    private static final class Fields<T> extends ClassSerializer<T> {

        private final Map<String, Field> fields;
        private final Constructor<T> constructor;

        private Fields(Class<T> clazz, Map<String, Field> fields, Constructor<T> constructor) {
            super(clazz);
            this.fields = fields;
            this.constructor = constructor;
        }

        @Override
        public T deserialize(Node node) throws SerializationException {
            if (node.getType() != Node.Type.OBJECT) {
                throw new SerializationException(node, "Expected an object value.");
            } else if (!fields.keySet().equals(node.getMap().keySet())) {
                Set<String> expected = Sets.difference(fields.keySet(), node.getMap().keySet());
                Set<String> unexpected = Sets.difference(node.getMap().keySet(), fields.keySet());
                throw new SerializationException(node, "Expected properties " + expected + " and not " + unexpected + ".");
            }
            Map<String, Object> values = Maps.newHashMap();
            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                values.put(entry.getKey(), node.get(entry.getKey(), entry.getValue().getType()));
            }
            try {
                if (constructor.getParameterCount() == 0) {
                    T instance = constructor.newInstance();
                    for (Field field : fields.values()) {
                        field.set(instance, values.get(field.getName()));
                    }
                    return instance;
                } else {
                    return constructor.newInstance(fields.values().stream()
                            .map(f -> values.get(f.getName()))
                            .toArray(Object[]::new));
                }
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                throw new SerializationException(node, e.getMessage());
            }
        }

        @Override
        public void reserialize(Node node, T value) throws SerializationException {
            if (value == null) {
                throw new SerializationException(node, "Expected a non-null value.");
            }
            try {
                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    node.set(entry.getKey(), entry.getValue().get(value));
                }
            } catch (IllegalAccessException e) {
                throw new SerializationException(node, e.getMessage());
            }
        }

    }

    private static final class Methods<T> extends ClassSerializer<T> {

        private final Method deserialize;
        private final Optional<Method> reserialize;

        private Methods(Class<T> clazz, Method deserialize, Optional<Method> reserialize) {
            super(clazz);
            this.deserialize = deserialize;
            this.reserialize = reserialize;
        }

        @Override
        public T deserialize(Node node) throws SerializationException {
            try {
                return (T) deserialize.invoke(null, node);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new SerializationException(node, e.getMessage());
            }
        }

        @Override
        public void reserialize(Node node, T value) throws SerializationException {
            if (!reserialize.isPresent()) {
                throw new SerializationException(node, "Reserialization is not supported for this serializer.");
            }
            try {
                reserialize.get().invoke(null, node, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new SerializationException(node, e.getMessage());
            }
        }

    }

}
