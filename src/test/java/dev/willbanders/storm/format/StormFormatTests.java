package dev.willbanders.storm.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.storm.StormGenerator;
import dev.willbanders.storm.format.storm.StormParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class StormFormatTests {

    @Test
    void testNull() {
        test("null", null, Node.Type.NULL);
    }

    @Test
    void testBoolean() {
        Assertions.assertAll(
                () -> test("true", true, Node.Type.BOOLEAN),
                () -> test("false", false, Node.Type.BOOLEAN)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input) {
        BigInteger integer = null;
        try {
            integer = new BigInteger(input);
        } catch (NumberFormatException ignored) {}
        test(input, integer, Node.Type.INTEGER);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "0"),
                Arguments.of("Multiple Digits", "123"),
                Arguments.of("Above Long Max", "123456789123456789123456789"),
                Arguments.of("Leading Zeros", "007"),
                Arguments.of("Trailing Zeros", "700"),
                Arguments.of("Positive Sign", "+10"),
                Arguments.of("Negative Sign", "-10")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input) {
        BigDecimal decimal = null;
        try {
            decimal = new BigDecimal(input);
        } catch (NumberFormatException ignored) {}
        test(input, decimal, Node.Type.DECIMAL);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Single Digits", "0.0"),
                Arguments.of("Multiple Digits", "123.456"),
                Arguments.of("Above Integer Precision (2^53 + 1)", "9007199254740993.0"),
                Arguments.of("Leading Zeros", "007.0"),
                Arguments.of("Trailing Zeros", "0.700"),
                Arguments.of("Positive Sign", "+10.0"),
                Arguments.of("Negative Sign", "-10.0")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, Character expected) {
        test(input, expected, expected != null ? Node.Type.CHARACTER : null);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Empty", "\'\'", null),
                Arguments.of("Alphabetic", "\'a\'", 'a'),
                Arguments.of("Numeric", "\'0\'", '0'),
                Arguments.of("Symbol", "\'!\'", '!'),
                Arguments.of("Escape", "\'\\\'\'", '\''),
                Arguments.of("Unicode Escape", "\'\\u12AB\'", "\u12AB"),
                Arguments.of("Invalid Escape", "\'\\c\'", null),
                Arguments.of("Unterminated", "\'c", null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, String expected) {
        test(input, expected, expected != null ? Node.Type.STRING : null);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", ""),
                Arguments.of("Single Character", "\"c\"", "c"),
                Arguments.of("Alphanumeric", "\"abc123\"", "abc123"),
                Arguments.of("Symbols", "\"!@#$%^&*\"", "!@#$%^&*"),
                Arguments.of("Escapes", "\"\\b\\f\\n\\r\\t\\\'\\\"\\\\\"", "\b\f\n\r\t\'\"\\"),
                Arguments.of("Unicode Escapes", "\"a\\u0000b\\u12ABc\"", "a\u0000b\u12ABc"),
                Arguments.of("Invalid Escape", "\"\\c\"", null),
                Arguments.of("Unterminated", "\"", null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testArray(String test, String input, List<Object> expected) {
        test(input, expected, Node.Type.ARRAY);
    }

    private static Stream<Arguments> testArray() {
        return Stream.of(
                Arguments.of("Empty", "[]", ImmutableList.of()),
                Arguments.of("Single Value", "[true]", ImmutableList.of(true)),
                Arguments.of("Multiple Values", "[\'a\',\'b\',\'c\']", ImmutableList.of('a', 'b', 'c')),
                Arguments.of("Newline Separators", "[\n\'a\'\n\n\'b\'\n\n\n]", ImmutableList.of('a', 'b')),
                Arguments.of("Trailing Comma", "[\'a\',]", ImmutableList.of('a')),
                Arguments.of("Nested Arrays", "[[\'a\'],[\'b\']]", ImmutableList.of(ImmutableList.of('a'), ImmutableList.of('b'))),
                Arguments.of("Mixed Values", "[false,0,\"==\"]", ImmutableList.of(false, BigInteger.ZERO, "=="))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testObject(String test, String input, Map<String, Object> expected) {
        Assertions.assertAll(
                () -> test(input, expected, Node.Type.OBJECT),
                () -> test(input.substring(1, input.length() - 1), expected, Node.Type.OBJECT)
        );
    }

    private static Stream<Arguments> testObject() {
        return Stream.of(
                Arguments.of("Empty", "{}", ImmutableMap.of()),
                Arguments.of("Single Value", "{a=true}", ImmutableMap.of("a", true)),
                Arguments.of("Multiple Values", "{a=\'a\',b=\'b\',c=\'c\'}", ImmutableMap.of("a", 'a', "b", 'b', "c", 'c')),
                Arguments.of("Newline Separators", "{\na=\'a\'\n\nb=\'b\'\n\n\n}", ImmutableMap.of("a", 'a', "b", 'b')),
                Arguments.of("Trailing Comma", "{a=\'a\',}", ImmutableMap.of("a", 'a')),
                Arguments.of("Nested Objects", "{x={},y={z=\'z\'}}", ImmutableMap.of("x", ImmutableMap.of(), "y", ImmutableMap.of("z", 'z'))),
                Arguments.of("Mixed Values", "{x=false,y=0,z=\"==\"}", ImmutableMap.of("x", false, "y", BigInteger.ZERO, "z", "=="))
        );
    }

    @Test
    void testEverything() {
        String input = String.join("\n", ImmutableList.of(
                "keywords = [",
                "    null, true, false,",
                "]",
                "numbers = {",
                "    integer = 42",
                "    decimal = 6.28",
                "}",
                "character = \'f\'",
                "string = \"Hello, World!\"",
                "mixed = [",
                "    [1, '2']",
                "    {",
                "        nested = [3.0, \"4.0\"]",
                "    }",
                "]"
        ));
        Object value = ImmutableMap.of(
                "keywords", Arrays.asList(null, true, false),
                "numbers", ImmutableMap.of(
                        "integer", BigInteger.valueOf(42),
                        "decimal", BigDecimal.valueOf(6.28)
                ),
                "character", 'f',
                "string", "Hello, World!",
                "mixed", ImmutableList.of(
                        ImmutableList.of(BigInteger.ONE, '2'),
                        ImmutableMap.of("nested", ImmutableList.of(BigDecimal.valueOf(3.0), "4.0"))
                )
        );
        test(input, value, Node.Type.OBJECT);
    }

    void test(String input, Object value, Node.Type type) {
        if (type != null) {
            Assertions.assertAll(
                    () -> {
                        Node node = StormParser.parse(input);
                        Assertions.assertAll(
                                () -> Assertions.assertEquals(type, node.getType()),
                                () -> Assertions.assertEquals(value, node.getValue())
                        );
                    },
                    () -> {
                        Node root = Node.root();
                        root.attach().setValue(value);
                        StringWriter writer = new StringWriter();
                        StormGenerator.generate(root, new PrintWriter(writer));
                        Node node = StormParser.parse(writer.toString());
                        Assertions.assertAll(
                                () -> Assertions.assertEquals(type, node.getType()),
                                () -> Assertions.assertEquals(value, node.getValue())
                        );
                    }
            );
        } else {
            Assertions.assertThrows(ParseException.class, () -> StormParser.parse(input));
        }
    }

}
