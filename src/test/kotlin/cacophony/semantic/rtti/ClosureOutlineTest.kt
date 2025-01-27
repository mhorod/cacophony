package cacophony.semantic.rtti

import cacophony.controlflow.Variable
import cacophony.controlflow.functions.ClosureHandler
import cacophony.semantic.syntaxtree.LambdaExpression
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClosureOutlineTest {
    val closureHandler: ClosureHandler = mockk()

    val lambdaExpression: LambdaExpression = mockk()

    @BeforeEach
    fun setUp() {
        every { closureHandler.getBodyReference() } returns lambdaExpression
        every { lambdaExpression.getLabel() } returns "f"
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `empty closure`() {
        every { closureHandler.getCapturedVariableOffsets() } returns emptyMap()

        val outline = generateClosureOutline(closureHandler)

        assertThat(outline).isEqualTo("closure_f: dq 0")
    }

    @Test
    fun `simple closure`() {
        val varA = Variable.PrimitiveVariable("a")
        val varB = Variable.PrimitiveVariable("b", true)
        every { closureHandler.getCapturedVariableOffsets() } returns
            mapOf(
                varA to 0,
                varB to 1,
            )

        val outline = generateClosureOutline(closureHandler)

        assertThat(outline).isEqualTo("closure_f: dq 2, 2")
    }

    @Test
    fun `closure with struct`() {
        val varA = Variable.PrimitiveVariable("a")
        val varB = Variable.PrimitiveVariable("b", true)
        val varC = Variable.PrimitiveVariable("c", true)
        val varD = Variable.PrimitiveVariable("d")
        val struct =
            Variable.StructVariable(
                mapOf(
                    "x" to varA,
                    "y" to
                        Variable.StructVariable(
                            mapOf(
                                "s" to varB,
                                "t" to varC,
                            ),
                        ),
                    "z" to varD,
                ),
            )
        TODO("Fix this")
//        every { closureHandler.getCapturedVariableOffsets() } returns
//            mapOf(
//                struct to 0,
//            )
//
//        val outline = generateClosureOutline(closureHandler)
//
//        assertThat(outline).isEqualTo("closure_f: dq 4, 6")
    }

    @Test
    fun `closure with a gap`() {
        val varA = Variable.PrimitiveVariable("a")
        val varB = Variable.PrimitiveVariable("b", true)
        val varC = Variable.PrimitiveVariable("c", true)
        val varD = Variable.PrimitiveVariable("d")
        val varE = Variable.PrimitiveVariable("e", true)
        val varF = Variable.PrimitiveVariable("f", true)
        every { closureHandler.getCapturedVariableOffsets() } returns
            mapOf(
                varA to 0,
                varB to 1,
                varC to 2,
                varD to 3,
                varE to 4,
                varF to 6,
            )

        val outline = generateClosureOutline(closureHandler)

        assertThat(outline).isEqualTo("closure_f: dq 7, ${6 + 16 + 64}")
    }

    @Test
    fun `large closure`() {
        val variableOffsets: MutableMap<Variable.PrimitiveVariable, Int> = mutableMapOf()
        for (i in 0..70) {
            val variable = Variable.PrimitiveVariable("x$i", true)
            variableOffsets[variable] = i
        }
        every { closureHandler.getCapturedVariableOffsets() } returns variableOffsets

        val outline = generateClosureOutline(closureHandler)

        assertThat(outline).isEqualTo("closure_f: dq 71, ${ULong.MAX_VALUE}, 127")
    }
}
