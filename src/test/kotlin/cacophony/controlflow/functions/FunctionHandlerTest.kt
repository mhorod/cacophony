package cacophony.controlflow.functions

import cacophony.*
import cacophony.controlflow.CFGNode
import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.VariableAllocation
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.AnalyzedVariable
import cacophony.semantic.analysis.ParentLink
import cacophony.semantic.analysis.VariableUseType
import cacophony.semantic.syntaxtree.*
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.max

class FunctionHandlerTest {
    private fun makeDefaultHandler(
        function: Definition.FunctionDeclaration,
        analyzedFunction: AnalyzedFunction,
        ancestorFunctionHandlers: List<FunctionHandler> = emptyList(),
    ): FunctionHandlerImpl {
        val callConvention = mockk<CallConvention>()
        every { callConvention.preservedRegisters() } returns emptyList()
        return FunctionHandlerImpl(function, analyzedFunction, ancestorFunctionHandlers, callConvention)
    }

    @Nested
    inner class GenerateCall {
        private fun checkStaticLinkInGenerateCallFrom(callee: FunctionHandler, caller: FunctionHandler, expectedStaticLink: CFGNode) {
            mockkStatic(::generateCall)
            callee.generateCallFrom(
                caller,
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
                            Definition.FunctionDeclaration(
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

            assertThatThrownBy { generateCall(handler.getFunctionDeclaration(), emptyList(), null) }
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

    @Test
    fun `initialization registers static link`() {
        val funDef = mockk<Definition.FunctionDeclaration>()
        val analyzedFunction = mockk<AnalyzedFunction>()
        val auxVariables = mutableSetOf<Variable.AuxVariable>()
        every { funDef.arguments } returns emptyList()
        every { analyzedFunction.auxVariables } returns auxVariables
        every { analyzedFunction.variables } returns emptySet()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns emptyList()

        val handler = makeDefaultHandler(funDef, analyzedFunction)

        assertThat(auxVariables).contains(handler.getStaticLink())

        val allocation = handler.getVariableAllocation(handler.getStaticLink())
        require(allocation is VariableAllocation.OnStack)
        assertThat(allocation.offset).isEqualTo(0)
    }

    @Test
    fun `variable from definition just works`() {
        // setup
        val funDef = mockk<Definition.FunctionDeclaration>()
        every { funDef.arguments } returns emptyList()
        val varDef = mockk<Definition>()
        val analyzedVariable = mockk<AnalyzedVariable>()
        every { analyzedVariable.declaration } returns varDef
        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns setOf(analyzedVariable)
        every { analyzedFunction.auxVariables } returns mutableSetOf()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)

        // run
        val handler = makeDefaultHandler(funDef, analyzedFunction)
        val variable = handler.getVariableFromDefinition(varDef)
        // check
        assertNotNull(variable)
        assert(variable is Variable.SourceVariable)
    }

    @Test
    fun `variable not used in nested function goes to virtual register`() {
        // setup
        val funDef = mockk<Definition.FunctionDeclaration>()
        every { funDef.arguments } returns emptyList()
        val varDef = mockk<Definition>()
        val analyzedVariable = mockk<AnalyzedVariable>()
        every { analyzedVariable.declaration } returns varDef
        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns setOf(analyzedVariable)
        every { analyzedFunction.auxVariables } returns mutableSetOf()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)

        // run
        val handler = makeDefaultHandler(funDef, analyzedFunction)
        val variable = handler.getVariableFromDefinition(varDef)
        val allocation = handler.getVariableAllocation(variable)
        // check
        require(allocation is VariableAllocation.InRegister)
        assert(allocation.register is Register.VirtualRegister)
    }

    @Test
    fun `variable used in nested function goes on stack`() {
        // setup
        val funDef = mockk<Definition.FunctionDeclaration>()
        every { funDef.arguments } returns emptyList()
        val varDef = mockk<Definition>()
        val analyzedVariable = mockk<AnalyzedVariable>()
        every { analyzedVariable.declaration } returns varDef
        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns setOf(analyzedVariable)
        every { analyzedFunction.auxVariables } returns mutableSetOf()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(varDef)
        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)

        // run
        val handler = makeDefaultHandler(funDef, analyzedFunction)
        val variable = handler.getVariableFromDefinition(varDef)
        val allocation = handler.getVariableAllocation(variable)
        // check
        require(allocation is VariableAllocation.OnStack)
        assertEquals(8, allocation.offset)
    }

    @Test
    fun `multiple variables, stack and virtual registers`() {
        // setup
        val funDef = mockk<Definition.FunctionDeclaration>()
        every { funDef.arguments } returns emptyList()
        val varDef1 = mockk<Definition>()
        val varDef2 = mockk<Definition>()
        val varDef3 = mockk<Definition>()
        val analyzedVariable1 = mockk<AnalyzedVariable>()
        every { analyzedVariable1.declaration } returns varDef1
        val analyzedVariable2 = mockk<AnalyzedVariable>()
        every { analyzedVariable2.declaration } returns varDef2
        val analyzedVariable3 = mockk<AnalyzedVariable>()
        every { analyzedVariable3.declaration } returns varDef3
        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns setOf(analyzedVariable1, analyzedVariable2, analyzedVariable3)
        every { analyzedFunction.auxVariables } returns mutableSetOf()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(varDef1, varDef3)
        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable1, analyzedVariable2, analyzedVariable3)

        // run
        val handler = makeDefaultHandler(funDef, analyzedFunction)
        val variable1 = handler.getVariableFromDefinition(varDef1)
        val variable2 = handler.getVariableFromDefinition(varDef2)
        val variable3 = handler.getVariableFromDefinition(varDef3)
        val allocation1 = handler.getVariableAllocation(variable1)
        val allocation2 = handler.getVariableAllocation(variable2)
        val allocation3 = handler.getVariableAllocation(variable3)
        // check
        require(allocation1 is VariableAllocation.OnStack)
        require(allocation2 is VariableAllocation.InRegister)
        require(allocation3 is VariableAllocation.OnStack)
        assertEquals(8, allocation1.offset)
        assert(allocation2.register is Register.VirtualRegister)
        assertEquals(16, allocation3.offset)
    }

