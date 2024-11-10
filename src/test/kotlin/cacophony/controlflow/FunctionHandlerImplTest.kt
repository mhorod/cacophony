package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.max

class FunctionHandlerImplTest {
    @Nested
    inner class GenerateCall {
        private fun mockFunDeclarationAndFunHandler(argumentCount: Int): FunctionHandlerImpl =
            FunctionHandlerImpl(
                Definition.FunctionDeclaration(
                    mockk(),
                    "fun def",
                    mockk(),
                    (1..argumentCount).map { mockk() },
                    mockk(),
                    mockk(),
                ),
                mockk(),
            )

        private fun getCallNodes(
            argumentCount: Int,
            result: Register?,
            alignStack: Boolean,
        ): List<CFGNode> {
            return mockFunDeclarationAndFunHandler(argumentCount).generateCall(
                (1..argumentCount).map { mockk() },
                result,
                alignStack,
            )
        }

        private fun getArgumentRegisters(callNodes: List<CFGNode>): List<X64Register> {
            val returnList = mutableListOf<X64Register>()
            for (node in callNodes) {
                if (node is CFGNode.Assignment && node.destination is Register.FixedRegister) {
                    val register = (node.destination as Register.FixedRegister).hardwareRegister
                    if (register != X64Register.RSP) {
                        returnList.add(register)
                    }
                }
            }
            return returnList
        }

        private fun getPushCount(callNodes: List<CFGNode>): Int {
            return callNodes.filterIsInstance<CFGNode.Push>().size
        }

        private fun getResultDestination(callNodes: List<CFGNode>): Register? {
            var register: Register? = null
            for (node in callNodes) {
                if (node is CFGNode.Assignment && node.value is CFGNode.VariableUse) {
                    val reg = (node.value as CFGNode.VariableUse).regvar
                    if (reg is Register.FixedRegister && reg.hardwareRegister == X64Register.RAX) {
                        assertThat(register).isNull()
                        register = node.destination
                    }
                }
            }
            return register
        }

        private fun getStackAlignmentAdded(callNodes: List<CFGNode>): Int? {
            var addedModulo: Int? = null
            var hasPopToRSP = false

            for (node in callNodes) {
                if (node is CFGNode.Assignment && node.destination is Register.FixedRegister) {
                    if ((node.destination as Register.FixedRegister).hardwareRegister != X64Register.RSP) {
                        continue
                    }
                    if (node.value is CFGNode.Addition) {
                        val lhs = (node.value as CFGNode.Addition).lhs
                        val rhs = (node.value as CFGNode.Addition).rhs
                        if (lhs !is CFGNode.VariableUse || lhs.regvar !is Register.FixedRegister ||
                            (lhs.regvar as Register.FixedRegister).hardwareRegister != X64Register.RSP
                        ) {
                            continue
                        }
                        // we could check whether rhs matches... but I do not think this is necessary
                        if (rhs !is CFGNode.Modulo) {
                            continue
                        }
                        val modulo = ((rhs.lhs as CFGNode.Addition).rhs as CFGNode.Constant).value
                        assertThat(addedModulo).isNull()
                        addedModulo = modulo
                    }
                }
                if (node is CFGNode.Pop && node.regvar is Register.FixedRegister) {
                    if ((node.regvar as Register.FixedRegister).hardwareRegister == X64Register.RSP) {
                        assertThat(hasPopToRSP).isFalse()
                        hasPopToRSP = true
                    }
                }
            }

            assertThat(addedModulo == null).isEqualTo(!hasPopToRSP)
            return addedModulo
        }

        @Test
        fun `function call argument count mismatch throws error`() {
            val handler = mockFunDeclarationAndFunHandler(1)

            assertThatThrownBy { handler.generateCall(listOf(), null) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `value is returned if requested and stack is aligned`() {
            val register = Register.VirtualRegister()
            val nodes = getCallNodes(0, register, true)
            assertThat(getResultDestination(nodes)).isEqualTo(register)
        }

        @Test
        fun `value is returned if requested and stack is not aligned`() {
            val register = Register.VirtualRegister()
            val nodes = getCallNodes(0, register, false)
            assertThat(getResultDestination(nodes)).isEqualTo(register)
        }

        @Test
        fun `value is not returned if not requested`() {
            val nodes = getCallNodes(0, null, true)
            assertThat(getResultDestination(nodes)).isNull()
        }

        @Test
        fun `stack is aligned if requested`() {
            assertThat(getStackAlignmentAdded(getCallNodes(0, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(1, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(2, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(3, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(4, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(5, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(6, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(7, null, true))).isEqualTo(8)
            assertThat(getStackAlignmentAdded(getCallNodes(8, null, true))).isEqualTo(0)
            assertThat(getStackAlignmentAdded(getCallNodes(9, null, true))).isEqualTo(8)
            assertThat(getStackAlignmentAdded(getCallNodes(10, null, true))).isEqualTo(0)
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 5, 6, 7, 8, 18])
        fun `stack is not aligned if not requested`(args: Int) {
            assertThat(getStackAlignmentAdded(getCallNodes(args, null, false))).isNull()
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 5, 6, 7, 8, 18])
        fun `excess arguments area passed on stack`(args: Int) {
            assertThat(getPushCount(getCallNodes(args, null, false))).isEqualTo(max(0, args - 6))
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 5, 6, 7, 8, 18])
        fun `up to first six arguments area passed via registers`(args: Int) {
            val expected =
                listOf(
                    X64Register.RDI,
                    X64Register.RSI,
                    X64Register.RDX,
                    X64Register.RCX,
                    X64Register.R8,
                    X64Register.R9,
                ).take(args)
            assertThat(getArgumentRegisters(getCallNodes(args, null, false))).isEqualTo(expected)
        }
    }
}
