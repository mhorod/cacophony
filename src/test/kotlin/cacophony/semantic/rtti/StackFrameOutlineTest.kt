package cacophony.semantic.rtti

import cacophony.*
import cacophony.controlflow.CFGNode
import cacophony.controlflow.functions.FunctionHandler
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.emptyList

class StackFrameOutlineTest {
    val functionHandler: FunctionHandler = mockk()

    val functionDefinition: FunctionDefinition = mockk()

    @BeforeEach
    fun setUp() {
        every { functionHandler.getFunctionDeclaration() } returns functionDefinition
        every { functionDefinition.getLabel() } returns "f"
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `empty stack outline`() {
        every { functionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 0 }
        every { functionHandler.getReferenceAccesses() } returns emptyList()

        val outline = generateStackFrameOutline(functionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 0")
    }

    @Test
    fun `small stack no refs`() {
        every { functionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 1 }
        every { functionHandler.getReferenceAccesses() } returns emptyList()

        val outline = generateStackFrameOutline(functionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 1, 0")
    }

    @Test
    fun `small stack with refs`() {
        every { functionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 1 }
        every { functionHandler.getReferenceAccesses() } returns listOf(0)

        val outline = generateStackFrameOutline(functionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 1, 1")
    }

    @Test
    fun `large stack with no refs`() {
        every { functionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 132 }
        every { functionHandler.getReferenceAccesses() } returns emptyList()

        val outline = generateStackFrameOutline(functionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 132, 0, 0, 0")
    }

    @Test
    fun `large stack with refs`() {
        every { functionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 116 }
        every { functionHandler.getReferenceAccesses() } returns listOf(0, 8 * 1, 8 * 100, 8 * 115)

        val outline = generateStackFrameOutline(functionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 116, 3, ${(1UL shl (100 - 64)) or (1UL shl (115 - 64))}")
    }

    @Test
    fun `throws on negative offset`() {
        every { functionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 116 }
        every { functionHandler.getReferenceAccesses() } returns listOf(0, -1, 100, 115)

        assertThatThrownBy { generateStackFrameOutline(functionHandler) }
    }

    @Test
    fun `throws on misaligned offset`() {
        every { functionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 116 }
        every { functionHandler.getReferenceAccesses() } returns listOf(0, 1, 100, 115)

        assertThatThrownBy { generateStackFrameOutline(functionHandler) }
    }
}
