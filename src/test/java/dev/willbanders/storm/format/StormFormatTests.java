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

    @ParameterizedTest
    @MethodSource
    void testComment(String test, String input, Map<Object, String> comments) {
        Node deserialized = StormParser.parse(input);
        StringWriter writer = new StringWriter();
        StormGenerator.generate(deserialized, new PrintWriter(writer));
        Node reserialized = StormParser.parse(writer.toString());
        Assertions.assertAll(comments.entrySet().stream().map(e -> () ->
                Assertions.assertAll(Stream.of(deserialized, reserialized).map(n -> () ->
                        Assertions.assertEquals(e.getValue(), (e.getKey() instanceof String ? n.get((String) e.getKey()) : n.resolve(e.getKey())).getComment())
                ))
        ));
    }

    private static Stream<Arguments> testComment() {
        return Stream.of(
                Arguments.of("Empty Config", "//header", ImmutableMap.of("", "header")),
                Arguments.of("Value", "//value\nnull", ImmutableMap.of("", "value")),
                Arguments.of("Property", "//property\nx = 1\ny = 2", ImmutableMap.of("", "", "x", "property", "y", "")),
                Arguments.of("Header", "//header\n\nx = 1\ny = 2", ImmutableMap.of("", "header", "x", "", "y", "")),
                Arguments.of("Header & Property", "//header\n\n//property\nx = 1\ny = 2", ImmutableMap.of("", "header", "x", "property", "y", "")),
                Arguments.of("Array", "[//one\n1,//two\n2,//three\n//three\n6]", ImmutableMap.of(
                        "", "",
                        0, "one",
                        1, "two",
                        2, "three" + System.lineSeparator() + "three"
                )),
                Arguments.of("Object", "//object\nobject={//scalar\nscalar=null, //array\narray=[1]}", ImmutableMap.of(
                        "", "",
                        "object", "object",
                        "object.scalar", "scalar",
                        "object.array", "array"
                )),
                Arguments.of("Multiline", "//line1\n//line2\n//line3\nnull", ImmutableMap.of(
                        "", "line1" + System.lineSeparator() + "line2" + System.lineSeparator() + "line3")
                )
        );
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
    void testInteger(String test, String input, BigInteger value) {
        test(input, value, Node.Type.INTEGER);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "0", new BigInteger("0")),
                Arguments.of("Multiple Digits", "123", new BigInteger("123")),
                Arguments.of("Above Long Max", "123456789123456789123456789", new BigInteger("123456789123456789123456789")),
                Arguments.of("Leading Zeros", "007", new BigInteger("007")),
                Arguments.of("Trailing Zeros", "700", new BigInteger("700")),
                Arguments.of("Positive Sign", "+10", new BigInteger("+10")),
                Arguments.of("Negative Sign", "-10", new BigInteger("-10")),
                Arguments.of("Binary", "0b10", new BigInteger("10", 2)),
                Arguments.of("Octal", "0o123", new BigInteger("123", 8)),
                Arguments.of("Hexadecimal", "0x123ABC", new BigInteger("123ABC", 16))
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
                Arguments.of("Negative Sign", "-10.0"),
                Arguments.of("Scientific", "123.456e789"),
                Arguments.of("Signed Exponent", "123.456e-789")
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
                Arguments.of("Single Property", "{a=true}", ImmutableMap.of("a", true)),
                Arguments.of("Multiple Properties", "{a=\'a\',b=\'b\',c=\'c\'}", ImmutableMap.of("a", 'a', "b", 'b', "c", 'c')),
                Arguments.of("String Key", "{\"0\"=true}", ImmutableMap.of("0", true)),
                Arguments.of("String Key Escapes", "{\"\\\'\"=true}", ImmutableMap.of("\'", true)),
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

    @ParameterizedTest
    @MethodSource
    void testDiagnosticRange(String test, String input, Diagnostic.Range range) {
        ParseException e = Assertions.assertThrows(ParseException.class, () -> StormParser.parse(input));
        Assertions.assertAll(
                () -> Assertions.assertEquals(range.getIndex(), e.getDiagnostic().getRange().getIndex()),
                () -> Assertions.assertEquals(range.getLine(), e.getDiagnostic().getRange().getLine()),
                () -> Assertions.assertEquals(range.getColumn(), e.getDiagnostic().getRange().getColumn()),
                () -> Assertions.assertEquals(range.getLength(), e.getDiagnostic().getRange().getLength())
        );
    }

    private static Stream<Arguments> testDiagnosticRange() {
        return Stream.of(
                Arguments.of("Invalid Comment", "//header\n\n//value\nnull", Diagnostic.range(10, 3, 1, 7)),
                Arguments.of("Empty Character", "\'\'", Diagnostic.range(0, 1, 1, 2)),
                Arguments.of("Too Many Characters", "\'abc\'", Diagnostic.range(0, 1, 1, 5)),
                Arguments.of("Unterminated Character", "\'c", Diagnostic.range(0, 1, 1, 2)),
                Arguments.of("Unterminated String", "\"abc", Diagnostic.range(0, 1, 1, 4)),
                Arguments.of("Invalid Escape", "\"\\c\"", Diagnostic.range(1, 1, 2, 2)),
                Arguments.of("Invalid Unicode Escape", "\"\\u1A#\"", Diagnostic.range(1, 1, 2, 5)),
                Arguments.of("Invalid Value", "#", Diagnostic.range(0, 1, 1, 1)),
                Arguments.of("Extra Value", "1 2", Diagnostic.range(2, 1, 3, 1)),
                Arguments.of("Invalid Value Separator", "[1 2]", Diagnostic.range(3, 1, 4, 1)),
                Arguments.of("Unterminated Array", "[1, 2", Diagnostic.range(4, 1, 5, 1)),
                Arguments.of("Invalid Property Separator", "{x=1 y=2}", Diagnostic.range(5, 1, 6, 1)),
                Arguments.of("Unterminated Object", "{x=1, y=2", Diagnostic.range(8, 1, 9, 1)),
                Arguments.of("Invalid Property Key", "{1=2}", Diagnostic.range(1, 1, 2, 1)),
                Arguments.of("Invalid Property Key-Value Separator", "{x:1}", Diagnostic.range(2, 1, 3, 1)),
                Arguments.of("Missing Property Equals And Value", "{x", Diagnostic.range(1, 1, 2, 1)),
                Arguments.of("Missing Property Value", "{x=", Diagnostic.range(1, 1, 2, 2)),
                Arguments.of("Duplicate Property Key", "{x=1, x=2}", Diagnostic.range(6, 1, 7, 1)),
                Arguments.of("Lazy Lexer", "{x 10, y = \"err}", Diagnostic.range(3, 1, 4, 2))
        );
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
