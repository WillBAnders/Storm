package dev.willbanders.storm.serializer.primitive;

import com.google.common.collect.Range;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.math.BigInteger;
import java.util.function.Function;

/**
 * Deserializes a {@link Node.Type#INTEGER} value into an integer number of type
 * {@link T}. A {@link Range<T>} may be provided to require the value to be
 * contained within a range.
 */
public final class IntegerSerializer<T extends Number & Comparable<T>> implements Serializer<T> {

    public static final IntegerSerializer<Byte> BYTE = new IntegerSerializer<>(BigInteger::byteValueExact, Range.closed(Byte.MIN_VALUE, Byte.MAX_VALUE));
    public static final IntegerSerializer<Short> SHORT = new IntegerSerializer<>(BigInteger::shortValueExact, Range.closed(Short.MIN_VALUE, Short.MAX_VALUE));
    public static final IntegerSerializer<Integer> INTEGER = new IntegerSerializer<>(BigInteger::intValueExact, Range.closed(Integer.MIN_VALUE, Integer.MAX_VALUE));
    public static final IntegerSerializer<Long> LONG = new IntegerSerializer<>(BigInteger::longValueExact, Range.closed(Long.MIN_VALUE, Long.MAX_VALUE));
    public static final IntegerSerializer<BigInteger> BIG_INTEGER = new IntegerSerializer<>(Function.identity(), Range.all());

    private final Function<BigInteger, T> parser;
    private final Range<T> range;

    private IntegerSerializer(Function<BigInteger, T> parser, Range<T> range) {
        this.parser = parser;
        this.range = range;
    }

    @Override
    public T deserialize(Node node) throws SerializationException {
        if (node.getType() != Node.Type.INTEGER) {
            throw new SerializationException(node, "Expected an integer value.");
        }
        try {
            T value = parser.apply((BigInteger) node.getValue());
            if (!range.contains(value)) {
                throw new SerializationException(node, "Expected value to be in range " + range + ".");
            }
            return value;
        } catch (ArithmeticException e) {
            throw new SerializationException(node, "Expected value to be in range " + range + ".");
        }
    }

    @Override
    public void serialize(Node node, T value) throws SerializationException {
        if (value == null) {
            throw new SerializationException(node, "Expected a non-null value.");
        } else if (!range.contains(value)) {
            throw new SerializationException(node, "Expected value to be in range " + range + ".");
        }
        node.attach().setValue(new BigInteger(value.toString()));
    }

    /**
     * Returns a new serializer requiring the value to be contained within the
     * given range.
     */
    public Serializer<T> range(Range<T> range) {
        return new IntegerSerializer<>(parser, range);
    }

}
