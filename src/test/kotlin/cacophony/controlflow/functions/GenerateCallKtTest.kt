package cacophony.controlflow.functions

import cacophony.controlflow.CFGNode
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.syntaxtree.Definition
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.max

class GenerateCallKtTest {
    private fun makeDefaultHandler(
        function: Definition.FunctionDefinition,
        analyzedFunction: AnalyzedFunction,
        ancestorFunctionHandlers: List<FunctionHandler> = emptyList(),
    ): FunctionHandlerImpl {
        val callConvention = mockk<CallConvention>()
        every { callConvention.preservedRegisters() } returns emptyList()
        return FunctionHandlerImpl(function, analyzedFunction, ancestorFunctionHandlers, callConvention)
    }

    private fun checkStaticLinkInGenerateCallFrom(callee: FunctionHandler, caller: FunctionHandler, expectedStaticLink: CFGNode) {
        mockkStatic(::generateCall)
        generateCallFrom(
            caller,
            callee.getFunctionDeclaration(),
            callee,
            emptyList(),
            null,
            false,
        )

        verify {
            generateCall(
                any(),
                listOf(
                    expectedStaticLink,
                ),
                any(),
                false,
            )
        }

        unmockkStatic(::generateCall)
    }

    private fun mockAnalyzedFunction(): AnalyzedFunction =
        run {
            val analyzedFunction = mockk<AnalyzedFunction>()
            val auxVariables = mutableSetOf<Variable.AuxVariable>()
            every { analyzedFunction.variables } returns emptySet()
            every { analyzedFunction.auxVariables } returns auxVariables
            every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
            every { analyzedFunction.declaredVariables() } returns emptyList()

            analyzedFunction
        }

    private fun mockFunDeclarationAndFunHandlerWithParents(argumentCount: Int, chainLength: Int): List<FunctionHandlerImpl> =
        run {
            val functionHandlers = mutableListOf<FunctionHandlerImpl>()
            for (i in 1..chainLength) {
                functionHandlers.add(
                    0,
                    makeDefaultHandler(
                        Definition.FunctionDefinition(
                            mockk(),
                            "fun def",
                            mockk(),
                            (1..argumentCount).map { mockk() },
                            mockk(),
                            mockk(),
                        ),
                        mockAnalyzedFunction(),
                        functionHandlers.toList(),
                    ),
                )
            }

            functionHandlers
        }

    private fun mockFunDeclarationAndFunHandler(argumentCount: Int): FunctionHandlerImpl =
        mockFunDeclarationAndFunHandlerWithParents(argumentCount, 1)[0]

    private fun getCallNodes(argumentCount: Int, result: Register?, alignStack: Boolean): List<CFGNode> =
        generateCall(
            mockFunDeclarationAndFunHandler(argumentCount).getFunctionDeclaration(),
            (1..argumentCount + 1).map { mockk() },
            result,
            alignStack,
        )

    private fun getArgumentRegisters(callNodes: List<CFGNode>): List<HardwareRegister> {
        val returnList = mutableListOf<HardwareRegister>()
        for (node in callNodes) {
            if (node is CFGNode.Assignment &&
                node.destination is CFGNode.RegisterUse &&
                (node.destination as CFGNode.RegisterUse).register is Register.FixedRegister
            ) {
                val reg = (node.destination as CFGNode.RegisterUse).register as Register.FixedRegister
                val register = reg.hardwareRegister
                if (register != HardwareRegister.RSP) {
                    returnList.add(register)
                }
            }
        }
        return returnList
    }

    private fun getPushCount(callNodes: List<CFGNode>): Int = callNodes.filterIsInstance<CFGNode.Push>().size

