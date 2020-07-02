package dev.willbanders.storm.serializer;

import dev.willbanders.storm.config.Node;

/**
 * An exception for serialization errors that contains the relevant node.
 */
public final class SerializationException extends RuntimeException {

    private final Node node;

    public SerializationException(Node node, String message) {
        super(message);
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

}