    @Test
    fun `returns correct function declaration`() {
        // given
        val fDef =
            unitFunctionDeclaration(
                "f",
                emptyList(),
                empty(),
            )
        val fAnalyzed =
            AnalyzedFunction(
                fDef,
                null,
                emptySet(),
                mutableSetOf(),
                0,
                emptySet(),
            )

        val fHandler = makeDefaultHandler(fDef, fAnalyzed)

        // when
        val declaration = fHandler.getFunctionDeclaration()

        // then
        assertThat(declaration).isEqualTo(fDef)
    }

    @Test
    fun `stack size is correctly calculated`() {
        // setup
        val argumentDef = mockk<Definition.FunctionArgument>()
        val analyzedArgumentVariable = mockk<AnalyzedVariable>()
        every { analyzedArgumentVariable.declaration } returns argumentDef

        val ownVariableDef = mockk<Definition>()
        val analyzedOwnVariable = mockk<AnalyzedVariable>()
        every { analyzedOwnVariable.declaration } returns ownVariableDef

        val nestedVarDef = mockk<Definition>()
        val analyzedNestedVariable = mockk<AnalyzedVariable>()
        every { analyzedNestedVariable.declaration } returns nestedVarDef

        val noArgFunDef = mockk<Definition.FunctionDeclaration>()
        every { noArgFunDef.arguments } returns emptyList()
        val unaryFunDef = mockk<Definition.FunctionDeclaration>()
        every { unaryFunDef.arguments } returns listOf(argumentDef)

        val staticLinkVariable = mockk<Variable.AuxVariable.StaticLinkVariable>()

        val noArgAnalyzedFunction = mockk<AnalyzedFunction>()
        every { noArgAnalyzedFunction.variables } returns emptySet()
        every { noArgAnalyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
        every { noArgAnalyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { noArgAnalyzedFunction.declaredVariables() } returns emptyList()

        val unaryAnalyzedFunction = mockk<AnalyzedFunction>()
        every { unaryAnalyzedFunction.variables } returns setOf(analyzedArgumentVariable, analyzedOwnVariable, analyzedNestedVariable)
        every { unaryAnalyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
        every { unaryAnalyzedFunction.variablesUsedInNestedFunctions } returns setOf(nestedVarDef)
        every { unaryAnalyzedFunction.declaredVariables() } returns listOf(analyzedOwnVariable)

        // run
        val noArgFunctionHandler = makeDefaultHandler(noArgFunDef, noArgAnalyzedFunction)
        val unaryFunctionHandler = makeDefaultHandler(unaryFunDef, unaryAnalyzedFunction)
        // check
        assertEquals(8, noArgFunctionHandler.getStackSpace().value)
        assertEquals(16, unaryFunctionHandler.getStackSpace().value)
    }

    @Test
    fun `registering variables increases stack space`() {
        val funDef = mockk<Definition.FunctionDeclaration>()
        every { funDef.arguments } returns emptyList()

        val staticLinkVariable = mockk<Variable.AuxVariable.StaticLinkVariable>()

        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns emptySet()
        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns emptyList()

        val handler = makeDefaultHandler(funDef, analyzedFunction)
        assertEquals(8, handler.getStackSpace().value)

        handler.allocateFrameVariable(mockk<Variable.AuxVariable>())
        assertEquals(16, handler.getStackSpace().value)

        handler.registerVariableAllocation(mockk<Variable.SourceVariable>(), VariableAllocation.OnStack(32))
        assertEquals(40, handler.getStackSpace().value)

        handler.allocateFrameVariable(mockk<Variable.AuxVariable>())
        assertEquals(48, handler.getStackSpace().value)
    }

    @Test
    fun `allocateFrameVariable creates variable allocation`() {
        val funDef = mockk<Definition.FunctionDeclaration>()
        every { funDef.arguments } returns emptyList()

        val staticLinkVariable = mockk<Variable.AuxVariable.StaticLinkVariable>()

        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns emptySet()
        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns emptyList()

        val handler = makeDefaultHandler(funDef, analyzedFunction)
        assertEquals(8, handler.getStackSpace().value)

        var auxVariable = mockk<Variable.AuxVariable>()
        handler.allocateFrameVariable(auxVariable)

        var allocation = handler.getVariableAllocation(auxVariable)
        require(allocation is VariableAllocation.OnStack)
        assertEquals(8, allocation.offset)

        auxVariable = mockk<Variable.AuxVariable>()
        handler.allocateFrameVariable(auxVariable)

        allocation = handler.getVariableAllocation(auxVariable)
        require(allocation is VariableAllocation.OnStack)
        assertEquals(16, allocation.offset)
    }

    @Nested
    inner class GenerateVariableAccess {
        @Test
        fun `generates variable access to its own source variable allocated in a register`() {
            // let f = [] -> Int => (
            //     let x = 10; #allocated in virtual register
            //     x #variable access
            // )

            // given
            val xDef = variableDeclaration("x", lit(10))
            val fDef =
                unitFunctionDeclaration(
                    "f",
                    emptyList(),
                    block(
                        xDef,
                        variableUse("x"),
                    ),
                )
            val xAnalyzed = AnalyzedVariable(xDef, fDef, VariableUseType.READ_WRITE)
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    null,
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    0,
                    emptySet(),
                )
            val xAllocation = Register.VirtualRegister()

            val fHandler = makeDefaultHandler(fDef, fAnalyzed)
            val x = fHandler.getVariableFromDefinition(xDef)
            fHandler.registerVariableAllocation(
                x,
                VariableAllocation.InRegister(xAllocation),
            )

            // when
            val xAccess = fHandler.generateVariableAccess(x)

            // then
            assertThat(xAccess).isEqualTo(CFGNode.RegisterUse(xAllocation))
        }

        @Test
        fun `generates variable access to its own source variable allocated on stack`() {
            // let f = [] -> Int => (
            //     let x: Int = 10; #allocated on stack
            //     x #variable access
            // )

            // given
            val xDef = variableDeclaration("x", lit(10))
            val fDef =
                unitFunctionDeclaration(
                    "f",
                    emptyList(),
                    block(
                        xDef,
                        variableUse("x"),
                    ),
                )
            val xAnalyzed = AnalyzedVariable(xDef, fDef, VariableUseType.READ_WRITE)
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    null,
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    0,
                    emptySet(),
                )

            val fHandler = makeDefaultHandler(fDef, fAnalyzed)
            val x = fHandler.getVariableFromDefinition(xDef)
            fHandler.registerVariableAllocation(
                x,
                VariableAllocation.OnStack(24),
            )

            // when
            val xAccess = fHandler.generateVariableAccess(x)

            // then
            assertThat(xAccess).isEqualTo(
                CFGNode.MemoryAccess(
                    // [rbp - 24]
                    CFGNode.Subtraction(
                        CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)),
                        CFGNode.ConstantKnown(24),
                    ),
                ),
            )
        }

