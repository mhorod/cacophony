package cacophony.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrintTreeTest {
    companion object {
        class TestTree(
            val value: String,
            val children: List<TestTree>,
        ) : Tree {
            override fun stringValue() = value

            override fun isLeaf() = children.isEmpty()

            override fun children() = children
        }
    }

    @Test
    fun `set of elements reachable from empty set is empty`() {
        val testTree =
            TestTree(
                "Root",
                listOf(
                    TestTree(
                        "Child1",
                        listOf(
                            TestTree("Grandchild1", emptyList()),
                            TestTree("Grandchild2", emptyList()),
                            TestTree(
                                "Grandchild3",
                                listOf(
                                    TestTree("Greatgrandchild1", emptyList()),
                                    TestTree("Greatgrandchild2", emptyList()),
                                    TestTree("Greatgrandchild3", emptyList()),
                                ),
                            ),
                        ),
                    ),
                    TestTree("Child2", emptyList()),
                ),
            )
        assertEquals(
            TreePrinter(StringBuilder()).printTree(testTree),
            """
            └┬Root
             ├┬Child1
             │├—Grandchild1
             │├—Grandchild2
             │└┬Grandchild3
             │ ├—Greatgrandchild1
             │ ├—Greatgrandchild2
             │ └—Greatgrandchild3
             └—Child2
            
            """.trimIndent(),
        )
    }
}
