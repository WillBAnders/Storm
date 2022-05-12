package dev.willbanders.storm.format.storm;

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
        parseRoot(node);
        return node;
    }

    private void parseRoot(Node node) throws ParseException {
        String comment = "";
        if (peek(StormTokenType.COMMENT)) {
            context.push(tokens.get(0).getRange());
            comment = parseComment();
            if (peek(StormTokenType.NEWLINE)) {
                while (match(StormTokenType.NEWLINE)) {}
                node.setComment(comment);
                if (peek(StormTokenType.COMMENT)) {
                    context.push(tokens.get(0).getRange());
                }
                comment = parseComment();
            }
        } else {
            while (match(StormTokenType.NEWLINE)) {}
        }
        if (!peek(Arrays.asList(StormTokenType.IDENTIFIER, StormTokenType.STRING), "=")) {
            if (!comment.isEmpty()) {
                if (!node.getComment().isEmpty()) {
                    throw error(Diagnostic.builder()
                            .summary("Invalid comment.")
                            .details("A comment on this node is not allowed as a header comment has already been defined.")
                            .range(context.peek()));
                }
                node.setComment(comment);
            }
            context.clear();
            if (tokens.has(0)) {
                context.addFirst(tokens.get(0).getRange());
                parseValue(node);
                while (match(StormTokenType.NEWLINE)) {}
                require(!tokens.has(0), () -> Diagnostic.builder()
                        .summary("Expected end of input.")
                        .details("The config was parsed as a single value, but more input was provided. Multiple values must be included in an array or object, such as [1, 2, 3] or {x = 1, y = 2, z = 3}."));
                context.removeLast();
            } else {
                node.attach().setValue(Maps.newLinkedHashMap());
            }
        } else {
            context.clear();
            node.attach().setValue(Maps.newLinkedHashMap());
            Map<String, Diagnostic.Range> defined = Maps.newHashMap();
            while (tokens.has(0)) {
                parseProperty(node, defined);
                if (tokens.has(0)) {
                    require(match(Arrays.asList(",", StormTokenType.NEWLINE)), () -> Diagnostic.builder()
                            .summary("Expected a comma/newline separator after property.")
                            .details("Object properties must be followed by either a comma or a newline. This could caused by an invalid value as well."));
                    while (match(StormTokenType.NEWLINE)) {}
                }
                context.removeLast();
            }
            node.getChildren().iterator().next().setComment(comment);
        }
    }

    private String parseComment() throws ParseException {
        StringBuilder builder = new StringBuilder();
        while (match(StormTokenType.COMMENT)) {
            builder.append(tokens.get(-1).getLiteral().substring(2));
            if (peek(StormTokenType.COMMENT)) {
                builder.append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private void parseValue(Node node) throws ParseException {
        Preconditions.checkState(tokens.has(0) && !peek(StormTokenType.NEWLINE), "Broken parser invariant.");
        if (peek("{")) {
            parseObject(node);
        } else if (peek("[")) {
            parseArray(node);
        } else if (match("null")) {
            node.attach().setValue(null);
        } else if (match(Arrays.asList("true", "false"))) {
            node.attach().setValue(Boolean.parseBoolean(tokens.get(-1).getLiteral()));
        } else if (match(StormTokenType.INTEGER)) {
            node.attach().setValue(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(StormTokenType.DECIMAL)) {
            node.attach().setValue(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(StormTokenType.CHARACTER)) {
            String literal = tokens.get(-1).getLiteral();
            node.attach().setValue(unescape(literal.substring(1, literal.length() - 1)).charAt(0));
        } else if (match(StormTokenType.STRING)) {
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
        while (match(StormTokenType.NEWLINE)) {}
        node.attach().setValue(Lists.newArrayList());
        while (!match("]")) {
            String comment = parseComment();
            require(tokens.has(0), () -> Diagnostic.builder()
                    .summary("Unexpected end of input.")
                    .details("Expected to parse an array value, but reached the end of available input. This could be caused by a missing closing bracket ']'."));
            context.addLast(tokens.get(0).getRange());
            Node child = node.resolve(node.getList().size());
            child.setComment(comment);
            parseValue(child);
            if (!peek("]")) {
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), () -> Diagnostic.builder()
                        .summary("Expected a comma/newline separator or the closing bracket after array value.")
                        .details("Array values must be followed by either a comma or a newline, or a closing bracket to complete the array. This could also be caused by an invalid value."));
                while (match(StormTokenType.NEWLINE)) {}
            }
            context.removeLast();
        }
    }

    private void parseObject(Node node) throws ParseException {
        Preconditions.checkState(match("{"), "Broken parser invariant.");
        while (match(StormTokenType.NEWLINE)) {}
        node.attach().setValue(Maps.newLinkedHashMap());
        Map<String, Diagnostic.Range> defined = Maps.newHashMap();
        while (!match("}")) {
            parseProperty(node, defined);
            if (!peek("}")) {
                require(match(Arrays.asList(",", StormTokenType.NEWLINE)), () -> Diagnostic.builder()
                        .summary("Expected a comma/newline separator or the closing brace after property.")
                        .details("Object properties must be followed by either a comma or a newline, or a closing brace to complete the object. This could also be caused by an invalid value."));
                while (match(StormTokenType.NEWLINE)) {}
            }
            context.removeLast();
        }
    }

    private void parseProperty(Node node, Map<String, Diagnostic.Range> defined) throws ParseException {
        String comment = parseComment();
        require(match(Arrays.asList(StormTokenType.IDENTIFIER, StormTokenType.STRING)), () -> Diagnostic.builder()
                .summary("Expected an identifier for property key.")
                .details("A property has the form 'key = value', where key is an identifier (alphanumeric, '_', or '-' starting with a letter or '_') or a string."));
        String key = tokens.get(-1).getType() == StormTokenType.IDENTIFIER
                ? tokens.get(-1).getLiteral()
                : unescape(tokens.get(-1).getLiteral().substring(1, tokens.get(-1).getLiteral().length() - 1));
        if (defined.containsKey(key)) {
            context.push(defined.get(key));
            throw error(Diagnostic.builder()
                    .summary("Property key is already defined.")
                    .details("Object properties must be unique, and thus the same key cannot be used in more than one property.")
                    .range(tokens.get(-1).getRange()));
        }
        defined.put(key, tokens.get(-1).getRange());
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
        Node child = node.resolve(key);
        child.setComment(comment);
        parseValue(child);
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
