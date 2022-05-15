package dev.willbanders.storm.format;

import com.google.common.base.Preconditions;
import dev.willbanders.storm.config.Node;

import java.io.PrintWriter;

public abstract class Generator {

    private final PrintWriter writer;
    protected int indent = 0;

    protected Generator(PrintWriter writer) {
        this.writer = writer;
    }

    public void generate(Node node) {
        if (node.isRoot()) {
            generateRoot(node);
        } else {
            generateNode(node);
        }
        writer.flush();
    }

    protected void generateRoot(Node root) {
        Preconditions.checkState(root.getType() != Node.Type.UNDEFINED, "Root node value is undefined.");
        generateNode(root);
    }

    protected void generateNode(Node node) {
        switch (node.getType()) {
            case NULL:
                generateNull(node);
                break;
            case BOOLEAN:
                generateBoolean(node);
                break;
            case INTEGER:
                generateInteger(node);
                break;
            case DECIMAL:
                generateDecimal(node);
                break;
            case CHARACTER:
                generateCharacter(node);
                break;
            case STRING:
                generateString(node);
                break;
            case ARRAY:
                generateArray(node);
                break;
            case OBJECT:
                generateObject(node);
        }
    }

    protected abstract void generateComment(Node node);

    protected void generateNull(Node node) {
        write(node.getValue());
    }

    protected void generateBoolean(Node node) {
        write(node.getValue());
    }

    protected void generateInteger(Node node) {
        write(node.getValue());
    }

    protected void generateDecimal(Node node) {
        write(node.getValue().toString().replace('E', 'e'));
    }

    protected void generateCharacter(Node node) {
        write("\'", node.getValue(), "\'");
    }

    protected void generateString(Node node) {
        write("\"", node.getValue(), "\"");
    }

    protected abstract void generateArray(Node node);

    protected abstract void generateObject(Node node);

    protected final void write(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Node) {
                generateNode((Node) object);
            } else {
                writer.print(object);
            }
        }
    }

    protected final void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.print("    ");
        }
    }

}
