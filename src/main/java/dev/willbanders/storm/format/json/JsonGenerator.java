package dev.willbanders.storm.format.json;

import com.google.common.base.Preconditions;
import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.Generator;

import java.io.PrintWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonGenerator extends Generator {

    private static final Pattern ESCAPES = Pattern.compile("([\"\\\\\\p{Cntrl}])");

    private JsonGenerator(PrintWriter writer) {
        super(writer);
    }

    public static void generate(Node node, PrintWriter writer) {
        new JsonGenerator(writer).generate(node);
    }

    @Override
    protected void generateRoot(Node root) {
        generateComment(root);
        super.generateRoot(root);
    }

    @Override
    protected void generateComment(Node node) {
        // Comments are not permitted in JSON. In the context of Storm, it's
        // preferable to error in this case rather than to silently ignore
        // comments. For future support, it may be worth considering a parser
        // option for this (or JSON5 compatibility).
        Preconditions.checkState(node.getComment().isEmpty(), "Comments are not supported in JSON.");
    }

    @Override
    protected void generateCharacter(Node node) {
        // Character literals are not permitted in JSON. In the context of
        // Storm, it's preferable to error in this case rather than to silently
        // convert to a String (or similar). For future support, it may be worth
        // considering a parser option for this.
        throw new IllegalStateException("Character literals are not supported in JSON.");
    }

    @Override
    protected void generateString(Node node) {
        write("\"", escape(node.getValue().toString()), "\"");
    }

    private String escape(String string) {
        Matcher matcher = ESCAPES.matcher(string);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            switch (matcher.group()) {
                case "\b": matcher.appendReplacement(buffer, "\\\\b"); break;
                case "\f": matcher.appendReplacement(buffer, "\\\\f"); break;
                case "\n": matcher.appendReplacement(buffer, "\\\\n"); break;
                case "\r": matcher.appendReplacement(buffer, "\\\\r"); break;
                case "\t": matcher.appendReplacement(buffer, "\\\\t"); break;
                case "\"": matcher.appendReplacement(buffer, "\\\\\""); break;
                case "\\": matcher.appendReplacement(buffer, "\\\\\\\\"); break;
                default: matcher.appendReplacement(buffer, String.format("\\\\u%04X", (int) matcher.group().charAt(0)));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    @Override
    protected void generateArray(Node node) {
        write("[");
        if (!node.getList().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < node.getList().size(); i++) {
                generateComment(node.resolve(i));
                write(node.resolve(i));
                if (i != node.getList().size() - 1) {
                    write(",");
                    newline(indent);
                }
            }
            newline(--indent);
        }
        write("]");
    }

    @Override
    protected void generateObject(Node node) {
        write("{");
        if (!node.getMap().isEmpty()) {
            newline(++indent);
            generateProperties(node);
            newline(--indent);
        }
        write("}");
    }

    private void generateProperties(Node node) {
        int i = 0;
        for (Map.Entry<String, Node> entry : node.getMap().entrySet()) {
            generateComment(entry.getValue());
            write("\"", escape(entry.getKey()) + "\": ", entry.getValue());
            if (i++ != node.getMap().size() - 1) {
                write(",");
                newline(indent);
            }
        }
    }

}
