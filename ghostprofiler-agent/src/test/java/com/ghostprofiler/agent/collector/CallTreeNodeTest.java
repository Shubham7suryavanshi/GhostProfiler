package com.ghostprofiler.agent.collector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CallTreeNode}.
 */
class CallTreeNodeTest {

    private CallTreeNode node;

    @BeforeEach
    void setUp() {
        node = new CallTreeNode("com.example.Service#doWork", System.nanoTime());
    }

    @Test
    void id_isUniquePerInstance() {
        CallTreeNode another = new CallTreeNode("com.example.Service#other", System.nanoTime());
        assertNotEquals(node.getId(), another.getId());
    }

    @Test
    void durationIsNegativeBeforeComplete() {
        assertEquals(-1L, node.getDurationNanos());
    }

    @Test
    void complete_setsDurationCorrectly() {
        long start = node.getStartNanos();
        long exit = start + 5_000_000L; // 5 ms in nanos
        node.complete(exit, false);
        assertEquals(5_000_000L, node.getDurationNanos());
        assertFalse(node.isThrewException());
    }

    @Test
    void complete_withException_setsFlag() {
        node.complete(node.getStartNanos() + 1_000L, true);
        assertTrue(node.isThrewException());
    }

    @Test
    void addChild_appendsChild() {
        CallTreeNode child = new CallTreeNode("com.example.Repo#findById", System.nanoTime());
        node.addChild(child);
        assertEquals(1, node.getChildren().size());
        assertSame(child, node.getChildren().get(0));
    }

    @Test
    void getChildren_returnsUnmodifiableView() {
        CallTreeNode child = new CallTreeNode("com.example.Repo#findById", System.nanoTime());
        node.addChild(child);
        assertThrows(UnsupportedOperationException.class,
                () -> node.getChildren().add(child));
    }
}
