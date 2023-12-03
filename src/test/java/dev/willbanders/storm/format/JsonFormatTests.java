package dev.willbanders.storm.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.json.JsonGenerator;
import dev.willbanders.storm.format.json.JsonParser;
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

public class JsonFormatTests {

    @Test
    void testComment() {
        Node node = Node.root().attach();
        node.setComment("comment");
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JsonGenerator.generate(node, new PrintWriter(new StringWriter()));
        });
    }

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
    void testInteger(String test, String input, BigInteger expected) {
        test(input, expected, expected != null ? Node.Type.INTEGER : null);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "0", new BigInteger("0")),
                Arguments.of("Multiple Digits", "123", new BigInteger("123")),
                Arguments.of("Above Long Max", "123456789123456789123456789", new BigInteger("123456789123456789123456789")),
                Arguments.of("Leading Zeros", "007", null),
                Arguments.of("Trailing Zeros", "700", new BigInteger("700")),
                Arguments.of("Positive Sign", "+10", null),
                Arguments.of("Negative Sign", "-10", new BigInteger("-10")),
                Arguments.of("Binary", "0b10", null),
                Arguments.of("Octal", "0o123", null),
                Arguments.of("Hexadecimal", "0x123ABC", null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, BigDecimal expected) {
        test(input, expected, expected != null ? Node.Type.DECIMAL : null);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Single Digits", "0.0", new BigDecimal("0.0")),
                Arguments.of("Multiple Digits", "123.456", new BigDecimal("123.456")),
                Arguments.of("Above Integer Precision (2^53 + 1)", "9007199254740993.0", new BigDecimal("9007199254740993.0")),
                Arguments.of("Leading Zeros", "007.0", null),
                Arguments.of("Trailing Zeros", "0.700", new BigDecimal("0.700")),
                Arguments.of("Positive Sign", "+10.0", null),
                Arguments.of("Negative Sign", "-10.0", new BigDecimal("-10.0")),
                Arguments.of("Integer Significand", "123e456", new BigDecimal("123e456")),
                Arguments.of("Scientific Lowercase e", "123.456e789", new BigDecimal("123.456e789")),
                Arguments.of("Scientific Uppercase E", "123.456E789", new BigDecimal("123.456E789")),
                Arguments.of("Signed Exponent Positive", "123.456e+789", new BigDecimal("123.456e+789")),
                Arguments.of("Signed Exponent Negative", "123.456e-789", new BigDecimal("123.456e-789"))
        );
    }

    @Test
    void testCharacter() {
        Node node = Node.root().attach();
        node.setValue('c');
        Assertions.assertThrows(IllegalStateException.class, () -> {
            JsonGenerator.generate(node, new PrintWriter(new StringWriter()));
        });
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
                Arguments.of("Control Character", "\"\u0007\"", null),
                Arguments.of("DEL Control Character", "\"\u001F\"", null),
                Arguments.of("Alphanumeric", "\"abc123\"", "abc123"),
                Arguments.of("Symbols", "\"!@#$%^&*\"", "!@#$%^&*"),
                Arguments.of("Escapes", "\"\\b\\f\\n\\r\\t\\\"\\\\\\/\"", "\b\f\n\r\t\"\\/"),
                Arguments.of("Unicode Escapes", "\"a\\u0000b\\u12ABc\\u12abd\"", "a\u0000b\u12ABc\u12abd"),
                Arguments.of("Invalid Escape", "\"\\c\"", null),
                Arguments.of("Unterminated", "\"", null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testArray(String test, String input, List<Object> expected) {
        test(input, expected, expected != null ? Node.Type.ARRAY : null);
    }

    private static Stream<Arguments> testArray() {
        return Stream.of(
                Arguments.of("Empty", "[]", ImmutableList.of()),
                Arguments.of("Single Value", "[true]", ImmutableList.of(true)),
                Arguments.of("Multiple Values", "[\"a\",\"b\",\"c\"]", ImmutableList.of("a", "b", "c")),
                Arguments.of("Newline Separators", "[\n\"a\"\n\n\"b\"\n\n\n]", null),
                Arguments.of("Trailing Comma", "[\"a\",]", null),
                Arguments.of("Nested Arrays", "[[\"a\"],[\"b\"]]", ImmutableList.of(ImmutableList.of("a"), ImmutableList.of("b"))),
                Arguments.of("Mixed Values", "[false,0,\"==\"]", ImmutableList.of(false, BigInteger.ZERO, "=="))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testObject(String test, String input, Map<String, Object> expected) {
        test(input, expected, expected != null ? Node.Type.OBJECT : null);
    }

    private static Stream<Arguments> testObject() {
        return Stream.of(
                Arguments.of("Empty", "{}", ImmutableMap.of()),
                Arguments.of("Single Property", "{\"a\":true}", ImmutableMap.of("a", true)),
                Arguments.of("Multiple Properties", "{\"a\":\"a\",\"b\":\"b\",\"c\":\"c\"}", ImmutableMap.of("a", "a", "b", "b", "c", "c")),
                Arguments.of("Identifier Key", "{a:\"a\"}", null),
                Arguments.of("String Key Escapes", "{\"\\\"\":true}", ImmutableMap.of("\"", true)),
                Arguments.of("Missing Comma (Newline)", "{\"a\":\"a\"\n\"b\":\"b\"}", null),
                Arguments.of("Trailing Comma", "{\"a\":\"a\",}", null),
                Arguments.of("Nested Objects", "{\"x\":{},\"y\":{\"z\":\"z\"}}", ImmutableMap.of("x", ImmutableMap.of(), "y", ImmutableMap.of("z", "z"))),
                Arguments.of("Mixed Values", "{\"x\":false,\"y\":0,\"z\":\"==\"}", ImmutableMap.of("x", false, "y", BigInteger.ZERO, "z", "=="))
        );
    }

    @Test
    void testEverything() {
        String input = String.join("\n", ImmutableList.of(
                "{",
                "    \"keywords\": [",
                "        null, true, false",
                "    ],",
                "    \"numbers\": {",
                "        \"integer\": 42,",
                "        \"decimal\": 6.28",
                "    },",
                "    \"string\": \"Hello, World!\",",
                "    \"mixed\": [",
                "        [1, \"2\"],",
                "        {",
                "            \"nested\": [3.0, \"4.0\"]",
                "        }",
                "    ]",
                "}"
        ));
        Object value = ImmutableMap.of(
                "keywords", Arrays.asList(null, true, false),
                "numbers", ImmutableMap.of(
                        "integer", BigInteger.valueOf(42),
                        "decimal", BigDecimal.valueOf(6.28)
                ),
                "string", "Hello, World!",
                "mixed", ImmutableList.of(
                        ImmutableList.of(BigInteger.ONE, "2"),
                        ImmutableMap.of("nested", ImmutableList.of(BigDecimal.valueOf(3.0), "4.0"))
                )
        );
        test(input, value, Node.Type.OBJECT);
    }

    @ParameterizedTest
    @MethodSource
    void testDiagnosticRange(String test, String input, Diagnostic.Range range) {
        ParseException e = Assertions.assertThrows(ParseException.class, () -> JsonParser.parse(input));
        Assertions.assertAll(
                () -> Assertions.assertEquals(range.getIndex(), e.getDiagnostic().getRange().getIndex()),
                () -> Assertions.assertEquals(range.getLine(), e.getDiagnostic().getRange().getLine()),
                () -> Assertions.assertEquals(range.getColumn(), e.getDiagnostic().getRange().getColumn()),
                () -> Assertions.assertEquals(range.getLength(), e.getDiagnostic().getRange().getLength())
        );
    }

    private static Stream<Arguments> testDiagnosticRange() {
        return Stream.of(
                Arguments.of("Empty", "", Diagnostic.range(0, 1, 1, 0)),
                Arguments.of("Empty Whitespace", " \t", Diagnostic.range(2, 1, 3, 0)),
                Arguments.of("Empty Newline", " \n\t", Diagnostic.range(3, 2, 2, 0)),
                Arguments.of("Newline", "\n\r#", Diagnostic.range(2, 2, 1, 1)),
                Arguments.of("Comment", "//header", Diagnostic.range(0, 1, 1, 1)),
                Arguments.of("Invalid Decimal", "1.zero", Diagnostic.range(0, 1, 1, 2)),
                Arguments.of("Invalid Exponent", "1ee7", Diagnostic.range(0, 1, 1, 2)),
                Arguments.of("Unterminated String", "\"abc", Diagnostic.range(0, 1, 1, 4)),
                Arguments.of("Invalid Escape", "\"\\c\"", Diagnostic.range(1, 1, 2, 2)),
                Arguments.of("Invalid Unicode Escape", "\"\\u1A#\"", Diagnostic.range(1, 1, 2, 5)),
                Arguments.of("Invalid Value", "#", Diagnostic.range(0, 1, 1, 1)),
                Arguments.of("Extra Value", "1 2", Diagnostic.range(2, 1, 3, 1)),
                Arguments.of("Invalid Value Separator", "[1 2]", Diagnostic.range(3, 1, 4, 1)),
                Arguments.of("Unterminated Array", "[1, 2", Diagnostic.range(4, 1, 5, 1)),
                Arguments.of("Invalid Property Separator", "{\"x\":1 \"y\":2}", Diagnostic.range(7, 1, 8, 3)),
                Arguments.of("Unterminated Object", "{\"x\":1, \"y\":2", Diagnostic.range(12, 1, 13, 1)),
                Arguments.of("Invalid Property Key", "{1:2}", Diagnostic.range(1, 1, 2, 1)),
                Arguments.of("Invalid Property Key-Value Separator", "{\"x\"=1}", Diagnostic.range(4, 1, 5, 1)),
                Arguments.of("Missing Property Equals And Value", "{\"x\"", Diagnostic.range(1, 1, 2, 3)),
                Arguments.of("Missing Property Value", "{\"x\":", Diagnostic.range(1, 1, 2, 4)),
                Arguments.of("Duplicate Property Key", "{\"x\":1, \"x\":2}", Diagnostic.range(8, 1, 9, 3)),
                Arguments.of("Lazy Lexer", "{\"x\" 10, \"y\": \"err}", Diagnostic.range(5, 1, 6, 2))
        );
    }

    void test(String input, Object value, Node.Type type) {
        if (type != null) {
            Assertions.assertAll(
                    () -> {
                        Node node = JsonParser.parse(input);
                        Assertions.assertAll(
                                () -> Assertions.assertEquals(type, node.getType()),
                                () -> Assertions.assertEquals(value, node.getValue())
                        );
                    },
                    () -> {
                        Node root = Node.root();
                        root.attach().setValue(value);
                        StringWriter writer = new StringWriter();
                        JsonGenerator.generate(root, new PrintWriter(writer));
                        Node node = JsonParser.parse(writer.toString());
                        Assertions.assertAll(
                                () -> Assertions.assertEquals(type, node.getType()),
                                () -> Assertions.assertEquals(value, node.getValue())
                        );
                    }
            );
        } else {
            Assertions.assertThrows(ParseException.class, () -> JsonParser.parse(input));
        }
    }

}