        @Test
        fun `generates variable access to source variable of ancestor function`() {
            // let h = [] -> Int => (
            //     let x = 10; #allocated on stack
            //     let g = [] -> Int => (
            //         let f = [] -> Int => (
            //             x #variable access
            //         );
            //     );
            // )

            // given
            val xDef = variableDeclaration("x", lit(10))
            val fDef =
                unitFunctionDeclaration(
                    "f",
                    emptyList(),
                    variableUse("x"),
                )
            val gDef =
                unitFunctionDeclaration(
                    "g",
                    emptyList(),
                    fDef,
                )
            val hDef =
                unitFunctionDeclaration(
                    "h",
                    emptyList(),
                    block(xDef, gDef),
                )

            val xAnalyzed = AnalyzedVariable(xDef, hDef, VariableUseType.READ_WRITE)
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    ParentLink(gDef, true),
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    2,
                    emptySet(),
                )
            val gAnalyzed =
                AnalyzedFunction(
                    gDef,
                    ParentLink(hDef, true),
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    1,
                    setOf(xDef),
                )
            val hAnalyzed =
                AnalyzedFunction(
                    hDef,
                    null,
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    0,
                    setOf(xDef),
                )

            val hHandler = makeDefaultHandler(hDef, hAnalyzed, emptyList())
            val gHandler = makeDefaultHandler(gDef, gAnalyzed, listOf(hHandler))
            val fHandler = makeDefaultHandler(fDef, fAnalyzed, listOf(gHandler, hHandler))

