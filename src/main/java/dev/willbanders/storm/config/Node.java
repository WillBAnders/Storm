package dev.willbanders.storm.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.willbanders.storm.serializer.SerializationException;
import dev.willbanders.storm.serializer.Serializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A node in the configuration hierarchy used for representing values.
 *
 * <p>The design of this class is based around dynamic (but strong) typing to
 * balance ease of use with type safety. The majority of methods can throw
 * exceptions as a result, and users of this class must ensure that these
 * requirements are met. The serializer system is built on top of this to
 * provide additional type safety.</p>
 *
 * <p>The node hierarchy is navigable and supports accessing nodes which are not
 * present in the config. These nodes are considered virtual, are unattached,
 * and have an {@link Type#UNDEFINED} value. Nodes that are present in the
 * config are considered attached and have a defined value.</p>
 */
public final class Node {

    /**
     * Defines the available primitive types supported by Storm, as well as
     * {@link #UNDEFINED} for unattached nodes.
     */
    public enum Type {
        NULL,
        BOOLEAN,
        INTEGER,
        DECIMAL,
        CHARACTER,
        STRING,
        ARRAY,
        OBJECT,
        UNDEFINED
    }

    private Node parent;
    private Object key;
    private String comment = "";
    private Type type;
    private Object value = null;

    private Node(Node parent, Object key, boolean attached) {
        this.parent = parent;
        this.key = key;
        this.type = attached ? Type.NULL : Type.UNDEFINED;
    }

    /**
     * Returns a new root node which is currently {@link Type#UNDEFINED}. The
     * root node does not have a parent or a key.
     */
    public static Node root() {
        return new Node(null, null, false);
    }

    /**
     * Returns {@code true} if the node is the root node. The root node does not
     * have a parent or a key.
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Returns the parent of this node, provided it is not the root.
     *
     * @see #isRoot()
     */
    public Node getParent() {
        Preconditions.checkState(!isRoot(), "Cannot get parent from root node.");
        return parent;
    }

    /**
     * Returns the key of this node, provided it is not the root. The key is
     * either an {@link Integer} for array elements and a {@link String} for
     * object children.
     *
     * @see #isRoot()
     */
    public Object getKey() {
        Preconditions.checkState(!isRoot(), "Cannot get key from root node.");
        return key;
    }

    /**
     * Returns the path to this node from the root, consisting of the keys of
     * nodes up to and including this one. The root's path is the empty list.
     */
    public List<Object> getPath() {
        Node node = this;
        List<Object> path = Lists.newArrayList();
        while (!node.isRoot()) {
            path.add(node.key);
            node = node.parent;
        }
        return Lists.reverse(path);
    }

    /**
     * Returns the comment for this node. An empty string represents no comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment for this node. An empty string represents no comment.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Returns the {@link Type} of this node.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the value of this node as a object, provided it is attached.
     * The available {@link Type}s and returned classes are as follows:
     *
     * <ul>
     *     <li>NULL: {@code null}</li>
     *     <li>BOOLEAN: {@link Boolean}</li>
     *     <li>INTEGER: {@link BigInteger} (guaranteed infinite precision)</li>
     *     <li>DECIMAL: {@link BigDecimal} (guaranteed infinite precision)</li>
     *     <li>CHARACTER: {@link Character}</li>
     *     <li>STRING: {@link String}</li>
     *     <li>ARRAY: {@link ArrayList} (guaranteed random access)</li>
     *     <li>OBJECT: {@link LinkedHashMap} (guaranteed iteration order)</li>
     *     <li>UNDEFINED: {@code throws} {@link IllegalStateException}</li>
     * </ul>
     *
     * @throws IllegalStateException if the node is not attached.
     */
    public Object getValue() {
        Preconditions.checkState(type != Type.UNDEFINED, "Cannot get value from unattached node.");
        switch (type) {
            case ARRAY:
                return Lists.newArrayList(Lists.transform((List<Node>) value, Node::getValue));
            case OBJECT:
                return Maps.newLinkedHashMap(Maps.transformValues((Map<String, Node>) value, Node::getValue));
            default:
                return value;
        }
    }

    /**
     * Sets the value of this node to the given object, provided it is attached.
     * The supported classes and corresponding types are as follows:
     *
     * <ul>
     *     <li>{@code null}: NULL</li>
     *     <li>{@link Boolean}: BOOLEAN</li>
     *     <li>{@link BigInteger}: INTEGER</li>
     *     <li>{@link BigDecimal}: DECIMAL</li>
     *     <li>{@link Character}: CHARACTER</li>
     *     <li>{@link String}: STRING</li>
     *     <li>{@link List}: ARRAY (with supported values</li>
     *     <li>{@link Map}: OBJECT (with string keys and supported values)</li>
     * </ul>
     *
     * <p>To remove the node entirely, see {@link #detach()}.</p>
     *
     * @throws IllegalArgumentException if the given value is unsupported
     * @throws IllegalStateException if the node is not attached
     * @see Type
     */
    public void setValue(Object value) {
        Preconditions.checkState(type != Type.UNDEFINED, "Cannot set value to unattached node.");
        if (value == null) {
            type = Type.NULL;
        } else if (value instanceof Boolean) {
            type = Type.BOOLEAN;
        } else if (value instanceof BigInteger) {
            type = Type.INTEGER;
        } else if (value instanceof BigDecimal) {
            type = Type.DECIMAL;
        } else if (value instanceof Character) {
            type = Type.CHARACTER;
        } else if (value instanceof String) {
            type = Type.STRING;
        } else if (value instanceof List) {
            List<Node> list = new ArrayList<>();
            for (Object element : (List<?>) value) {
                Node node = new Node(this, list.size(), true);
                node.setValue(element);
                list.add(node);
            }
            type = Type.ARRAY;
            value = list;
        } else if (value instanceof Map) {
            Map<String, Node> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                Preconditions.checkState(entry.getKey() instanceof String, "Unsupported map key type " + value.getClass().getName() + ".");
                Node node = new Node(this, entry.getKey(), true);
                node.setValue(entry.getValue());
                map.put((String) entry.getKey(), node);
            }
            type = Type.OBJECT;
            value = map;
        } else {
            throw new IllegalArgumentException("Unsupported value type " + value.getClass().getName() + ".");
        }
        this.value = value;
    }

    /**
     * Returns a list of children for this node, provided it is an {@link
     * Type#ARRAY}. The returned list is unmodifiable.
     */
    public List<Node> getList() {
        Preconditions.checkState(type == Type.ARRAY, "Cannot get list from value of type " + type + ".");
        return Collections.unmodifiableList((List<Node>) value);
    }

    /**
     * Returns a map of children for this node, provided it is an {@link
     * Type#OBJECT}. The returned map is unmodifiable.
     */
    public Map<String, Node> getMap() {
        Preconditions.checkState(type == Type.OBJECT, "Cannot get map from value of type " + type + ".");
        return Collections.unmodifiableMap((Map<String, Node>) value);
    }

    /**
     * Returns a collection of children for this node, provided it is either an
     * {@link Type#ARRAY} or {@link Type#OBJECT}. The returned collection is
     * unmodifiable.
     */
    public Collection<Node> getChildren() {
        switch (type) {
            case ARRAY: return getList();
            case OBJECT: return getMap().values();
            default: throw new IllegalStateException("Cannot get children from value of type " + type + ".");
        }
    }

    /**
     * Returns a node located at the given path relative to this node. If the
     * path does not exist in the current structure, a virtual node is returned
     * which is unattached and has an undefined value.
     *
     * <p>Each element in the path is a key and must be either a non-negative
     * {@link Integer} for array values or a {@link String} for map values.</p>
     *
     * @throws IllegalArgumentException if a key in the path is invalid.
     */
    public Node resolve(Object... path) {
        Node node = this;
        for (Object key : path) {
            Node child = null;
            if (key instanceof Integer) {
                Preconditions.checkState((int) key >= 0, "Index must be non-negative.");
                if (node.getType() == Type.ARRAY && (int) key < node.getList().size()) {
                    child = node.getList().get((int) key);
                }
            } else if (key instanceof String) {
                if (node.getType() == Type.OBJECT) {
                    child = node.getMap().get(key);
                }
            } else {
                throw new IllegalArgumentException("Key must be an integer or string.");
            }
            node = child != null ? child : new Node(node, key, false);
        }
        return node;
    }

    /**
     * Ensures this node is attached to the node hierarchy. If the node was
     * previously unattached, the node's value will be {@link Type#NULL}.
     *
     * <p>This process will create intermediate nodes necessary to attach this
     * node, provided it does not overwrite any existing values. For arrays,
     * this includes inserting nodes at earlier indices to prevent holes.</p>
     *
     * @throws IllegalStateException if attaching would overwrite values.
     */
    public Node attach() {
        if (type != Type.UNDEFINED) {
            return this;
        } else if (parent != null) {
            parent.attachChild(this);
        }
        type = Type.NULL;
        value = null;
        return this;
    }

    private void attachChild(Node child) {
        Preconditions.checkState(resolve(child.key).type == Type.UNDEFINED, "A node is already attached at this path.");
        if (type == Type.UNDEFINED) {
            if (parent != null) {
                parent.attachChild(this);
            }
            if (child.key instanceof Integer) {
                type = Type.ARRAY;
                value = new ArrayList<>();
            } else if (child.key instanceof String) {
                type = Type.OBJECT;
                value = new LinkedHashMap<>();
            } else {
                throw new AssertionError();
            }
        }
        switch (type) {
            case ARRAY:
                Preconditions.checkState(child.key instanceof Integer, "Cannot attach object child to array value.");
                List<Node> list = (List<Node>) value;
                while (list.size() < (int) child.key) {
                    list.add(new Node(this, list.size(), true));
                }
                list.add(child);
                break;
            case OBJECT:
                Preconditions.checkState(child.key instanceof String, "Cannot attach array child to object value.");
                ((Map<String, Node>) value).put((String) child.key, child);
                break;
            default:
                throw new IllegalStateException("Cannot attach node to value of type " + parent.type + ".");
        }
    }

    /**
     * Ensures this node is detached from the node hierarchy. After being
     * detached, the node's value will be {@link Type#UNDEFINED}.
     *
     * <p>For arrays, this process includes shifting any subsequent nodes in the
     * array to fill the hole left by removal.</p>
     */
    public Node detach() {
        if (type == Type.UNDEFINED) {
            return this;
        } else if (parent != null) {
            switch (parent.type) {
                case ARRAY:
                    List<Node> list = (List<Node>) parent.value;
                    list.remove((int) key);
                    for (int i = (int) key; i < list.size(); i++) {
                        list.get(i).key = i;
                    }
                    break;
                case OBJECT:
                    ((Map) parent.value).remove(key);
                    break;
                default:
                    throw new AssertionError();
            }
        }
        type = Type.UNDEFINED;
        value = null;
        return this;
    }

    /**
     * Returns a node located at the given path relative to this node. The path
     * is split on {@code .}, as in {@code first.second.third}. Array indices
     * cannot be represented with this method.
     *
     * @see #resolve(Object...)
     */
    public Node get(String path) {
        return resolve((Object[]) path.split("\\."));
    }

    /**
     * Deserializes a value from this node using the given serializer.
     *
     * @throws SerializationException if the node could not be deserialized
     * @see Serializer#deserialize(Node)
     */
    public <T> T get(Serializer<T> serializer) throws SerializationException {
        return serializer.deserialize(this);
    }

    /**
     * Deserializes a value from the node located at the given path relative
     * to this node using the given serializer.
     *
     * @throws SerializationException if the node could not be deserialized
     * @see #get(String)
     * @see #get(Serializer)
     */
    public <T> T get(String path, Serializer<T> serializer) throws SerializationException {
        return get(path).get(serializer);
    }

    /**
     * Reserializes the value to this node using the given serializer.
     *
     * @throws SerializationException if the node could not be serialized
     * @see Serializer#reserialize(Node, Object) 
     */
    public <T> void set(T value, Serializer<T> serializer) throws SerializationException {
        serializer.reserialize(this, value);
    }

    /**
     * Reserializes the value to the node located at the given path relative to
     * this node using the given serializer.
     *
     * @throws SerializationException if the node could not be serialized
     * @see #get(String)
     * @see #set(Object, Serializer)
     */
    public <T> void set(String path, T value, Serializer<T> serializer) throws SerializationException {
        get(path).set(value, serializer);
    }

}
