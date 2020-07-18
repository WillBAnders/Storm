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

public final class StormParser extends Parser<StormTokenType> {

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
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), "Expected a comma or newline separator following property.");
                while (match(StormTokenType.NEWLINE)) {}
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
            return escape(literal.substring(1, literal.length() - 1)).charAt(0);
        } else if (match(StormTokenType.STRING)) {
            String literal = tokens.get(-1).getLiteral();
            return escape(literal.substring(1, literal.length() - 1));
        } else {
            throw new ParseException("Invalid value: " + tokens.get(0).getLiteral());
        }
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
                System.out.println("Pre: " + tokens.get(-1).getType() + "/" + tokens.get(-1).getLiteral());
                System.out.println("Post: " + tokens.get(0).getType() + "/" + tokens.get(0).getLiteral());
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), "Expected a comma or newline separator following property.");
                while (match(StormTokenType.NEWLINE)) {}
            }
        }
        return map;
    }

    private List<Object> parseArray() throws ParseException {
        require(match("["), "Broken parser invariant");
        while (match(StormTokenType.NEWLINE)) {}
        List<Object> list = new ArrayList<>();
        while (!match("]")) {
            list.add(parseValue());
            if (!peek("]")) {
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), "Expected a comma or newline separator following array element.");
                while (match(StormTokenType.NEWLINE)) {}
            }
        }
        return list;
    }

    private String escape(String string) {
        return string.replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\'", "\'")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

}