            val x = hHandler.getVariableFromDefinition(xDef)
            hHandler.registerVariableAllocation(
                x,
                VariableAllocation.OnStack(24),
            )

            // when
            val xAccess = fHandler.generateVariableAccess(x)

            // then
            assertThat(xAccess).isEqualTo(
                CFGNode.MemoryAccess(
                    // [[[rbp]] - 24]
                    CFGNode.Subtraction(
                        CFGNode.MemoryAccess(
                            CFGNode.MemoryAccess(
                                CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)),
                            ),
                        ),
                        CFGNode.ConstantKnown(24),
                    ),
                ),
            )
        }

        @Test
        fun `generates variable access to its own static link`() {
            // let f = [] -> Int => 42 #request static link of f

            // given
            val fDef =
                unitFunctionDeclaration(
                    "f",
                    emptyList(),
                    lit(42),
                )
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    null,
                    emptySet(),
                    mutableSetOf(),
                    0,
                    emptySet(),
                )

            val fHandler = makeDefaultHandler(fDef, fAnalyzed)

            // when
            val staticLinkAccess = fHandler.generateVariableAccess(fHandler.getStaticLink())

            // then
            assertThat(staticLinkAccess).isEqualTo(
                CFGNode.MemoryAccess(
                    // [rbp - 0]
                    CFGNode.Subtraction(
                        CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)),
                        CFGNode.ConstantKnown(0),
                    ),
                ),
            )
        }

        @Test
        fun `generates variable access to static link of its ancestor`() {
            // let g = [] -> Int => (
            //   let f = [] -> Int => 42 #request static link of g
            //                           #should recursively request static link of f
            // );

            // given
            val fDef =
                unitFunctionDeclaration(
                    "f",
                    emptyList(),
                    lit(42),
                )
            val gDef =
                unitFunctionDeclaration(
                    "g",
                    emptyList(),
                    fDef,
                )
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    ParentLink(gDef, true),
                    emptySet(),
                    mutableSetOf(),
                    1,
                    emptySet(),
                )
            val gAnalyzed =
                AnalyzedFunction(
                    gDef,
                    null,
                    emptySet(),
                    mutableSetOf(),
                    0,
                    emptySet(),
                )

            val gHandler = makeDefaultHandler(gDef, gAnalyzed, emptyList())
            val fHandler = makeDefaultHandler(fDef, fAnalyzed, listOf(gHandler))

            // when
            val staticLinkAccess = fHandler.generateVariableAccess(gHandler.getStaticLink())

            // then
            assertThat(staticLinkAccess).isEqualTo(
                CFGNode.MemoryAccess(
                    // [[rbp] - 0]
                    CFGNode.Subtraction(
                        CFGNode.MemoryAccess(
                            CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)),
                        ),
                        CFGNode.ConstantKnown(0),
                    ),
                ),
            )
        }

        @Test
        fun `calls its sibling with static link to parent `() {
            // let g = [] -> Int => (
            //   let h = [] -> Int => 42
            //   let f = [] -> Int => h[]  # should pass a static link to parent
            // );

            // given
            val hDef =
                unitFunctionDeclaration(
                    "h",
                    emptyList(),
                    lit(42),
                )
            val fDef =
                unitFunctionDeclaration(
                    "f",
                    emptyList(),
                    call(variableUse("h")),
                )
            val gDef =
                unitFunctionDeclaration(
                    "g",
                    emptyList(),
                    block(hDef, fDef),
                )
            val hAnalyzed =
                AnalyzedFunction(
                    hDef,
                    ParentLink(gDef, true),
                    emptySet(),
                    mutableSetOf(),
                    1,
                    emptySet(),
                )
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    ParentLink(gDef, true),
                    emptySet(),
                    mutableSetOf(),
                    1,
                    emptySet(),
                )
            val gAnalyzed =
                AnalyzedFunction(
                    gDef,
                    null,
                    emptySet(),
                    mutableSetOf(),
                    0,
                    emptySet(),
                )

            val gHandler = makeDefaultHandler(gDef, gAnalyzed, emptyList())
            val hHandler = makeDefaultHandler(hDef, hAnalyzed, listOf(gHandler))
            val fHandler = makeDefaultHandler(fDef, fAnalyzed, listOf(gHandler))

            mockkStatic(::generateCall)
            // when
            hHandler.generateCallFrom(fHandler, emptyList(), null, false)
            verify {
                generateCall(
                    any(),
                    listOf(CFGNode.MemoryAccess(CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)))),
                    any(),
                    false,
                )
            }
            unmockkStatic(::generateCall)
        }

        @Test
        fun `throws if requested access to variable that is not accessible`() {
            // given
            val fDef =
                unitFunctionDeclaration(
                    "f",
                    emptyList(),
                    lit(42),
                )
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    null,
                    emptySet(),
                    mutableSetOf(),
                    0,
                    emptySet(),
                )

            val fHandler = makeDefaultHandler(fDef, fAnalyzed, emptyList())

            // when & then
            org.junit.jupiter.api.assertThrows<GenerateVariableAccessException> {
                fHandler.generateVariableAccess(
                    Variable.SourceVariable(
                        variableDeclaration("x", lit(10)),
                    ),
                )
            }
        }

        @Test
        fun `throws if requested generation access of variable other than source variable or static link`() {
            // given
            val fDef = unitFunctionDeclaration("f", emptyList(), lit(42))
            val fAnalyzed =
                AnalyzedFunction(
                    fDef,
                    null,
                    setOf(),
                    mutableSetOf(),
                    0,
                    emptySet(),
                )
            val fHandler = makeDefaultHandler(fDef, fAnalyzed)

            // when & then
            org.junit.jupiter.api.assertThrows<GenerateVariableAccessException> {
                fHandler.generateVariableAccess(Variable.AuxVariable.SpillVariable())
            }
        }
    }
}