    private fun getResultDestination(callNodes: List<CFGNode>): CFGNode.LValue? {
        var register: CFGNode.LValue? = null
        for (node in callNodes) {
            if (node is CFGNode.Assignment && node.value is CFGNode.RegisterUse) {
                val reg = (node.value as CFGNode.RegisterUse).register
                if (reg is Register.FixedRegister && reg.hardwareRegister == HardwareRegister.RAX) {
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
            if (node is CFGNode.Assignment &&
                node.destination is CFGNode.RegisterUse &&
                (node.destination as CFGNode.RegisterUse).register is Register.FixedRegister
            ) {
                val reg = (node.destination as CFGNode.RegisterUse).register as Register.FixedRegister
                if (reg.hardwareRegister != HardwareRegister.RSP) {
                    continue
                }
                if (node.value is CFGNode.Addition) {
                    val lhs = (node.value as CFGNode.Addition).lhs
                    val rhs = (node.value as CFGNode.Addition).rhs
                    if (lhs !is CFGNode.RegisterUse ||
                        lhs.register !is Register.FixedRegister ||
                        (lhs.register as Register.FixedRegister).hardwareRegister != HardwareRegister.RSP
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
            if (node is CFGNode.Pop) {
                val registerUse = node.register as CFGNode.RegisterUse
                if (registerUse.register is Register.FixedRegister) {
                    if ((registerUse.register as Register.FixedRegister).hardwareRegister == HardwareRegister.RSP) {
                        assertThat(hasPopToRSP).isFalse()
                        hasPopToRSP = true
                    }
                }
            }
        }

        assertThat(addedModulo == null).isEqualTo(!hasPopToRSP)
        return addedModulo
    }

    @Test
    fun `function call argument count mismatch throws error`() {
        val handler = mockFunDeclarationAndFunHandler(1)
        val caller = mockFunDeclarationAndFunHandler(0)

        assertThatThrownBy { generateCallFrom(caller, handler.getFunctionDeclaration(), handler, emptyList(), null, false) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `value is returned if requested and stack is aligned`() {
        val register = Register.VirtualRegister()
        val nodes = getCallNodes(0, register, true)
        val resultDestination = getResultDestination(nodes)
        assertThat(resultDestination).isInstanceOf(CFGNode.RegisterUse::class.java)
        assertThat((resultDestination as CFGNode.RegisterUse).register).isEqualTo(register)
    }

    @Test
    fun `value is returned if requested and stack is not aligned`() {
        val register = Register.VirtualRegister()
        val nodes = getCallNodes(0, register, false)
        val resultDestination = getResultDestination(nodes)
        assertThat(resultDestination).isInstanceOf(CFGNode.RegisterUse::class.java)
        assertThat((resultDestination as CFGNode.RegisterUse).register).isEqualTo(register)
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
        assertThat(getStackAlignmentAdded(getCallNodes(6, null, true))).isEqualTo(8)
        assertThat(getStackAlignmentAdded(getCallNodes(7, null, true))).isEqualTo(0)
        assertThat(getStackAlignmentAdded(getCallNodes(8, null, true))).isEqualTo(8)
        assertThat(getStackAlignmentAdded(getCallNodes(9, null, true))).isEqualTo(0)
        assertThat(getStackAlignmentAdded(getCallNodes(10, null, true))).isEqualTo(8)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 5, 6, 7, 8, 18])
    fun `stack is not aligned if not requested`(args: Int) {
        assertThat(getStackAlignmentAdded(getCallNodes(args, null, false))).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 5, 6, 7, 8, 18])
    fun `excess arguments area passed on stack`(args: Int) {
        assertThat(getPushCount(getCallNodes(args, null, false))).isEqualTo(max(0, args + 1 - 6))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 5, 6, 7, 8, 18])
    fun `up to first six arguments area passed via registers`(args: Int) {
        val expected =
            listOf(
                HardwareRegister.RDI,
                HardwareRegister.RSI,
                HardwareRegister.RDX,
                HardwareRegister.RCX,
                HardwareRegister.R8,
                HardwareRegister.R9,
            ).take(args + 1)
        assertThat(getArgumentRegisters(getCallNodes(args, null, false))).isEqualTo(expected)
    }

    @Test
    fun `function calling child`() {
        val handlers = mockFunDeclarationAndFunHandlerWithParents(0, 3)
        val childHandler = handlers[0]
        val parentHandler = handlers[1]
        checkStaticLinkInGenerateCallFrom(
            childHandler,
            parentHandler,
            CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)),
        )
    }

    @Test
    fun `function calling itself works`() {
        val handlers = mockFunDeclarationAndFunHandlerWithParents(0, 3)
        val childHandler = handlers[0]
        checkStaticLinkInGenerateCallFrom(
            childHandler,
            childHandler,
            CFGNode.MemoryAccess(CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP))),
        )
    }

    @Test
    fun `function calling parent works`() {
        val handlers = mockFunDeclarationAndFunHandlerWithParents(0, 3)
        val childHandler = handlers[0]
        val parentHandler = handlers[1]
        checkStaticLinkInGenerateCallFrom(
            parentHandler,
            childHandler,
            CFGNode.MemoryAccess(CFGNode.MemoryAccess(CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)))),
        )
    }
}
