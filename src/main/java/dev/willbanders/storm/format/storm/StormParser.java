package dev.willbanders.storm.format.storm;

import dev.willbanders.storm.config.Node;
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
        require(!tokens.has(0), "Expected end of input.");
        return node;
    }

    private Object parseRoot() throws ParseException {
        while (match(StormTokenType.NEWLINE)) {}
        if (!tokens.has(0)) {
            return new LinkedHashMap<>();
        } else if (!peek(StormTokenType.IDENTIFIER) || peek(Arrays.asList("null", "true", "false"))) {
            return parseValue();
        } else {
            Map<String, Object> map = new LinkedHashMap<>();
            while (tokens.has(0)) {
                require(match(StormTokenType.IDENTIFIER), "Expected an identifier or string for property key.");
                String key = tokens.get(-1).getLiteral();
                require(match("="), "Expected an equal sign separator for property value.");
                map.put(key, parseValue());
                if (tokens.has(0)) {
                    require(match(Arrays.asList(",", StormTokenType.NEWLINE)), "Expected a comma/newline separator or closing brace following property.");
                    while (match(StormTokenType.NEWLINE)) {}
                }
            }
            return map;
        }
    }

    private Object parseValue() throws ParseException {
        require(tokens.has(0), "Unexpected end of input.");
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
            throw new ParseException("Invalid value: " + tokens.get(0).getLiteral());
        }
    }

    private List<Object> parseArray() throws ParseException {
        require(match("["), "Broken parser invariant");
        while (match(StormTokenType.NEWLINE)) {}
        List<Object> list = new ArrayList<>();
        while (!match("]")) {
            list.add(parseValue());
            if (!peek("]")) {
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), "Expected a comma/newline separator or the closing bracket following array element.");
                while (match(StormTokenType.NEWLINE)) {}
            }
        }
        return list;
    }

    private Map<String, Object> parseObject() throws ParseException {
        require(match("{"), "Broken parser invariant.");
        while (match(StormTokenType.NEWLINE)) {}
        Map<String, Object> map = new LinkedHashMap<>();
        while (!match("}")) {
            require(match(StormTokenType.IDENTIFIER), "Expected an identifier or string for property key.");
            String key = tokens.get(-1).getLiteral();
            require(match("="), "Expected an equal sign separator for property value.");
            map.put(key, parseValue());
            if (!peek("}")) {
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), "Expected a comma/newline separator or the closing brace following property.");
                while (match(StormTokenType.NEWLINE)) {}
            }
        }
        return map;
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
