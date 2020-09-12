package dev.willbanders.storm.serializer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import dev.willbanders.storm.Storm;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.serializer.primitive.EnumSerializer;
import dev.willbanders.storm.serializer.primitive.ListSerializer;
import dev.willbanders.storm.serializer.primitive.MapSerializer;
import dev.willbanders.storm.serializer.primitive.NullableSerializer;
import dev.willbanders.storm.serializer.primitive.OptionalSerializer;
import dev.willbanders.storm.serializer.primitive.SetSerializer;
import dev.willbanders.storm.serializer.primitive.TupleSerializer;
import dev.willbanders.storm.serializer.primitive.UnionSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

class SerializerTests {

    @Nested
    class AnyTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testAny")
        void testAny(String test, Object value) {
            testSerializer(Storm.ANY, value, value, value != null);
        }

        @Test
        void testAnyUndefined() {
            Assertions.assertThrows(SerializationException.class, () -> Node.root().get(Storm.ANY));
        }

    }

    private static Stream<Arguments> testAny() {
        return Stream.of(
                Arguments.of("Null", null),
                Arguments.of("Boolean", true),
                Arguments.of("Integer", BigInteger.ONE),
                Arguments.of("String", "string"),
                Arguments.of("Array", ImmutableList.of('a', 'b', 'c'))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBoolean(String test, Boolean value) {
        testSerializer(Storm.BOOLEAN, value, value, value != null);
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
            testSerializer(Storm.BIG_INTEGER, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testIntegerRange")
        void testIntegerRange(String test, Range<BigInteger> range) {
            Serializer<BigInteger> serializer = Storm.BIG_INTEGER.range(range);
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
                Arguments.of("Byte", Byte.MAX_VALUE, Storm.BYTE),
                Arguments.of("Short", Short.MAX_VALUE, Storm.SHORT),
                Arguments.of("Integer", Integer.MAX_VALUE, Storm.INTEGER),
                Arguments.of("Long", Long.MAX_VALUE, Storm.LONG)
        );
    }

    @Nested
    class DecimalTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testDecimal")
        void testDecimal(String test, BigDecimal value) {
            testSerializer(Storm.BIG_DECIMAL, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testDecimalRange")
        void testDecimalRange(String test, Range<BigDecimal> range) {
            Serializer<BigDecimal> serializer = Storm.BIG_DECIMAL.range(range);
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
                Arguments.of("Float", Float.MAX_VALUE, Storm.FLOAT),
                Arguments.of("Double", Double.MAX_VALUE, Storm.DOUBLE)
        );
    }

    @Nested
    class CharacterTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testCharacter")
        void testCharacter(String test, Character value) {
            testSerializer(Storm.CHARACTER, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testCharacterRegex")
        void testCharacterRegex(String test, Character value, String regex) {
            testSerializer(Storm.CHARACTER.matches(regex), value, value, value.toString().matches(regex));
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
            testSerializer(Storm.STRING, value, value, value != null);
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testStringRegex")
        void testStringRegex(String test, String value, String regex) {
            testSerializer(Storm.STRING.matches(regex), value, value, value.matches(regex));
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

    @Nested
    class ListTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testList")
        void testList(String test, Serializer<?> serializer, Object value) {
            testSerializer(Storm.LIST.of(serializer), value, value, value != null);
        }

        @Test
        void testListInvalidElement() {
            List<Object> list = ImmutableList.of("first", false, "third");
            testDeserializer(Storm.LIST.of(Storm.STRING), list, null, false);
        }

        @Test
        void testListSize() {
            List<Object> list = ImmutableList.of("first", "second", "third");
            ListSerializer<String> base = Storm.LIST.of(Storm.STRING);
            Assertions.assertAll(
                    () -> testSerializer(base.size(Range.atLeast(0)), list, list, true),
                    () -> testSerializer(base.size(Range.atLeast(5)), list, list, false),
                    () -> testSerializer(base.size(Range.closed(1, 5)), list, list, true)
            );
        }

    }

    private static Stream<Arguments> testList() {
        return Stream.of(
                Arguments.of("Empty", Storm.BOOLEAN, ImmutableList.of()),
                Arguments.of("Integer Element", Storm.BIG_INTEGER, ImmutableList.of(BigInteger.ONE)),
                Arguments.of("Character Elements", Storm.CHARACTER, ImmutableList.of('a', 'b', 'c')),
                Arguments.of("Invalid Type", Storm.STRING, null)
        );
    }

    @Nested
    class SetTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testSet")
        void testSet(String test, Serializer<?> serializer, Set value) {
            Object expected = value != null ? ImmutableList.copyOf(value) : null;
            testSerializer(Storm.SET.of(serializer), value, expected, value != null);
        }

        @Test
        void testSetDuplicateElement() {
            List<Object> list = ImmutableList.of("element", "element");
            testDeserializer(Storm.SET.of(Storm.STRING), list, null, false);
        }

        @Test
        void testSetInvalidElement() {
            List<Object> list = ImmutableList.of("first", false, "third");
            testDeserializer(Storm.SET.of(Storm.STRING), list, null, false);
        }

        @Test
        void testSetSize() {
            Set<Object> set = ImmutableSet.of("first", "second", "third");
            List<Object> list = ImmutableList.copyOf(set);
            SetSerializer<String> base = Storm.SET.of(Storm.STRING);
            Assertions.assertAll(
                    () -> testSerializer(base.size(Range.atLeast(1)), set, list, true),
                    () -> testSerializer(base.size(Range.atLeast(5)), set, list, false),
                    () -> testSerializer(base.size(Range.closed(1, 5)), set, list, true)
            );
        }

    }

    private static Stream<Arguments> testSet() {
        return Stream.of(
                Arguments.of("Empty", Storm.BOOLEAN, ImmutableSet.of()),
                Arguments.of("Integer Element", Storm.BIG_INTEGER, ImmutableSet.of(BigInteger.ONE)),
                Arguments.of("Character Elements", Storm.CHARACTER, ImmutableSet.of('a', 'b', 'c')),
                Arguments.of("Invalid Type", Storm.STRING, null)
        );
    }

    public enum TestEnum {
        FIRST, SECOND, THIRD
    }

    @Nested
    class EnumTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testEnum")
        void testEnum(String test, TestEnum value) {
            testSerializer(Storm.ENUM.of(TestEnum.class), value, value != null ? value.name() : null, value != null);
        }

        @Test
        void testEnumCaseInsensitive() {
            EnumSerializer<TestEnum> serializer = Storm.ENUM.of(TestEnum.class);
            Assertions.assertAll(
                    () -> testDeserializer(serializer, "first", TestEnum.FIRST, true),
                    () -> testDeserializer(serializer, "Second", TestEnum.SECOND, true),
                    () -> testDeserializer(serializer, "THIRD", TestEnum.THIRD, true)
            );
        }

    }

    private static Stream<Arguments> testEnum() {
        return Stream.of(
                Arguments.of("Valid Constant", TestEnum.FIRST),
                Arguments.of("Invalid Constant", null)
        );
    }

    @Nested
    class TupleTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testTuple")
        void testTuple(String test, List<Serializer<?>> serializers, List<?> values, boolean success) {
            testSerializer(Storm.TUPLE.of(serializers), values, values, success);
        }

    }

    private static Stream<Arguments> testTuple() {
        return Stream.of(
                Arguments.of("Empty", ImmutableList.of(), ImmutableList.of(), true),
                Arguments.of("Boolean", ImmutableList.of(Storm.BOOLEAN), ImmutableList.of(true), true),
                Arguments.of("Boolean, Character, String",
                        ImmutableList.of(Storm.BOOLEAN, Storm.CHARACTER, Storm.STRING),
                        ImmutableList.of(true, 'c', "string"),
                        true),
                Arguments.of("Invalid Length",
                        ImmutableList.of(Storm.BOOLEAN),
                        ImmutableList.of(true, false),
                        false),
                Arguments.of("Invalid Type",
                        ImmutableList.of(Storm.BOOLEAN, Storm.CHARACTER),
                        ImmutableList.of(true, "string"),
                        false)
        );
    }

    @Nested
    class MapTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testMap")
        void testMap(String test, Serializer<?> serializer, Object value) {
            testSerializer(Storm.MAP.of(serializer), value, value, value != null);
        }

        @Test
        void testMapInvalidElement() {
            Map<String, Object> map = ImmutableMap.of("x", "first", "y", false, "z", "third");
            testDeserializer(Storm.MAP.of(Storm.STRING), map, null, false);
        }

        @Test
        void testMapSize() {
            Map<String, Object> map = ImmutableMap.of("x", "first", "y", "second", "z", "third");
            MapSerializer<String> base = Storm.MAP.of(Storm.STRING);
            Assertions.assertAll(
                    () -> testSerializer(base.size(Range.atLeast(0)), map, map, true),
                    () -> testSerializer(base.size(Range.atLeast(5)), map, map, false),
                    () -> testSerializer(base.size(Range.closed(1, 5)), map, map, true)
            );
        }

    }

    @Nested
    class ObjectTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testObject")
        void testObject(String test, Map<String, Serializer<?>> serializers, Map<String, ?> values, boolean success) {
            testSerializer(Storm.OBJECT.of(serializers), values, values, success);
        }

    }

    private static Stream<Arguments> testObject() {
        return Stream.of(
                Arguments.of("Empty", ImmutableMap.of(), ImmutableMap.of(), true),
                Arguments.of("Boolean", ImmutableMap.of("b", Storm.BOOLEAN), ImmutableMap.of("b", true), true),
                Arguments.of("Boolean, Character, String",
                        ImmutableMap.of("b", Storm.BOOLEAN, "c", Storm.CHARACTER, "s", Storm.STRING),
                        ImmutableMap.of("b", true, "c", 'c', "s", "string"),
                        true),
                Arguments.of("Missing Property",
                        ImmutableMap.of("x", Storm.BOOLEAN, "y", Storm.BOOLEAN),
                        ImmutableMap.of("x", true),
                        false),
                Arguments.of("Unexpected Property",
                        ImmutableMap.of("x", Storm.BOOLEAN, "y", Storm.BOOLEAN),
                        ImmutableMap.of("z", false),
                        false)
        );
    }

    @Nested
    class UnionTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testUnion")
        void testUnion(String test, Map<Node.Type, Serializer<?>> serializers, Object value, boolean success) {
            testDeserializer(Storm.UNION.of(serializers), value, value, success);
        }

        @Test
        void testUnionUndefined() {
            Assertions.assertEquals("undefined", Node.root().get(Storm.UNION.of(ImmutableMap.of(
                    Node.Type.NULL, Storm.ANY_NULLABLE.def("null"),
                    Node.Type.UNDEFINED, Storm.ANY_OPTIONAL.def("undefined")
            ))));
        }

        @Test
        void testUnionReserialization() {
            testReserializer(Storm.UNION, null, null, false);
        }

    }

    private static Stream<Arguments> testUnion() {
        return Stream.of(
                Arguments.of("Empty", ImmutableMap.of(), null, false),
                Arguments.of("Single", ImmutableMap.of(Node.Type.BOOLEAN, Storm.BOOLEAN), true, true),
                Arguments.of("Multiple", ImmutableMap.of(
                        Node.Type.INTEGER, Storm.BIG_INTEGER,
                        Node.Type.DECIMAL, Storm.BIG_DECIMAL,
                        Node.Type.CHARACTER, Storm.CHARACTER,
                        Node.Type.STRING, Storm.STRING
                ), "string", true),
                Arguments.of("Unknown",
                        ImmutableMap.of(Node.Type.ARRAY, Storm.LIST),
                        ImmutableMap.of("x", BigInteger.ONE, "y", BigInteger.TEN),
                        false)
        );
    }

    @Nested
    class NullableTests {

        @Test
        void testNullable() {
            Serializer<String> serializer = Storm.STRING.nullable();
            Assertions.assertAll(
                    () -> testSerializer(serializer, "string", "string", true),
                    () -> testSerializer(serializer, null, null, true),
                    () -> testDeserializer(serializer, ImmutableList.of("invalid"), null, false)
            );
        }

        @Test
        void testNullableDefault() {
            NullableSerializer<String> serializer = Storm.STRING.nullable("def");
            Assertions.assertAll(
                    () -> testDeserializer(serializer, "string", "string", true),
                    () -> testDeserializer(serializer, null, "def", true),
                    () -> testReserializer(serializer, "def", "def", true),
                    () -> testReserializer(serializer.convertDef(true), "def", null, true),
                    () -> testReserializer(serializer.convertDef(true), null, null, false)
            );
        }

    }

    @Nested
    class OptionalTests {

        @Test
        void testOptional() {
            Serializer<Optional<String>> serializer = Storm.STRING.optional();
            Assertions.assertAll(
                    () -> testSerializer(serializer, Optional.of("string"), "string", true),
                    () -> Assertions.assertEquals(Optional.empty(), Node.root().get(serializer)),
                    () -> testDeserializer(serializer, ImmutableList.of("invalid"), null, false)
            );
        }

        @Test
        void testOptionalDefault() {
            OptionalSerializer.OptionalDefaultSerializer<String> serializer = Storm.STRING.optional("def");
            Assertions.assertAll(
                    () -> testDeserializer(serializer, "string", "string", true),
                    () -> Assertions.assertEquals("def", Node.root().get(serializer)),
                    () -> testReserializer(serializer, "def", "def", true),
                    () -> {
                        Node node = Node.root();
                        node.set("def", serializer.convertDef(true));
                        Assertions.assertEquals(Node.Type.UNDEFINED, node.getType());
                    }
            );
        }

    }

    private static Stream<Arguments> testMap() {
        return Stream.of(
                Arguments.of("Empty", Storm.BOOLEAN, ImmutableMap.of()),
                Arguments.of("Integer Element", Storm.BIG_INTEGER,
                        ImmutableMap.of("key", BigInteger.ONE)),
                Arguments.of("Character Elements", Storm.CHARACTER,
                        ImmutableMap.of("a",'a', "b", 'b', "c", 'c')),
                Arguments.of("Invalid Type", Storm.STRING, null)
        );
    }

    @ParameterizedTest
    @MethodSource("dev.willbanders.storm.serializer.SerializerTests#testCommentRetention")
    <T> void testCommentRetention(String test, Serializer<T> serializer, T start, T value) {
        Node node = Node.root();
        node.attach().setValue(start);
        node.getChildren().forEach(n -> n.setComment("comment: " + n.getKey()));
        node.set(value, serializer);
        Assertions.assertAll(Stream.concat(
                Stream.of(() -> Assertions.assertEquals(value, node.getValue())),
                node.getChildren().stream().map(n -> () -> Assertions.assertEquals("comment: " + n.getKey(), n.getComment())))
        );
    }

    private static Stream<Arguments> testCommentRetention() {
        return Stream.of(
                Arguments.of("List", Storm.LIST.of(Storm.STRING),
                        ImmutableList.of("first", "second", "third"),
                        ImmutableList.of("first", "third")),
                Arguments.of("Tuple", Storm.TUPLE.of(ImmutableList.of(Storm.STRING, Storm.STRING)),
                        ImmutableList.of("first", "second", "third"),
                        ImmutableList.of("first", "third")),
                Arguments.of("Map", Storm.MAP.of(Storm.STRING),
                        ImmutableMap.of("x", "first", "y", "second", "z", "third"),
                        ImmutableMap.of("x", "first", "z", "third")),
                Arguments.of("Object", Storm.OBJECT.of(ImmutableMap.of("x", Storm.STRING, "z", Storm.STRING)),
                        ImmutableMap.of("x", "first", "y", "second", "z", "third"),
                        ImmutableMap.of("x", "first", "z", "third"))
        );
    }

    /**
     * Tests that deserializing the given value equals the expected value if
     * success is true, else asserts a {@link SerializationException} is thrown.
     */
    private void testDeserializer(Serializer<?> serializer, Object value, Object expected, boolean success) {
        Node node = Node.root();
        node.attach().setValue(value);
        test(() -> Assertions.assertEquals(expected, node.get(serializer)), success);
    }

    /**
     * Tests that reserializing the given value equals the expected value if
     * success is true, else asserts a {@link SerializationException} is thrown.
     */
    private void testReserializer(Serializer<?> serializer, Object value, Object expected, boolean success) {
        Node node = Node.root();
        test(() -> {
            node.set(value, (Serializer<Object>) serializer);
            Assertions.assertEquals(expected, node.getValue());
        }, success);
    }

    /**
     * Tests that serializing the given value equals the expect value and that
     * deserializing the expected value equals the given value if success is
     * true, else asserts a {@link SerializationException} is thrown.
     */
    private void testSerializer(Serializer<?> serializer, Object value, Object expected, boolean success) {
        Assertions.assertAll(
                () -> testDeserializer(serializer, expected, value, success),
                () -> testReserializer(serializer, value, expected, success)
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
