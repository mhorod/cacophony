package cacophony.controlflow.functions

import cacophony.basicType
import cacophony.controlflow.CFGNode
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.foreignFunctionDeclaration
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

    // there is no easy way to check if constant computes exactly the stack space of a function handler
    private fun matchStackSpaceToHandler(constant: CFGNode.Constant, handler: FunctionHandler): Boolean {
        for (i in 1..3) {
            handler.allocateFrameVariable(Variable.PrimitiveVariable())
            if (handler.getStackSpace().value != constant.value)
                return false
        }
        return true
    }

    private fun checkStaticLinkInGenerateCallFrom(callee: FunctionHandler, caller: FunctionHandler, expectedStaticLink: CFGNode) {
        mockkStatic(::generateCall)
        generateCallFrom(
            caller,
            callee.getFunctionDeclaration(),
            callee,
            emptyList(),
            null,
        )

        verify {
            generateCall(
                any(),
                listOf(
                    expectedStaticLink,
                ),
                any(),
                match { matchStackSpaceToHandler(it, caller) }
            )
        }

        unmockkStatic(::generateCall)
    }

    private fun mockAnalyzedFunction(): AnalyzedFunction =
        run {
            val analyzedFunction = mockk<AnalyzedFunction>()
            val auxVariables = mutableSetOf<Variable>()
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
                            (1..argumentCount).map { mockk<Definition.FunctionArgument>().also { every { it.identifier } returns "x" } },
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

    private fun getCallNodes(argumentCount: Int, result: Register?): List<CFGNode> =
        generateCall(
            mockFunDeclarationAndFunHandler(argumentCount).getFunctionDeclaration(),
            (1..argumentCount + 1).map { mockk() },
            result,
            CFGNode.ConstantKnown(0)
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

    @Test
    fun `function call argument count mismatch throws error`() {
        val handler = mockFunDeclarationAndFunHandler(1)
        val caller = mockFunDeclarationAndFunHandler(0)

        assertThatThrownBy { generateCallFrom(caller, handler.getFunctionDeclaration(), handler, emptyList(), null) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `value is returned if requested`() {
        val register = Register.VirtualRegister()
        val nodes = getCallNodes(0, register)
        val resultDestination = getResultDestination(nodes)
        assertThat(resultDestination).isInstanceOf(CFGNode.RegisterUse::class.java)
        assertThat((resultDestination as CFGNode.RegisterUse).register).isEqualTo(register)
    }

    @Test
    fun `value is not returned if not requested`() {
        val nodes = getCallNodes(0, null)
        assertThat(getResultDestination(nodes)).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 5, 6, 7, 8, 18])
    fun `excess arguments area passed on stack`(args: Int) {
        assertThat(getPushCount(getCallNodes(args, null))).isEqualTo(max(0, args + 1 - 6))
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
        assertThat(getArgumentRegisters(getCallNodes(args, null))).isEqualTo(expected)
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

    @Test
    fun `calling foreign functions respects stack alignment`() {
        val caller = mockFunDeclarationAndFunHandler(0)
        val function = foreignFunctionDeclaration("f", emptyList(), basicType("Int"))

        mockkStatic(::generateCall)
        generateCallFrom(caller, function, null, emptyList(), null)
        verify {
            generateCall(
                function,
                any(),
                any(),
                match { matchStackSpaceToHandler(it, caller) }
            )
        }
        unmockkStatic(::generateCall)
    }
}
