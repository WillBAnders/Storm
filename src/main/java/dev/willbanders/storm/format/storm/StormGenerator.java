package dev.willbanders.storm.format.storm;

import dev.willbanders.storm.config.Node;
import dev.willbanders.storm.format.Generator;

import java.io.PrintWriter;
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
        if (root.getType() == Node.Type.OBJECT) {
            generateProperties(root);
        } else {
            generateNode(root);
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
            write(entry.getKey(), " = ", entry.getValue());
            if (i++ != node.getMap().size() - 1) {
                newline(indent);
            }
        }
    }

}
