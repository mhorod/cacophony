package cacophony.codegen.linearization

import cacophony.codegen.instructions.Instruction
import cacophony.codegen.instructions.InstructionCovering
import cacophony.codegen.linearization.linearize
import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGVertex
import cacophony.semantic.syntaxtree.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.*

class LinearizationTest {
    @MockK
    lateinit var covering: InstructionCovering

    init {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `correctly handles linear CFG`() {
        // v1 -> v2 -> v3 -> v4
        val l1 = CFGLabel()
        val l2 = CFGLabel()
        val l3 = CFGLabel()
        val l4 = CFGLabel()

        val i1 = spyk(listOf<Instruction>(mockk()))
        val i2 = spyk(listOf<Instruction>(mockk()))
        val i3 = spyk(listOf<Instruction>(mockk()))
        val i4 = spyk(listOf<Instruction>(mockk()))

        val v1 =
            mockk<CFGVertex.Jump> {
                every { destination } returns l2
            }
        val v2 =
            mockk<CFGVertex.Jump> {
                every { destination } returns l3
            }
        val v3 =
            mockk<CFGVertex.Jump> {
                every { destination } returns l4
            }
        val v4 = mockk<CFGVertex.Final>()

        every { covering.coverWithInstructions(v1.tree) } returns i1
        every { covering.coverWithInstructions(v2.tree) } returns i2
        every { covering.coverWithInstructions(v3.tree) } returns i3
        every { covering.coverWithInstructions(v4.tree) } returns i4

        val cfg =
            CFGFragment(
                mapOf(
                    l1 to v1,
                    l2 to v2,
                    l3 to v3,
                    l4 to v4,
                ),
                l1,
            )

        val linear = linearize(cfg, covering)

        assertThat(linear[0].label().name).isEqualTo("bb0")
        assertThat(linear[0].successors()).isEqualTo(mutableSetOf(linear[1]))
        assertThat(linear[0].predecessors()).isEqualTo(mutableSetOf<BasicBlock>())
        assertThat(linear[0].instructions()).isEqualTo(i1)

        assertThat(linear[1].label().name).isEqualTo("bb1")
        assertThat(linear[1].successors()).isEqualTo(mutableSetOf(linear[2]))
        assertThat(linear[1].predecessors()).isEqualTo(mutableSetOf(linear[0]))
        assertThat(linear[1].instructions()).isEqualTo(i2)

        assertThat(linear[2].label().name).isEqualTo("bb2")
        assertThat(linear[2].successors()).isEqualTo(mutableSetOf(linear[3]))
        assertThat(linear[2].predecessors()).isEqualTo(mutableSetOf(linear[1]))
        assertThat(linear[2].instructions()).isEqualTo(i3)

        assertThat(linear[3].label().name).isEqualTo("bb3")
        assertThat(linear[3].successors()).isEqualTo(mutableSetOf<BasicBlock>())
        assertThat(linear[3].predecessors()).isEqualTo(mutableSetOf(linear[2]))
        assertThat(linear[3].instructions()).isEqualTo(i4)
    }

    @Test
    fun `correctly handles cycle CFG`() {
        // v1 -> v2 -> v3 -> v1
        val l1 = CFGLabel()
        val l2 = CFGLabel()
        val l3 = CFGLabel()

        val i1 = spyk(listOf<Instruction>(mockk()))
        val i2 = spyk(listOf<Instruction>(mockk()))
        val i3 = spyk(listOf<Instruction>(mockk()))

        val v1 =
            mockk<CFGVertex.Jump> {
                every { destination } returns l2
            }
        val v2 =
            mockk<CFGVertex.Jump> {
                every { destination } returns l3
            }
        val v3 =
            mockk<CFGVertex.Jump> {
                every { destination } returns l1
            }

        every { covering.coverWithInstructions(v1.tree) } returns i1
        every { covering.coverWithInstructions(v2.tree) } returns i2
        every { covering.coverWithInstructionsAndJump(v3.tree, match { it.name == "bb0" }) } returns i3

        val cfg =
            CFGFragment(
                mapOf(
                    l1 to v1,
                    l2 to v2,
                    l3 to v3,
                ),
                l1,
            )

        val linear = linearize(cfg, covering)

        assertThat(linear[0].label().name).isEqualTo("bb0")
        assertThat(linear[0].successors()).isEqualTo(mutableSetOf(linear[1]))
        assertThat(linear[0].predecessors()).isEqualTo(mutableSetOf(linear[2]))
        assertThat(linear[0].instructions()).isEqualTo(i1)

        assertThat(linear[1].label().name).isEqualTo("bb1")
        assertThat(linear[1].successors()).isEqualTo(mutableSetOf(linear[2]))
        assertThat(linear[1].predecessors()).isEqualTo(mutableSetOf(linear[0]))
        assertThat(linear[1].instructions()).isEqualTo(i2)

        assertThat(linear[2].label().name).isEqualTo("bb2")
        assertThat(linear[2].successors()).isEqualTo(mutableSetOf(linear[0]))
        assertThat(linear[2].predecessors()).isEqualTo(mutableSetOf(linear[1]))
        assertThat(linear[2].instructions()).isEqualTo(i3)
    }

    @Test
    fun `correctly handles all conditionals`() {
        // v1 f-> v2, v1 t-> v3 | both new
        // v2 f-> v1, v2 t-> v3 | true new, false old
        // v3 f-> v4, v3 t-> v3 | true old, false new
        // v4 f-> v1, v4 t-> v2 | both old
        val l1 = CFGLabel()
        val l2 = CFGLabel()
        val l3 = CFGLabel()
        val l4 = CFGLabel()

        val i1 = spyk(listOf<Instruction>(mockk()))
        val i2 = spyk(listOf<Instruction>(mockk()))
        val i3 = spyk(listOf<Instruction>(mockk()))
        val i4 = spyk(listOf<Instruction>(mockk()))
        val i5 = spyk(listOf<Instruction>(mockk()))

        val v1 =
            mockk<CFGVertex.Conditional> {
                every { falseDestination } returns l2
                every { trueDestination } returns l3
            }

        val node = mockk<CFGNode>()
        val v2 =
            mockk<CFGVertex.Conditional> {
                every { falseDestination } returns l1
                every { trueDestination } returns l3
                every { tree } returns node
            }
        val v3 =
            mockk<CFGVertex.Conditional> {
                every { falseDestination } returns l4
                every { trueDestination } returns l3
            }
        val v4 =
            mockk<CFGVertex.Conditional> {
                every { falseDestination } returns l1
                every { trueDestination } returns l2
            }

        every { covering.coverWithInstructionsAndJump(v1.tree, match { it.name == "bb2" }) } returns i1
        every {
            covering.coverWithInstructionsAndJump(CFGNode.LogicalNot(node), match { it.name == "bb0" })
        } returns i2
        every { covering.coverWithInstructionsAndJump(v3.tree, match { it.name == "bb2" }) } returns i3
        every { covering.coverWithInstructionsAndJump(v4.tree, match { it.name == "bb1" }) } returns i4
        every { covering.coverWithInstructionsAndJump(CFGNode.Constant(42), match { it.name == "bb0" }) } returns i5

        val cfg =
            CFGFragment(
                mapOf(
                    l1 to v1,
                    l2 to v2,
                    l3 to v3,
                    l4 to v4,
                ),
                l1,
            )

        val linear = linearize(cfg, covering)

        assertThat(linear.size).isEqualTo(4)

        assertThat(linear[0].label().name).isEqualTo("bb0")
        assertThat(linear[0].successors()).isEqualTo(mutableSetOf(linear[1], linear[2]))
        assertThat(linear[0].predecessors()).isEqualTo(mutableSetOf(linear[1], linear[3]))
        assertThat(linear[0].instructions()).isEqualTo(i1)

        assertThat(linear[1].label().name).isEqualTo("bb1")
        assertThat(linear[1].successors()).isEqualTo(mutableSetOf(linear[0], linear[2]))
        assertThat(linear[1].predecessors()).isEqualTo(mutableSetOf(linear[0], linear[3]))
        assertThat(linear[1].instructions()).isEqualTo(i2)

        assertThat(linear[2].label().name).isEqualTo("bb2")
        assertThat(linear[2].successors()).isEqualTo(mutableSetOf(linear[2], linear[3]))
        assertThat(linear[2].predecessors()).isEqualTo(mutableSetOf(linear[0], linear[1], linear[2]))
        assertThat(linear[2].instructions()).isEqualTo(i3)

        assertThat(linear[3].label().name).isEqualTo("bb3")
        assertThat(linear[3].successors()).isEqualTo(mutableSetOf(linear[0], linear[1]))
        assertThat(linear[3].predecessors()).isEqualTo(mutableSetOf(linear[2]))
        assertThat(linear[3].instructions()).isEqualTo(i4 + i5)
    }
}
