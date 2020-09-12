package dev.willbanders.storm;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.storm.StormGenerator;
import dev.willbanders.storm.format.storm.StormParser;
import dev.willbanders.storm.serializer.primitive.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Storm {

    public static final AnySerializer ANY = AnySerializer.INSTANCE;
    public static final NullableSerializer<Object> ANY_NULLABLE = NullableSerializer.INSTANCE;
    public static final OptionalSerializer<Object> ANY_OPTIONAL = OptionalSerializer.INSTANCE;
    public static final BooleanSerializer BOOLEAN = BooleanSerializer.INSTANCE;
    public static final IntegerSerializer<Byte> BYTE = IntegerSerializer.BYTE;
    public static final IntegerSerializer<Short> SHORT = IntegerSerializer.SHORT;
    public static final IntegerSerializer<Integer> INTEGER = IntegerSerializer.INTEGER;
    public static final IntegerSerializer<Long> LONG = IntegerSerializer.LONG;
    public static final IntegerSerializer<BigInteger> BIG_INTEGER = IntegerSerializer.BIG_INTEGER;
    public static final DecimalSerializer<Float> FLOAT = DecimalSerializer.FLOAT;
    public static final DecimalSerializer<Double> DOUBLE = DecimalSerializer.DOUBLE;
    public static final DecimalSerializer<BigDecimal> BIG_DECIMAL = DecimalSerializer.BIG_DECIMAL;
    public static final CharacterSerializer CHARACTER = CharacterSerializer.INSTANCE;
    public static final StringSerializer STRING = StringSerializer.INSTANCE;
    public static final ListSerializer<Object> LIST = ListSerializer.INSTANCE;
    public static final SetSerializer<Object> SET = SetSerializer.INSTANCE;
    public static final MapSerializer<Object> MAP = MapSerializer.INSTANCE;
    public static final ObjectSerializer<?> OBJECT = ObjectSerializer.INSTANCE;
    public static final EnumSerializer<?> ENUM = EnumSerializer.INSTANCE;
    public static final TupleSerializer<?> TUPLE = TupleSerializer.INSTANCE;
    public static final UnionSerializer<?> UNION = UnionSerializer.INSTANCE;

    public static Node deserialize(String input) {
        return StormParser.parse(input);
    }

    public static String reserialize(Node node) {
        StringWriter writer = new StringWriter();
        StormGenerator.generate(node, new PrintWriter(writer));
        return writer.toString();
    }

}
