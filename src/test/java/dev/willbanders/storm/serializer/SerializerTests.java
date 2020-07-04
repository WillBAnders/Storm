package dev.willbanders.storm.serializer;

import com.google.common.collect.Range;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.primitive.BooleanSerializer;
import dev.willbanders.storm.serializer.primitive.CharacterSerializer;
import dev.willbanders.storm.serializer.primitive.DecimalSerializer;
import dev.willbanders.storm.serializer.primitive.IntegerSerializer;
import dev.willbanders.storm.serializer.primitive.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

class SerializerTests {

    @ParameterizedTest
    @MethodSource
    void testBoolean(String test, Boolean value) {
        testSerializer(BooleanSerializer.INSTANCE, value, value, value != null);
    }

    private static Stream<Arguments> testBoolean() {
        return Stream.of(
                Arguments.of("True", true),
                Arguments.of("False", false),
                Arguments.of("Invalid", null)
        );
    }

    @Nested
    class IntegerTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testInteger")
        void testInteger(String test, BigInteger value) {
            testSerializer(IntegerSerializer.BIG_INTEGER, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testIntegerRange")
        void testIntegerRange(String test, Range<BigInteger> range) {
            Serializer<BigInteger> serializer = IntegerSerializer.BIG_INTEGER.range(range);
            Assertions.assertAll(
                    () -> testSerializer(serializer, BigInteger.ZERO, BigInteger.ZERO, range.contains(BigInteger.ZERO)),
                    () -> testSerializer(serializer, BigInteger.ONE, BigInteger.ONE, range.contains(BigInteger.ONE)),
                    () -> testSerializer(serializer, BigInteger.TEN, BigInteger.TEN, range.contains(BigInteger.TEN))
            );
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testIntegerTypes")
        <T extends Number> void testIntegerTypes(String test, T max, Serializer<T> serializer) {
            BigInteger num = BigInteger.valueOf(max.longValue());
            Assertions.assertAll(
                    () -> testSerializer(serializer, max, num, true),
                    () -> testDeserializer(serializer, num.add(BigInteger.ONE), null, false)
            );
        }

    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Zero", BigInteger.ZERO),
                Arguments.of("Positive", BigInteger.ONE),
                Arguments.of("Negative", BigInteger.ONE.negate()),
                Arguments.of("Above Long Max", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)),
                Arguments.of("Below Long Min", BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE)),
                Arguments.of("Invalid", null)
        );
    }

    private static Stream<Arguments> testIntegerRange() {
        return Stream.of(
                Arguments.of("Open", Range.open(BigInteger.ZERO, BigInteger.TEN)),
                Arguments.of("Closed", Range.closed(BigInteger.ZERO, BigInteger.TEN)),
                Arguments.of("Empty", Range.closedOpen(BigInteger.ZERO, BigInteger.ZERO))
        );
    }

    private static Stream<Arguments> testIntegerTypes() {
        return Stream.of(
                Arguments.of("Byte", Byte.MAX_VALUE, IntegerSerializer.BYTE),
                Arguments.of("Short", Short.MAX_VALUE, IntegerSerializer.SHORT),
                Arguments.of("Integer", Integer.MAX_VALUE, IntegerSerializer.INTEGER),
                Arguments.of("Long", Long.MAX_VALUE, IntegerSerializer.LONG)
        );
    }

    @Nested
    class DecimalTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testDecimal")
        void testDecimal(String test, BigDecimal value) {
            testSerializer(DecimalSerializer.BIG_DECIMAL, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testDecimalRange")
        void testDecimalRange(String test, Range<BigDecimal> range) {
            Serializer<BigDecimal> serializer = DecimalSerializer.BIG_DECIMAL.range(range);
            Assertions.assertAll(
                    () -> testSerializer(serializer, BigDecimal.ZERO, BigDecimal.ZERO, range.contains(BigDecimal.ZERO)),
                    () -> testSerializer(serializer, BigDecimal.ONE, BigDecimal.ONE, range.contains(BigDecimal.ONE)),
                    () -> testSerializer(serializer, BigDecimal.TEN, BigDecimal.TEN, range.contains(BigDecimal.TEN))
            );
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testDecimalTypes")
        <T extends Number> void testDecimalTypes(String test, T value, Serializer<T> serializer) {
            BigDecimal number = new BigDecimal(value.toString());
            //imprecise conversion requires a difference larger than 1 ulp (and some); 2 is used for simplicity.
            BigDecimal outOfRange = number.add(number.ulp().multiply(BigDecimal.valueOf(2)));
            Assertions.assertAll(
                    () -> testSerializer(serializer, value, number, true),
                    () -> testDeserializer(serializer, outOfRange, null, false)
            );
        }

    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Zero", BigDecimal.ZERO),
                Arguments.of("Positive", BigDecimal.ONE),
                Arguments.of("Negative", BigDecimal.ONE.negate()),
                Arguments.of("Above Double Max", BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.ONE)),
                Arguments.of("Below Double Min", BigDecimal.valueOf(Double.MIN_VALUE).divide(BigDecimal.TEN)),
                Arguments.of("Invalid", null)
        );
    }

    private static Stream<Arguments> testDecimalRange() {
        return Stream.of(
                Arguments.of("Open", Range.open(BigDecimal.ZERO, BigDecimal.TEN)),
                Arguments.of("Closed", Range.closed(BigDecimal.ZERO, BigDecimal.TEN)),
                Arguments.of("Empty", Range.closedOpen(BigDecimal.ZERO, BigDecimal.ZERO))
        );
    }

    private static Stream<Arguments> testDecimalTypes() {
        return Stream.of(
                Arguments.of("Float", Float.MAX_VALUE, DecimalSerializer.FLOAT),
                Arguments.of("Double", Double.MAX_VALUE, DecimalSerializer.DOUBLE)
        );
    }

    @Nested
    class CharacterTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testCharacter")
        void testCharacter(String test, Character value) {
            testSerializer(CharacterSerializer.INSTANCE, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testCharacterRegex")
        void testCharacterRegex(String test, Character value, String regex) {
            testSerializer(CharacterSerializer.INSTANCE.matches(regex), value, value, value.toString().matches(regex));
        }

    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Letter", 'c'),
                Arguments.of("Escape", '\''),
                Arguments.of("Invalid", null)
        );
    }

    private static Stream<Arguments> testCharacterRegex() {
        return Stream.of(
                Arguments.of("Letter Valid", 'c', "[A-Za-z]"),
                Arguments.of("Letter Invalid", '\'', "[A-Za-z]"),
                Arguments.of("Escape Valid", '\'', "[bfnrt\'\"\\\\]"),
                Arguments.of("Escape Invalid", 'c', "[bfnrt\'\"\\\\]"),
                Arguments.of("Wildcard", '?', ".*")
        );
    }

    @Nested
    class StringTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testString")
        void testString(String test, String value) {
            testSerializer(StringSerializer.INSTANCE, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testStringRegex")
        void testStringRegex(String test, String value, String regex) {
            testSerializer(StringSerializer.INSTANCE.matches(regex), value, value, value.matches(regex));
        }

    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", ""),
                Arguments.of("Single Character", "c"),
                Arguments.of("Escapes", "\"Hello, World!\""),
                Arguments.of("Invalid", null)
        );
    }

    private static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Email Valid", "x@y.z", "\\w+@\\w+\\.\\w+"),
                Arguments.of("Email Invalid", "xyz", "\\w+@\\w+\\.\\w+"),
                Arguments.of("Escape Valid", "\\\"", "\\\\[bfnrt\'\"\\\\]"),
                Arguments.of("Escape Invalid", "\\c", "\\\\[bfnrt\'\"\\\\]"),
                Arguments.of("Wildcard", "???", ".*")
        );
    }

    /**
     * Tests that deserializing the given value equals the expected value if
     * success is true, else asserts a {@link SerializationException} is thrown.
     */
    private void testDeserializer(Deserializer<?> deserializer, Object value, Object expected, boolean success) {
        Node node = Node.root().attach();
        node.setValue(value);
        test(() -> Assertions.assertEquals(expected, node.get(deserializer)), success);
    }

    /**
     * Tests that serializing the given value equals the expect value and that
     * deserializing the expected value equals the given value if success is
     * true, else asserts a {@link SerializationException} is thrown.
     */
    private void testSerializer(Serializer<?> serializer, Object value, Object expected, boolean success) {
        Assertions.assertAll(
                () -> testDeserializer(serializer, expected, value, success),
                () -> test(() -> {
                    Node node = Node.root();
                    node.set(value, (Serializer<Object>) serializer);
                    Assertions.assertEquals(expected, node.getValue());
                }, success)
        );
    }

    /**
     * Executes the runnable. If success is false, assert that the runnable
     * throws a {@link SerializationException}.
     */
    private void test(Runnable runnable, boolean success) {
        if (success) {
            runnable.run();
        } else {
            Assertions.assertThrows(SerializationException.class, runnable::run);
        }
    }

}
