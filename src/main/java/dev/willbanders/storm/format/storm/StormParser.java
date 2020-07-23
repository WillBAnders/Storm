package dev.willbanders.storm.format.storm;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.Diagnostic;
import dev.willbanders.storm.format.ParseException;
import dev.willbanders.storm.format.Parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StormParser extends Parser<StormTokenType> {

    private static final Pattern ESCAPES = Pattern.compile("\\\\(?:([bfnrt\'\"\\\\])|u([0-9A-F]{4}))");

    private StormParser(String input) throws ParseException {
        super(new StormLexer(input));
    }

    public static Node parse(String input) {
        return new StormParser(input).parse();
    }

    @Override
    protected Node parse() throws ParseException {
        Node node = Node.root();
        node.attach().setValue(parseRoot());
        return node;
    }

    private Object parseRoot() throws ParseException {
        while (match(StormTokenType.NEWLINE)) {}
        if (!tokens.has(0)) {
            return new LinkedHashMap<>();
        } else if (!peek(StormTokenType.IDENTIFIER) || peek(Arrays.asList("null", "true", "false"))) {
            context.addFirst(tokens.get(0).getRange());
            Object value = parseValue();
            while (match(StormTokenType.NEWLINE)) {}
            require(!tokens.has(0), () -> Diagnostic.builder()
                    .summary("Expected end of input.")
                    .details("The config was parsed as a single value, but more input was provided. Multiple values must be included in an array or object, such as [1, 2, 3] or {x = 1, y = 2, z = 3}."));
            context.removeLast();
            return value;
        } else {
            Map<String, Object> map = new LinkedHashMap<>();
            while (tokens.has(0)) {
                Map.Entry<String, Object> property = parseProperty();
                map.put(property.getKey(), property.getValue());
                if (tokens.has(0)) {
                    require(match(Arrays.asList(",", StormTokenType.NEWLINE)), () -> Diagnostic.builder()
                            .summary("Expected a comma/newline separator after property.")
                            .details("Object properties must be followed by either a comma or a newline. This could caused by an invalid value as well."));
                    while (match(StormTokenType.NEWLINE)) {}
                }
                context.removeLast();
            }
            return map;
        }
    }

    private Object parseValue() throws ParseException {
        Preconditions.checkState(tokens.has(0) && !peek(StormTokenType.NEWLINE), "Broken parser invariant.");
        if (peek("{")) {
            return parseObject();
        } else if (peek("[")) {
            return parseArray();
        } else if (match("null")) {
            return null;
        } else if (match(Arrays.asList("true", "false"))) {
            return Boolean.parseBoolean(tokens.get(-1).getLiteral());
        } else if (match(StormTokenType.INTEGER)) {
            return new BigInteger(tokens.get(-1).getLiteral());
        } else if (match(StormTokenType.DECIMAL)) {
            return new BigDecimal(tokens.get(-1).getLiteral());
        } else if (match(StormTokenType.CHARACTER)) {
            String literal = tokens.get(-1).getLiteral();
            return unescape(literal.substring(1, literal.length() - 1)).charAt(0);
        } else if (match(StormTokenType.STRING)) {
            String literal = tokens.get(-1).getLiteral();
            return unescape(literal.substring(1, literal.length() - 1));
        } else {
            throw error(Diagnostic.builder()
                    .summary("Invalid value.")
                    .details("Expected to parse a value, but found an invalid token. This could be caused by a missing bracket, brace, or quotes.")
                    .range(tokens.get(0).getRange()));
        }
    }

    private List<Object> parseArray() throws ParseException {
        Preconditions.checkState(match("["), "Broken parser invariant.");
        while (match(StormTokenType.NEWLINE)) {}
        List<Object> list = new ArrayList<>();
        while (!match("]")) {
            require(tokens.has(0), () -> Diagnostic.builder()
                    .summary("Unexpected end of input.")
                    .details("Expected to parse an array value, but reached the end of available input. This could be caused by a missing closing bracket ']'."));
            context.addLast(tokens.get(0).getRange());
            list.add(parseValue());
            if (!peek("]")) {
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), () -> Diagnostic.builder()
                        .summary("Expected a comma/newline separator or the closing bracket after array value.")
                        .details("Array values must be followed by either a comma or a newline, or a closing bracket to complete the array. This could also be caused by an invalid value."));
                while (match(StormTokenType.NEWLINE)) {}
            }
            context.removeLast();
        }
        return list;
    }

    private Map<String, Object> parseObject() throws ParseException {
        Preconditions.checkState(match("{"), "Broken parser invariant.");
        while (match(StormTokenType.NEWLINE)) {}
        Map<String, Object> map = new LinkedHashMap<>();
        while (!match("}")) {
            Map.Entry<String, Object> property = parseProperty();
            map.put(property.getKey(), property.getValue());
            if (!peek("}")) {
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), () -> Diagnostic.builder()
                        .summary("Expected a comma/newline separator or the closing brace after property.")
                        .details("Object properties must be followed by either a comma or a newline, or a closing brace to complete the object. This could also be caused by an invalid value."));
                while (match(StormTokenType.NEWLINE)) {}
            }
            context.removeLast();
        }
        return map;
    }

    private Map.Entry<String, Object> parseProperty() {
        require(match(StormTokenType.IDENTIFIER), () -> Diagnostic.builder()
                .summary("Expected an identifier for property key.")
                .details("A property has the form 'key = value', where key is an identifier (a-z followed by a-z, _, or -). Strings and other symbols are not allowed."));
        String key = tokens.get(-1).getLiteral();
        context.addLast(tokens.get(-1).getRange());
        if (!tokens.has(0) || peek(StormTokenType.NEWLINE)) {
            throw error(Diagnostic.builder()
                    .summary("Expected a property value following key.")
                    .details("A property has the form 'key = value', and thus requires an equals sign and value following the key.")
                    .range(tokens.get(-1).getRange()));
        }
        require(match("="), () -> Diagnostic.builder()
                .summary("Expected a equals sign between property key and value.")
                .details("A property has the form 'key = value', and thus requires an equals sign even for arrays and objects."));
        if (!tokens.has(0) || peek(StormTokenType.NEWLINE)) {
            Diagnostic.Range start = tokens.get(-2).getRange();
            Diagnostic.Range end = tokens.get(-1).getRange();
            throw error(Diagnostic.builder()
                    .summary("Expected a property value following key and equals sign.")
                    .details("A property has the form 'key = value', and thus requires a value following the key and equals sign.")
                    .range(Diagnostic.range(start.getIndex(), start.getLine(), start.getColumn(), end.getIndex() + end.getLength() - start.getIndex())));
        }
        return Maps.immutableEntry(key, parseValue());
    }

    private String unescape(String string) {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = ESCAPES.matcher(string);
        int index = 0;
        while (matcher.find()) {
            if (index < matcher.start()) {
                builder.append(string, index, matcher.start());
            }
            if (matcher.group(1) != null) {
                switch (matcher.group(1)) {
                    case "b": builder.append("\b"); break;
                    case "f": builder.append("\f"); break;
                    case "n": builder.append("\n"); break;
                    case "r": builder.append("\r"); break;
                    case "t": builder.append("\t"); break;
                    case "\'": builder.append("\'"); break;
                    case "\"": builder.append("\""); break;
                    case "\\": builder.append("\\"); break;
                    default: throw new IllegalStateException("Broken parser invariant.");
                }
            } else if (matcher.group(2) != null) {
                builder.append((char) Integer.parseInt(matcher.group(2), 16));
            } else {
                throw new IllegalStateException("Broken parser invariant.");
            }
            index = matcher.end();
        }
        return builder.append(string.substring(index)).toString();
    }

}
