package dev.willbanders.storm.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

class NodeTests {

    private final Node root = Node.root();

    @Test
    void testIsRoot() {
        Assertions.assertAll(
                () -> Assertions.assertTrue(root.isRoot()),
                () -> Assertions.assertFalse(root.resolve(0).isRoot()),
                () -> Assertions.assertFalse(root.resolve("child").isRoot())
        );
    }

    @Test
    void testGetParent() {
        Assertions.assertAll(
                () -> Assertions.assertThrows(RuntimeException.class, root::getParent),
                () -> Assertions.assertSame(root, root.resolve(0).getParent()),
                () -> Assertions.assertSame(root, root.resolve("child").getParent())
        );
    }

    @Test
    void testGetKey() {
        Assertions.assertAll(
                () -> Assertions.assertThrows(RuntimeException.class, root::getKey),
                () -> Assertions.assertEquals(0, root.resolve(0).getKey()),
                () -> Assertions.assertEquals("child", root.resolve("child").getKey())
        );
    }

    @Test
    void testGetPath() {
        Assertions.assertAll(
                () -> Assertions.assertEquals(ImmutableList.of(), root.getPath()),
                () -> Assertions.assertEquals(ImmutableList.of(0), root.resolve(0).getPath()),
                () -> Assertions.assertEquals(ImmutableList.of("child"), root.resolve("child").getPath()),
                () -> Assertions.assertEquals(ImmutableList.of(0, "child"), root.resolve(0, "child").getPath())
        );
    }

    @Nested
    class SetValueTests {

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.config.NodeTests#testSetValueValid")
        void testSetValueValid(String test, Node.Type type, Object value) {
            root.attach().setValue(value);
            Assertions.assertAll(
                    () -> Assertions.assertEquals(type, root.getType()),
                    () -> Assertions.assertEquals(value, root.getValue())
            );
        }

        @ParameterizedTest
        @MethodSource("dev.willbanders.storm.config.NodeTests#testSetValueInvalid")
        void testSetValueInvalid(String test, Object value) {
            Assertions.assertThrows(RuntimeException.class, () -> root.attach().setValue(value));
        }

        @Test
        void testSetValueUndefined() {
            Assertions.assertAll(
                    () -> Assertions.assertEquals(Node.Type.UNDEFINED, root.getType()),
                    () -> Assertions.assertThrows(RuntimeException.class, root::getValue),
                    () -> Assertions.assertThrows(RuntimeException.class, () -> root.setValue("unused"))
            );
        }

    }

    private static Stream<Arguments> testSetValueValid() {
        return Stream.of(
                Arguments.of("null", Node.Type.NULL, null),
                Arguments.of("Boolean", Node.Type.BOOLEAN, true),
                Arguments.of("BigInteger", Node.Type.INTEGER, BigInteger.ZERO),
                Arguments.of("BigDecimal", Node.Type.DECIMAL, BigDecimal.ONE),
                Arguments.of("Character", Node.Type.CHARACTER, 'c'),
                Arguments.of("String", Node.Type.STRING, "string"),
                Arguments.of("List", Node.Type.ARRAY, ImmutableList.of("element")),
                Arguments.of("Map", Node.Type.OBJECT, ImmutableMap.of("key", "value"))
        );
    }

    private static Stream<Object> testSetValueInvalid() {
        return Stream.of(
                Arguments.of("Integer", 0),
                Arguments.of("Double", 0.0),
                Arguments.of("Array", new Object[] {}),
                Arguments.of("Set", ImmutableSet.of("element")),
                Arguments.of("Map<Integer, ...>", ImmutableMap.of(0, "value")),
                Arguments.of("LocalDateTime", LocalDateTime.now())
        );
    }

    @Nested
    class GetListTests {

        @Test
        void testGetListValid() {
            root.attach().setValue(ImmutableList.of("element"));
            Assertions.assertEquals(1, root.getList().size());
            Node child = root.getList().get(0);
            Assertions.assertAll(
                    () -> Assertions.assertSame(root, child.getParent()),
                    () -> Assertions.assertEquals(0, child.getKey()),
                    () -> Assertions.assertEquals("element", child.getValue())
            );
        }

        @Test
        void testGetListInvalid() {
            root.attach().setValue("not-a-list");
            Assertions.assertThrows(RuntimeException.class, root::getList);
        }

        @Test
        void testGetListUndefined() {
            Assertions.assertThrows(RuntimeException.class, root::getList);
        }

    }

    @Nested
    class GetMapTests {

