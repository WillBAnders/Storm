package dev.willbanders.storm.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import dev.willbanders.storm.serializer.Serializer;

import java.util.Map;

public class Scope {

    private final Map<Class<?>, Serializer<?>> serializers = Maps.newHashMap();

    public <T> Serializer<T> get(Class<T> clazz) {
        Preconditions.checkArgument(serializers.containsKey(clazz), "No serializer is registered for %s.", clazz);
        return (Serializer<T>) serializers.get(clazz);
    }

    public <T> void register(Class<T> clazz, Serializer<T> serializer) {
        Preconditions.checkState(!serializers.containsKey(clazz), "A serializer is already registered for %s.", clazz);
        serializers.put(clazz, serializer);
    }

}
