package dev.willbanders.storm.format.json;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.Diagnostic;
import dev.willbanders.storm.format.ParseException;
import dev.willbanders.storm.format.Parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonParser extends Parser<JsonTokenType> {

    private static final Pattern ESCAPES = Pattern.compile("\\\\(?:([bfnrt\"\\\\/])|u([0-9A-Fa-f]{4}))");

    private JsonParser(String input) throws ParseException {
        super(new JsonLexer(input));
    }

    public static Node parse(String input) {
        return new JsonParser(input).parse();
    }

    @Override
    protected Node parse() throws ParseException {
        Node node = Node.root();
        require(tokens.has(0), () -> Diagnostic.builder()
                .summary("Empty JSON content.")
                .details("Expected to parse a value, but the JSON content was empty."));
        context.addFirst(tokens.get(0).getRange());
        parseValue(node);
        require(!tokens.has(0), () -> Diagnostic.builder()
                .summary("Expected end of input.")
                .details("The config was parsed as a single value, but more input was provided. Multiple values must be included in an array or object, such as [1, 2, 3] or {x = 1, y = 2, z = 3}."));
        context.removeLast();
        return node;
    }

    private void parseValue(Node node) throws ParseException {
        Preconditions.checkState(tokens.has(0), "Broken parser invariant.");
        if (peek("{")) {
            parseObject(node);
        } else if (peek("[")) {
            parseArray(node);
        } else if (match("null")) {
            node.attach().setValue(null);
        } else if (match(Arrays.asList("true", "false"))) {
            node.attach().setValue(Boolean.parseBoolean(tokens.get(-1).getLiteral()));
        } else if (match(JsonTokenType.INTEGER)) {
            node.attach().setValue(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(JsonTokenType.DECIMAL)) {
            node.attach().setValue(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(JsonTokenType.STRING)) {
            String literal = tokens.get(-1).getLiteral();
            node.attach().setValue(unescape(literal.substring(1, literal.length() - 1)));
        } else {
            throw error(Diagnostic.builder()
                    .summary("Invalid value.")
                    .details("Expected to parse a value, but found an invalid token. This could be caused by a missing bracket, brace, or quotes.")
                    .range(tokens.get(0).getRange()));
        }
    }

    private void parseArray(Node node) throws ParseException {
        Preconditions.checkState(match("["), "Broken parser invariant.");
        node.attach().setValue(Lists.newArrayList());
        while (!match("]")) {
            require(tokens.has(0), () -> Diagnostic.builder()
                    .summary("Unexpected end of input.")
                    .details("Expected to parse an array value, but reached the end of available input. This could be caused by a missing closing bracket ']'."));
            context.addLast(tokens.get(0).getRange());
            Node child = node.resolve(node.getList().size());
            parseValue(child);
            if (!peek("]")) {
                require(match(","), () -> Diagnostic.builder()
                        .summary("Expected a comma separator or the closing bracket after array value.")
                        .details("Array values must be followed by either a comma or a closing bracket to complete the array. This could also be caused by an invalid value."));
                require(!peek("]"), () -> Diagnostic.builder()
                        .summary("Invalid trailing comma.")
                        .details("Array values must not use a trailing comma. This could also be caused by a missing value."));
            }
            context.removeLast();
        }
    }

    private void parseObject(Node node) throws ParseException {
        Preconditions.checkState(match("{"), "Broken parser invariant.");
        node.attach().setValue(Maps.newLinkedHashMap());
        Map<String, Diagnostic.Range> defined = Maps.newHashMap();
        while (!match("}")) {
            parseProperty(node, defined);
            if (!peek("}")) {
                require(match(","), () -> Diagnostic.builder()
                    .summary("Expected a comma separator or the closing brace after property.")
                    .details("Object properties must be followed by either a comma or a closing bracket to complete the object. This could also be caused by an invalid value."));
                require(!peek("}"), () -> Diagnostic.builder()
                    .summary("Invalid trailing comma.")
                    .details("Object properties must not use a trailing comma. This could also be caused by a missing property."));
            }
            context.removeLast();
        }
    }

    private void parseProperty(Node node, Map<String, Diagnostic.Range> defined) throws ParseException {
        require(match(JsonTokenType.STRING), () -> Diagnostic.builder()
                .summary("Expected a string for property key.")
                .details("A property has the form 'key: value', where key is a string. Properties must not use unquoted keys."));
        String key = unescape(tokens.get(-1).getLiteral().substring(1, tokens.get(-1).getLiteral().length() - 1));
        if (defined.containsKey(key)) {
            context.push(defined.get(key));
            // Duplicate keys are considered implementation-dependent in JSON.
            // In the context of Storm, it's preferable to reject duplicate keys
            // to maintain stronger type safety and reporting of potential
            // errors. For compatability with JSON generated by other tools, it
            // may be worth considering a parser option for this.
            throw error(Diagnostic.builder()
                    .summary("Property key is already defined.")
                    .details("Object properties must be unique, and thus the same key cannot be used in more than one property.")
                    .range(tokens.get(-1).getRange()));
        }
        defined.put(key, tokens.get(-1).getRange());
        context.addLast(tokens.get(-1).getRange());
        if (!tokens.has(0)) {
            throw error(Diagnostic.builder()
                    .summary("Expected a property value following key.")
                    .details("A property has the form 'key: value', and thus requires a colon and value following the key.")
                    .range(tokens.get(-1).getRange()));
        }
        require(match(":"), () -> Diagnostic.builder()
                .summary("Expected a colon between property key and value.")
                .details("A property has the form 'key: value', and thus requires a colon even for arrays and objects."));
        if (!tokens.has(0)) {
            Diagnostic.Range start = tokens.get(-2).getRange();
            Diagnostic.Range end = tokens.get(-1).getRange();
            throw error(Diagnostic.builder()
                    .summary("Expected a property value following key and colon.")
                    .details("A property has the form 'key: value', and thus requires a value following the key and colon.")
                    .range(Diagnostic.range(start.getIndex(), start.getLine(), start.getColumn(), end.getIndex() + end.getLength() - start.getIndex())));
        }
        parseValue(node.resolve(key));
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
                    case "\"": builder.append("\""); break;
                    case "\\": builder.append("\\"); break;
                    case "/": builder.append("/"); break;
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