        @Test
        void testGetMapValid() {
            root.attach().setValue(ImmutableMap.of("key", "value"));
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1, root.getMap().size()),
                    () -> Assertions.assertTrue(root.getMap().containsKey("key"))
            );
            Node child = root.getMap().get("key");
            Assertions.assertAll(
                    () -> Assertions.assertSame(root, child.getParent()),
                    () -> Assertions.assertEquals("key", child.getKey()),
                    () -> Assertions.assertEquals("value", child.getValue())
            );
        }

        @Test
        void testGetMapInvalid() {
            root.attach().setValue("not-a-map");
            Assertions.assertThrows(RuntimeException.class, root::getMap);
        }

        @Test
        void testGetMapUndefined() {
            Assertions.assertThrows(RuntimeException.class, root::getMap);
        }

    }

    @Nested
    class GetChildrenTests {

        @Test
        void testGetChildrenList() {
            root.attach().setValue(ImmutableList.of("element"));
            Assertions.assertEquals(1, root.getChildren().size());
            Node child = root.getChildren().iterator().next();
            Assertions.assertAll(
                    () -> Assertions.assertSame(root, child.getParent()),
                    () -> Assertions.assertEquals(0, child.getKey()),
                    () -> Assertions.assertEquals("element", child.getValue())
            );
        }

        @Test
        void testGetChildrenMap() {
            root.attach().setValue(ImmutableMap.of("key", "value"));
            Assertions.assertEquals(1, root.getChildren().size());
            Node child = root.getChildren().iterator().next();
            Assertions.assertAll(
                    () -> Assertions.assertSame(root, child.getParent()),
                    () -> Assertions.assertEquals("key", child.getKey()),
                    () -> Assertions.assertEquals("value", child.getValue())
            );
        }

        @Test
        void testGetChildrenInvalid() {
            root.attach().setValue("not-a-collection");
            Assertions.assertThrows(RuntimeException.class, root::getChildren);
        }

        @Test
        void testGetChildrenUndefined() {
            Assertions.assertThrows(RuntimeException.class, root::getChildren);
        }

    }

    @ParameterizedTest
    @MethodSource
    void testResolve(String test, List<Object> path) {
        Node resolved = root.resolve(path.toArray()).attach();
        Assertions.assertAll(
                () -> Assertions.assertEquals(path, resolved.getPath()),
                () -> Assertions.assertSame(resolved, root.resolve(path.toArray()))
        );
    }

    private static Stream<Object> testResolve() {
        return Stream.of(
                Arguments.of("Empty", ImmutableList.of()),
                Arguments.of("Array", ImmutableList.of(0)),
                Arguments.of("Nested Arrays", ImmutableList.of(0, 1, 2)),
                Arguments.of("Object", ImmutableList.of("child")),
                Arguments.of("Nested Objects", ImmutableList.of("first", "second", "third")),
                Arguments.of("Mixed", ImmutableList.of(0, "child")),
                Arguments.of("Repeated Keys", ImmutableList.of("child", "child"))
        );
    }

    @Nested
    class AttachTests {

        @Test
        void testAttachRoot() {
            root.attach();
            Assertions.assertNull(root.getValue());
        }

        @Test
        void testAttachList() {
            Node child = root.resolve(1).attach();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(Node.Type.ARRAY, root.getType()),
                    () -> Assertions.assertNull(root.resolve(0).getValue()),
                    () -> Assertions.assertSame(child, root.resolve(1)),
                    () -> Assertions.assertNull(child.getValue())
            );
        }

        @Test
        void testAttachMap() {
            Node one = root.resolve("one").attach();
            Node two = root.resolve("two").attach();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(Node.Type.OBJECT, root.getType()),
                    () -> Assertions.assertSame(one, root.resolve("one")),
                    () -> Assertions.assertNull(one.getValue()),
                    () -> Assertions.assertSame(two, root.resolve("two")),
                    () -> Assertions.assertNull(two.getValue())
            );
        }

        @Test
        void testAttachAlreadyAttached() {
            Node unattached = root.resolve("child");
            Node attached = root.resolve("child").attach();
            Assertions.assertAll(
                    () -> Assertions.assertNotSame(unattached, attached),
                    () -> Assertions.assertThrows(RuntimeException.class, root::attach),
                    () -> Assertions.assertThrows(RuntimeException.class, unattached::attach),
                    () -> Assertions.assertThrows(RuntimeException.class, attached::attach)
            );
        }

    }

    @Nested
    class DetachTests {

        @Test
        void testDetachRoot() {
            root.attach().detach();
            Assertions.assertEquals(Node.Type.UNDEFINED, root.getType());
        }

        @Test
        void testDetachList() {
            Node first = root.resolve(0).attach();
            Node second = root.resolve(1).attach();
            first.detach();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1, root.getList().size()),
                    () -> Assertions.assertEquals(Node.Type.UNDEFINED, first.getType()),
                    () -> Assertions.assertEquals(second, root.resolve(0)),
                    () -> Assertions.assertEquals(0, second.getKey())
            );
        }

        @Test
        void testDetachMap() {
            Node one = root.resolve("one").attach();
            Node two = root.resolve("two").attach().detach();
            Assertions.assertAll(
                    () -> Assertions.assertEquals(1, root.getMap().size()),
                    () -> Assertions.assertSame(one, root.resolve("one")),
                    () -> Assertions.assertNotSame(two, root.resolve("two")),
                    () -> Assertions.assertEquals(Node.Type.UNDEFINED, two.getType())
            );
        }

        @Test
        void testDetachUnattached() {
            Assertions.assertAll(
                    () -> Assertions.assertThrows(RuntimeException.class, root::detach),
                    () -> Assertions.assertThrows(RuntimeException.class, () -> root.resolve("child").detach())
            );
        }

    }

}
