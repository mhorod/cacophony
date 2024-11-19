package cacophony.controlflow

import cacophony.semantic.AnalyzedFunction
import cacophony.semantic.AnalyzedVariable
import cacophony.semantic.ParentLink
import cacophony.semantic.VariableUseType
import cacophony.semantic.syntaxtree.*
import cacophony.utils.Location
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
    val mockRange = Location(0) to Location(0)

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
                    val analyzedFunction = mockAnalyzedFunction()
                    functionHandlers.add(
                        0,
                        FunctionHandlerImpl(
                            Definition.FunctionDeclaration(
                                mockk(),
                                "fun def",
                                mockk(),
                                (1..argumentCount).map { mockk() },
                                mockk(),
                                mockk(),
                            ),
                            analyzedFunction,
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

        private fun getArgumentRegisters(callNodes: List<CFGNode>): List<X64Register> {
            val returnList = mutableListOf<X64Register>()
            for (node in callNodes) {
                if (node is CFGNode.Assignment &&
                    node.destination is CFGNode.RegisterUse &&
                    node.destination.register is Register.FixedRegister
                ) {
                    val register = node.destination.register.hardwareRegister
                    if (register != X64Register.RSP) {
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
                if (node is CFGNode.Assignment &&
                    node.destination is CFGNode.RegisterUse &&
                    node.destination.register is Register.FixedRegister
                ) {
                    if (node.destination.register.hardwareRegister != X64Register.RSP) {
                        continue
                    }
                    if (node.value is CFGNode.Addition) {
                        val lhs = (node.value as CFGNode.Addition).lhs
                        val rhs = (node.value as CFGNode.Addition).rhs
                        if (lhs !is CFGNode.RegisterUse ||
                            lhs.register !is Register.FixedRegister ||
                            (lhs.register as Register.FixedRegister).hardwareRegister != X64Register.RSP
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
                if (node is CFGNode.Pop && node.register.register is Register.FixedRegister) {
                    if ((node.register.register as Register.FixedRegister).hardwareRegister == X64Register.RSP) {
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
                    X64Register.RDI,
                    X64Register.RSI,
                    X64Register.RDX,
                    X64Register.RCX,
                    X64Register.R8,
                    X64Register.R9,
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
                CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP)),
            )
        }

        @Test
        fun `function calling itself works`() {
            val handlers = mockFunDeclarationAndFunHandlerWithParents(0, 3)
            val childHandler = handlers[0]
            checkStaticLinkInGenerateCallFrom(
                childHandler,
                childHandler,
                CFGNode.MemoryAccess(CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP))),
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
                CFGNode.MemoryAccess(CFGNode.MemoryAccess(CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP)))),
            )
        }
    }

    @Test
    fun `initialization registers static link`() {
        val analyzedFunction = mockk<AnalyzedFunction>()
        val auxVariables = mutableSetOf<Variable.AuxVariable>()
        every { analyzedFunction.auxVariables } returns auxVariables
        every { analyzedFunction.variables } returns emptySet()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns emptyList()

        val handler = FunctionHandlerImpl(mockk(), analyzedFunction, emptyList())

        assertThat(auxVariables).contains(handler.getStaticLink())

        val allocation = handler.getVariableAllocation(handler.getStaticLink())
        require(allocation is VariableAllocation.OnStack)
        assertThat(allocation.offset).isEqualTo(0)
    }

    @Test
    fun `variable from definition just works`() {
        // setup
        val varDef = mockk<Definition>()
        val analyzedVariable = mockk<AnalyzedVariable>()
        every { analyzedVariable.declaration } returns varDef
        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns setOf(analyzedVariable)
        every { analyzedFunction.auxVariables } returns mutableSetOf()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)

        // run
        val handler = FunctionHandlerImpl(mockk(), analyzedFunction, emptyList())
        val variable = handler.getVariableFromDefinition(varDef)
        // check
        assertNotNull(variable)
        assert(variable is Variable.SourceVariable)
    }

    @Test
    fun `variable not used in nested function goes to virtual register`() {
        // setup
        val varDef = mockk<Definition>()
        val analyzedVariable = mockk<AnalyzedVariable>()
        every { analyzedVariable.declaration } returns varDef
        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns setOf(analyzedVariable)
        every { analyzedFunction.auxVariables } returns mutableSetOf()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)

        // run
        val handler = FunctionHandlerImpl(mockk(), analyzedFunction, emptyList())
        val variable = handler.getVariableFromDefinition(varDef)
        val allocation = handler.getVariableAllocation(variable)
        // check
        require(allocation is VariableAllocation.InRegister)
        assert(allocation.register is Register.VirtualRegister)
    }

    @Test
    fun `variable used in nested function goes on stack`() {
        // setup
        val varDef = mockk<Definition>()
        val analyzedVariable = mockk<AnalyzedVariable>()
        every { analyzedVariable.declaration } returns varDef
        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns setOf(analyzedVariable)
        every { analyzedFunction.auxVariables } returns mutableSetOf()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(varDef)
        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)

        // run
        val handler = FunctionHandlerImpl(mockk(), analyzedFunction, emptyList())
        val variable = handler.getVariableFromDefinition(varDef)
        val allocation = handler.getVariableAllocation(variable)
        // check
        require(allocation is VariableAllocation.OnStack)
        assertEquals(8, allocation.offset)
    }

    @Test
    fun `multiple variables, stack and virtual registers`() {
        // setup
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
        val handler = FunctionHandlerImpl(mockk(), analyzedFunction, emptyList())
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
            Definition.FunctionDeclaration(
                mockRange,
                "f",
                null,
                emptyList(),
                Type.Basic(mockRange, "Int"),
                Empty(mockRange),
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
        val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, emptyList())

        // when
        val declaration = fHandler.getFunctionDeclaration()

        // then
        assertThat(declaration).isEqualTo(fDef)
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
            val xDef = Definition.VariableDeclaration(mockRange, "x", null, Literal.IntLiteral(mockRange, 10))
            val fDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Block(
                        mockRange,
                        listOf(
                            xDef,
                            VariableUse(mockRange, "x"),
                        ),
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
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, emptyList())
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
            val xDef = Definition.VariableDeclaration(mockRange, "x", null, Literal.IntLiteral(mockRange, 10))
            val fDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Block(
                        mockRange,
                        listOf(
                            xDef,
                            VariableUse(mockRange, "x"),
                        ),
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
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, emptyList())
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
                    // [rbp + 24]
                    CFGNode.Addition(
                        CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP)),
                        CFGNode.Constant(24),
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
            val xDef = Definition.VariableDeclaration(mockRange, "x", null, Literal.IntLiteral(mockRange, 10))
            val fDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    VariableUse(mockRange, "x"),
                )
            val gDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "g",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    fDef,
                )
            val hDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "h",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Block(mockRange, listOf(xDef, gDef)),
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
            val hHandler = FunctionHandlerImpl(hDef, hAnalyzed, emptyList())
            val gHandler = FunctionHandlerImpl(gDef, gAnalyzed, listOf(hHandler))
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf(gHandler, hHandler))

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
                    // [[[rbp]] + 24]
                    CFGNode.Addition(
                        CFGNode.MemoryAccess(
                            CFGNode.MemoryAccess(
                                CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP)),
                            ),
                        ),
                        CFGNode.Constant(24),
                    ),
                ),
            )
        }

        @Test
        fun `generates variable access to its own static link`() {
            // let f = [] -> Int => 42 #request static link of f

            // given
            val fDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Literal.IntLiteral(mockRange, 42),
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
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, emptyList())

            // when
            val staticLinkAccess = fHandler.generateVariableAccess(fHandler.getStaticLink())

            // then
            assertThat(staticLinkAccess).isEqualTo(
                CFGNode.MemoryAccess(
                    // [rbp + 0]
                    CFGNode.Addition(
                        CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP)),
                        CFGNode.Constant(0),
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
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Literal.IntLiteral(mockRange, 42),
                )
            val gDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "g",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
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
            val gHandler = FunctionHandlerImpl(gDef, gAnalyzed, emptyList())
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf(gHandler))

            // when
            val staticLinkAccess = fHandler.generateVariableAccess(gHandler.getStaticLink())

            // then
            assertThat(staticLinkAccess).isEqualTo(
                CFGNode.MemoryAccess(
                    // [[rbp] + 0]
                    CFGNode.Addition(
                        CFGNode.MemoryAccess(
                            CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP)),
                        ),
                        CFGNode.Constant(0),
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
                Definition.FunctionDeclaration(
                    mockRange,
                    "h",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Literal.IntLiteral(mockRange, 42),
                )
            val fDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    FunctionCall(mockRange, VariableUse(mockRange, "h"), emptyList()),
                )
            val gDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "g",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Block(mockRange, listOf(hDef, fDef)),
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
            val gHandler = FunctionHandlerImpl(gDef, gAnalyzed, emptyList())
            val hHandler = FunctionHandlerImpl(hDef, hAnalyzed, listOf(gHandler))
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf(gHandler))

            mockkStatic(::generateCall)
            // when
            hHandler.generateCallFrom(fHandler, emptyList(), null, false)
            verify {
                generateCall(
                    any(),
                    listOf(CFGNode.MemoryAccess(CFGNode.RegisterUse(Register.FixedRegister(X64Register.RBP)))),
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
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    emptyList(),
                    Type.Basic(mockRange, "Int"),
                    Literal.IntLiteral(mockRange, 42),
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
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, emptyList())

            // when & then
            org.junit.jupiter.api.assertThrows<GenerateVariableAccessException> {
                fHandler.generateVariableAccess(
                    Variable.SourceVariable(
                        Definition.VariableDeclaration(mockRange, "x", null, Literal.IntLiteral(mockRange, 10)),
                    ),
                )
            }
        }
    }
}
