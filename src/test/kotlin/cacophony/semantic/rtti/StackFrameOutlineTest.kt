package cacophony.semantic.rtti

import cacophony.controlflow.CFGNode
import cacophony.controlflow.functions.StaticFunctionHandler
import cacophony.semantic.syntaxtree.LambdaExpression
import io.mockk.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.collections.emptyList

class StackFrameOutlineTest {
    val staticFunctionHandler: StaticFunctionHandler = mockk()

    val function: LambdaExpression = mockk()

    @BeforeEach
    fun setUp() {
        every { staticFunctionHandler.getBodyReference() } returns function
        every { function.getLabel() } returns "f"
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `empty stack outline`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 0 }
        every { staticFunctionHandler.getReferenceAccesses() } returns emptyList()

        val outline = generateStackFrameOutline(staticFunctionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 0")
    }

    @Test
    fun `small stack no refs`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 8 }
        every { staticFunctionHandler.getReferenceAccesses() } returns emptyList()

        val outline = generateStackFrameOutline(staticFunctionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 1, 0")
    }

    @Test
    fun `small stack with refs`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 8 }
        every { staticFunctionHandler.getReferenceAccesses() } returns listOf(0)

        val outline = generateStackFrameOutline(staticFunctionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 1, 1")
    }

    @Test
    fun `large stack with no refs`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 8 * 132 }
        every { staticFunctionHandler.getReferenceAccesses() } returns emptyList()

        val outline = generateStackFrameOutline(staticFunctionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 132, 0, 0, 0")
    }

    @Test
    fun `large stack with refs`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 8 * 116 }
        every { staticFunctionHandler.getReferenceAccesses() } returns listOf(0, 8 * 1, 8 * 100, 8 * 115)

        val outline = generateStackFrameOutline(staticFunctionHandler)

        assertThat(outline).isEqualTo("frame_f: dq 116, 3, ${(1UL shl (100 - 64)) or (1UL shl (115 - 64))}")
    }

    @Test
    fun `throws on negative offset`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 8 * 116 }
        every { staticFunctionHandler.getReferenceAccesses() } returns listOf(0, -1, 100, 115)

        assertThatThrownBy { generateStackFrameOutline(staticFunctionHandler) }
    }

    @Test
    fun `throws on misaligned offset`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 8 * 116 }
        every { staticFunctionHandler.getReferenceAccesses() } returns listOf(0, 1, 100, 115)

        assertThatThrownBy { generateStackFrameOutline(staticFunctionHandler) }
    }

    @Test
    fun `throws on misaligned stack size`() {
        every { staticFunctionHandler.getStackSpace() } returns CFGNode.ConstantLazy { 7 }
        every { staticFunctionHandler.getReferenceAccesses() } returns listOf()

        assertThatThrownBy { generateStackFrameOutline(staticFunctionHandler) }
    }
}
