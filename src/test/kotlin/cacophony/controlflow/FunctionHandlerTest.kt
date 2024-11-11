package cacophony.controlflow

import cacophony.semantic.AnalyzedFunction
import cacophony.semantic.AnalyzedVariable
import cacophony.semantic.ParentLink
import cacophony.semantic.VariableUseType
import cacophony.semantic.syntaxtree.*
import cacophony.utils.Location
import io.mockk.every
import io.mockk.mockk
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
        private fun mockAnalyzedFunction(): AnalyzedFunction =
            run {
                val analyzedFunction = mockk<AnalyzedFunction>()
                val auxVariables = mutableSetOf<Variable.AuxVariable>()
                every { analyzedFunction.variables } returns emptySet()
                every { analyzedFunction.auxVariables } returns auxVariables
                every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()

                analyzedFunction
            }

        private fun mockFunDeclarationAndFunHandlerWithParents(
            argumentCount: Int,
            chainLength: Int,
        ): List<FunctionHandlerImpl> =
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

        private fun getCallNodes(
            argumentCount: Int,
            result: Register?,
            alignStack: Boolean,
        ): List<CFGNode> =
            mockFunDeclarationAndFunHandler(argumentCount).generateCall(
                (1..argumentCount).map { mockk() },
                result,
                alignStack,
            )

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

        private fun getPushCount(callNodes: List<CFGNode>): Int = callNodes.filterIsInstance<CFGNode.Push>().size

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
                        if (lhs !is CFGNode.VariableUse ||
                            lhs.regvar !is Register.FixedRegister ||
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

        @Test
        fun `function calling child`() {
            val handlers = mockFunDeclarationAndFunHandlerWithParents(0, 3)
            val childHandler = handlers[0]
            val parentHandler = handlers[1]

            val staticLinkNode =
                childHandler.generateCallFrom(
                    parentHandler,
                    emptyList(),
                    null,
                )[0]

            // This test isn't too interesting, it's more about checking if nothing fails rather if it returns particular value.
            val staticLinkAccess = childHandler.generateVariableAccess(childHandler.getStaticLink())
            val expected =
                CFGNode.MemoryWrite(
                    CFGNode.MemoryAccess(staticLinkAccess),
                    CFGNode.VariableUse(Register.FixedRegister(X64Register.RBP)),
                )

            assertThat(staticLinkNode).isEqualTo(expected)
        }

        @Test
        fun `function calling itself works`() {
            val handlers = mockFunDeclarationAndFunHandlerWithParents(0, 3)
            val childHandler = handlers[0]

            val staticLinkNode =
                childHandler.generateCallFrom(
                    childHandler,
                    emptyList(),
                    null,
                )[0]

            // This test isn't too interesting, it's more about checking if nothing fails rather if it returns particular value.
            val staticLinkAccess = childHandler.generateVariableAccess(childHandler.getStaticLink())
            val expected =
                CFGNode.MemoryWrite(
                    CFGNode.MemoryAccess(staticLinkAccess),
                    childHandler.generateVariableAccess(childHandler.getStaticLink()),
                )

            assertThat(staticLinkNode).isEqualTo(expected)
        }

        @Test
        fun `function calling parent works`() {
            val handlers = mockFunDeclarationAndFunHandlerWithParents(0, 3)
            val childHandler = handlers[0]
            val parentHandler = handlers[1]

            val staticLinkNode =
                parentHandler.generateCallFrom(
                    childHandler,
                    emptyList(),
                    null,
                )[0]

            // This test isn't too interesting, it's more about checking if nothing fails rather if it returns particular value.
            val staticLinkAccess = childHandler.generateVariableAccess(parentHandler.getStaticLink())
            val expected =
                CFGNode.MemoryWrite(
                    CFGNode.MemoryAccess(staticLinkAccess),
                    childHandler.generateVariableAccess(parentHandler.getStaticLink()),
                )

            assertThat(staticLinkNode).isEqualTo(expected)
        }
    }

    @Test
    fun `initialization registers static link`() {
        val analyzedFunction = mockk<AnalyzedFunction>()
        val auxVariables = mutableSetOf<Variable.AuxVariable>()
        every { analyzedFunction.auxVariables } returns auxVariables
        every { analyzedFunction.variables } returns emptySet()
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()

        val handler = FunctionHandlerImpl(mockk(), analyzedFunction, emptyList())

        assertThat(auxVariables).contains(handler.getStaticLink())

        val allocation = handler.getVariableAllocation(handler.getStaticLink())
        require(allocation is VariableAllocation.InRegister)
        assert(allocation.register is Register.VirtualRegister)
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
        // run
        val handler = FunctionHandlerImpl(mockk(), analyzedFunction, emptyList())
        val variable = handler.getVariableFromDefinition(varDef)
        val allocation = handler.getVariableAllocation(variable)
        // check
        require(allocation is VariableAllocation.OnStack)
        assertEquals(0, allocation.offset)
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
        assertEquals(0, allocation1.offset)
        assert(allocation2.register is Register.VirtualRegister)
        assertEquals(8, allocation3.offset)
    }

    @Test
    fun `returns correct function declaration`() {
        // given
        val fDef =
            Definition.FunctionDeclaration(
                mockRange,
                "f",
                null,
                listOf(),
                Type.Basic(mockRange, "Int"),
                Empty(mockRange),
            )
        val fAnalyzed =
            AnalyzedFunction(
                null,
                setOf(),
                mutableSetOf(),
                0,
                setOf(),
            )
        val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf())

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
                    listOf(),
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
                    null,
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    0,
                    setOf(),
                )
            val xAllocation = Register.VirtualRegister()
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf())
            val x = fHandler.getVariableFromDefinition(xDef)
            fHandler.registerVariableAllocation(
                x,
                VariableAllocation.InRegister(xAllocation),
            )

            // when
            val xAccess = fHandler.generateVariableAccess(x)

            // then
            assertThat(xAccess).isEqualTo(CFGNode.VariableUse(xAllocation))
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
                    listOf(),
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
                    null,
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    0,
                    setOf(),
                )
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf())
            val x = fHandler.getVariableFromDefinition(xDef)
            fHandler.registerVariableAllocation(
                x,
                VariableAllocation.OnStack(24),
            )

            // when
            val xAccess = fHandler.generateVariableAccess(x)

            // then
            assertThat(xAccess).isEqualTo(
                CFGNode.MemoryAccess( // [rbp + 24]
                    CFGNode.Addition(
                        CFGNode.VariableUse(Register.FixedRegister(X64Register.RBP)),
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
                    listOf(),
                    Type.Basic(mockRange, "Int"),
                    VariableUse(mockRange, "x"),
                )
            val gDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "g",
                    null,
                    listOf(),
                    Type.Basic(mockRange, "Int"),
                    fDef,
                )
            val hDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "h",
                    null,
                    listOf(),
                    Type.Basic(mockRange, "Int"),
                    Block(mockRange, listOf(xDef, gDef)),
                )

            val xAnalyzed = AnalyzedVariable(xDef, hDef, VariableUseType.READ_WRITE)
            val fAnalyzed =
                AnalyzedFunction(
                    ParentLink(gDef, true),
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    2,
                    setOf(),
                )
            val gAnalyzed =
                AnalyzedFunction(
                    ParentLink(hDef, true),
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    1,
                    setOf(xDef),
                )
            val hAnalyzed =
                AnalyzedFunction(
                    null,
                    setOf(xAnalyzed),
                    mutableSetOf(),
                    0,
                    setOf(xDef),
                )
            val hHandler = FunctionHandlerImpl(hDef, hAnalyzed, listOf())
            val gHandler = FunctionHandlerImpl(gDef, gAnalyzed, listOf(hHandler))
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf(gHandler, hHandler))

            val x = hHandler.getVariableFromDefinition(xDef)
            hHandler.registerVariableAllocation(
                x,
                VariableAllocation.OnStack(24),
            )
            gHandler.registerVariableAllocation(
                gHandler.getStaticLink(),
                VariableAllocation.OnStack(8),
            )
            fHandler.registerVariableAllocation(
                fHandler.getStaticLink(),
                VariableAllocation.OnStack(32),
            )

            // when
            val xAccess = fHandler.generateVariableAccess(x)

            // then
            assertThat(xAccess).isEqualTo(
                CFGNode.MemoryAccess( // [[[rbp + 32] + 8] + 24]
                    CFGNode.Addition(
                        CFGNode.MemoryAccess(
                            CFGNode.Addition(
                                CFGNode.MemoryAccess(
                                    CFGNode.Addition(
                                        CFGNode.VariableUse(Register.FixedRegister(X64Register.RBP)),
                                        CFGNode.Constant(32),
                                    ),
                                ),
                                CFGNode.Constant(8),
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
                    listOf(),
                    Type.Basic(mockRange, "Int"),
                    Literal.IntLiteral(mockRange, 42),
                )
            val fAnalyzed =
                AnalyzedFunction(
                    null,
                    setOf(),
                    mutableSetOf(),
                    0,
                    setOf(),
                )
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf())
            fHandler.registerVariableAllocation(
                fHandler.getStaticLink(),
                VariableAllocation.OnStack(16),
            )

            // when
            val staticLinkAccess = fHandler.generateVariableAccess(fHandler.getStaticLink())

            // then
            assertThat(staticLinkAccess).isEqualTo(
                CFGNode.MemoryAccess( // [rbp + 16]
                    CFGNode.Addition(
                        CFGNode.VariableUse(Register.FixedRegister(X64Register.RBP)),
                        CFGNode.Constant(16),
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
                    listOf(),
                    Type.Basic(mockRange, "Int"),
                    Literal.IntLiteral(mockRange, 42),
                )
            val gDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "g",
                    null,
                    listOf(),
                    Type.Basic(mockRange, "Int"),
                    fDef,
                )
            val fAnalyzed =
                AnalyzedFunction(
                    ParentLink(gDef, true),
                    setOf(),
                    mutableSetOf(),
                    1,
                    setOf(),
                )
            val gAnalyzed =
                AnalyzedFunction(
                    null,
                    setOf(),
                    mutableSetOf(),
                    0,
                    setOf(),
                )
            val gHandler = FunctionHandlerImpl(gDef, gAnalyzed, listOf())
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf(gHandler))
            gHandler.registerVariableAllocation(
                gHandler.getStaticLink(),
                VariableAllocation.OnStack(48),
            )
            fHandler.registerVariableAllocation(
                fHandler.getStaticLink(),
                VariableAllocation.OnStack(16),
            )

            // when
            val staticLinkAccess = fHandler.generateVariableAccess(gHandler.getStaticLink())

            // then
            assertThat(staticLinkAccess).isEqualTo(
                CFGNode.MemoryAccess( // [[rbp + 16] + 48]
                    CFGNode.Addition(
                        CFGNode.MemoryAccess(
                            CFGNode.Addition(
                                CFGNode.VariableUse(Register.FixedRegister(X64Register.RBP)),
                                CFGNode.Constant(16),
                            ),
                        ),
                        CFGNode.Constant(48),
                    ),
                ),
            )
        }

        @Test
        fun `throws if requested access to variable that is not accessible`() {
            // given
            val fDef =
                Definition.FunctionDeclaration(
                    mockRange,
                    "f",
                    null,
                    listOf(),
                    Type.Basic(mockRange, "Int"),
                    Literal.IntLiteral(mockRange, 42),
                )
            val fAnalyzed =
                AnalyzedFunction(
                    null,
                    setOf(),
                    mutableSetOf(),
                    0,
                    setOf(),
                )
            val fHandler = FunctionHandlerImpl(fDef, fAnalyzed, listOf())

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
