package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Range;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Deserializes a {@link Node.Type#DECIMAL} value into a decimal number of type
 * {@link T}. A {@link Range<T>} may be provided to require the value to be
 * contained within a range.
 */
public final class DecimalSerializer<T extends Number & Comparable<T>> implements Serializer<T> {

    public static final DecimalSerializer<Float> FLOAT = new DecimalSerializer<>(BigDecimal::floatValue, Range.closed(-Float.MAX_VALUE, Float.MAX_VALUE));
    public static final DecimalSerializer<Double> DOUBLE = new DecimalSerializer<>(BigDecimal::doubleValue, Range.closed(-Double.MAX_VALUE, Double.MAX_VALUE));
    public static final DecimalSerializer<BigDecimal> BIG_DECIMAL = new DecimalSerializer<>(Function.identity(), Range.all());

    private final Function<BigDecimal, T> parser;
    private final Range<T> range;

    private DecimalSerializer(Function<BigDecimal, T> parser, Range<T> range) {
        this.parser = parser;
        this.range = range;
    }

    @Override
    public T deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.DECIMAL) {
            throw new SerializationException(node, "Expected a decimal value.");
        }
        T value = parser.apply((BigDecimal) node.getValue());
        if (!range.contains(value)) {
            throw new SerializationException(node, "Expected value to be in range " + range + ".");
        }
        return value;
    }

    @Override
    public void serialize(Node node, T value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (!range.contains(value)) {
            throw new SerializationException(node, "Expected value to be in range " + range + ".");
        }
        if (node.getType() == Node.Type.UNDEFINED) {
            node.attach();
        }
        node.attach().setValue(new BigDecimal(value.toString()));
    }

    /**
     * Returns a new serializer requiring the value to be contained within the
     * given range.
     */
    public Serializer<T> range(Range<T> range) {
        return new DecimalSerializer<>(parser, range);
    }

}
