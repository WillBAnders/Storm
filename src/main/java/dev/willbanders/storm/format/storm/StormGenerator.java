package dev.willbanders.storm.format.storm;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.Generator;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

public final class StormGenerator extends Generator {

    private StormGenerator(PrintWriter writer) {
        super(writer);
    }

    public static void generate(Node node, PrintWriter writer) {
        new StormGenerator(writer).generate(node);
    }

    @Override
    protected void generateRoot(Node root) {
        generateComment(root);
        if (root.getType() == Node.Type.OBJECT) {
            if (!root.getComment().isEmpty()) {
                newline(indent);
            }
            generateProperties(root);
        } else {
            generateNode(root);
        }
    }

    @Override
    protected void generateComment(Node node) {
        if (!node.getComment().isEmpty()) {
            Arrays.stream(node.getComment().split("\n\r|\r\n|\n|\r")).forEach(c -> {
                write("//", c);
                newline(indent);
            });
        }
    }

    @Override
    protected void generateCharacter(Node node) {
        write("\'", escape(node.getValue().toString()), "\'");
    }

    @Override
    protected void generateString(Node node) {
        write("\"", escape(node.getValue().toString()), "\"");
    }

    private String escape(String string) {
        return string.replace("\\", "\\\\")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\'", "\\'")
                .replace("\"", "\\\"");
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
            write(entry.getKey(), " = ", entry.getValue());
            if (i++ != node.getMap().size() - 1) {
                newline(indent);
            }
        }
    }

}
