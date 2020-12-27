package dev.willbanders.storm;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.config.Scope;
import dev.willbanders.storm.format.storm.StormGenerator;
import dev.willbanders.storm.format.storm.StormParser;
import dev.willbanders.storm.serializer.primitive.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class Storm {

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Serialized {}

    public static final Scope SCOPE = new Scope();
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
    public static final ClassSerializer<?> CLASS = ClassSerializer.INSTANCE;

    public static Node deserialize(String input) {
        return StormParser.parse(input);
    }

    public static String reserialize(Node node) {
        StringWriter writer = new StringWriter();
        StormGenerator.generate(node, new PrintWriter(writer));
        return writer.toString();
    }

    static {
        SCOPE.register(boolean.class, BOOLEAN);
        SCOPE.register(byte.class, BYTE);
        SCOPE.register(short.class, SHORT);
        SCOPE.register(int.class, INTEGER);
        SCOPE.register(long.class, LONG);
        SCOPE.register(float.class, FLOAT);
        SCOPE.register(double.class, DOUBLE);
        SCOPE.register(char.class, CHARACTER);
        SCOPE.register(Object.class, ANY);
        SCOPE.register(Boolean.class, BOOLEAN);
        SCOPE.register(Byte.class, BYTE);
        SCOPE.register(Short.class, SHORT);
        SCOPE.register(Integer.class, INTEGER);
        SCOPE.register(Long.class, LONG);
        SCOPE.register(BigInteger.class, BIG_INTEGER);
        SCOPE.register(Float.class, FLOAT);
        SCOPE.register(Double.class, DOUBLE);
        SCOPE.register(BigDecimal.class, BIG_DECIMAL);
        SCOPE.register(Character.class, CHARACTER);
        SCOPE.register(String.class, STRING);
    }

}
