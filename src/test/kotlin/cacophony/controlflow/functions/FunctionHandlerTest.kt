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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FunctionHandlerTest {
    private fun makeDefaultHandler(
        function: Definition.FunctionDefinition,
        analyzedFunction: AnalyzedFunction,
        ancestorFunctionHandlers: List<FunctionHandler> = emptyList(),
    ): FunctionHandlerImpl {
        val callConvention = mockk<CallConvention>()
        every { callConvention.preservedRegisters() } returns emptyList()
        return FunctionHandlerImpl(function, analyzedFunction, ancestorFunctionHandlers, callConvention)
    }

    @Test
    fun `initialization registers static link`() {
        val funDef = mockk<Definition.FunctionDefinition>()
        val analyzedFunction = mockk<AnalyzedFunction>()
        val auxVariables = mutableSetOf<Variable>()
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
    fun `variable from definition just works`() { // TODO: adjust or remove this test
        // setup
        val funDef = mockk<Definition.FunctionDefinition>()
        every { funDef.arguments } returns emptyList()
        val varDef = mockk<Definition>()
        every { varDef.identifier } returns "x"
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
        val funDef = mockk<Definition.FunctionDefinition>()
        every { funDef.arguments } returns emptyList()
        val varDef = mockk<Definition>()
        every { varDef.identifier } returns "x"
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
        val funDef = mockk<Definition.FunctionDefinition>()
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
        val funDef = mockk<Definition.FunctionDefinition>()
        every { funDef.arguments } returns emptyList()
        val varDef1 = mockk<Definition>()
        every { varDef1.identifier } returns "x1"
        val varDef2 = mockk<Definition>()
        every { varDef2.identifier } returns "x2"
        val varDef3 = mockk<Definition>()
        every { varDef3.identifier } returns "x3"
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
            unitFunctionDefinition(
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
        every { argumentDef.identifier } returns "x"
        val analyzedArgumentVariable = mockk<AnalyzedVariable>()
        every { analyzedArgumentVariable.declaration } returns argumentDef

        val ownVariableDef = mockk<Definition>()
        every { ownVariableDef.identifier } returns "y"
        val analyzedOwnVariable = mockk<AnalyzedVariable>()
        every { analyzedOwnVariable.declaration } returns ownVariableDef

        val nestedVarDef = mockk<Definition>()
        every { ownVariableDef.identifier } returns "z"
        val analyzedNestedVariable = mockk<AnalyzedVariable>()
        every { analyzedNestedVariable.declaration } returns nestedVarDef

        val noArgFunDef = mockk<Definition.FunctionDefinition>()
        every { noArgFunDef.arguments } returns emptyList()
        val unaryFunDef = mockk<Definition.FunctionDefinition>()
        every { unaryFunDef.arguments } returns listOf(argumentDef)

        val staticLinkVariable = mockk<Variable.PrimitiveVariable>()

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
        val funDef = mockk<Definition.FunctionDefinition>()
        every { funDef.arguments } returns emptyList()

        val staticLinkVariable = mockk<Variable>()

        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns emptySet()
        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns emptyList()

        val handler = makeDefaultHandler(funDef, analyzedFunction)
        assertEquals(8, handler.getStackSpace().value)

        handler.allocateFrameVariable(mockk<Variable>())
        assertEquals(16, handler.getStackSpace().value)

        handler.registerVariableAllocation(mockk<Variable>(), VariableAllocation.OnStack(32))
        assertEquals(40, handler.getStackSpace().value)

        handler.allocateFrameVariable(mockk<Variable>())
        assertEquals(48, handler.getStackSpace().value)
    }

    @Test
    fun `allocateFrameVariable creates variable allocation`() {
        val funDef = mockk<Definition.FunctionDefinition>()
        every { funDef.arguments } returns emptyList()

        val staticLinkVariable = mockk<Variable>()

        val analyzedFunction = mockk<AnalyzedFunction>()
        every { analyzedFunction.variables } returns emptySet()
        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
        every { analyzedFunction.declaredVariables() } returns emptyList()

        val handler = makeDefaultHandler(funDef, analyzedFunction)
        assertEquals(8, handler.getStackSpace().value)

        var auxVariable = mockk<Variable>()
        handler.allocateFrameVariable(auxVariable)

        var allocation = handler.getVariableAllocation(auxVariable)
        require(allocation is VariableAllocation.OnStack)
        assertEquals(8, allocation.offset)

        auxVariable = mockk<Variable>()
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
                unitFunctionDefinition(
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
                unitFunctionDefinition(
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
                unitFunctionDefinition(
                    "f",
                    emptyList(),
                    variableUse("x"),
                )
            val gDef =
                unitFunctionDefinition(
                    "g",
                    emptyList(),
                    fDef,
                )
            val hDef =
                unitFunctionDefinition(
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
                unitFunctionDefinition(
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
                unitFunctionDefinition(
                    "f",
                    emptyList(),
                    lit(42),
                )
            val gDef =
                unitFunctionDefinition(
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
                unitFunctionDefinition(
                    "h",
                    emptyList(),
                    lit(42),
                )
            val fDef =
                unitFunctionDefinition(
                    "f",
                    emptyList(),
                    call(variableUse("h")),
                )
            val gDef =
                unitFunctionDefinition(
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
            generateCallFrom(fHandler, hDef, hHandler, emptyList(), null)
            verify {
                generateCall(
                    any(),
                    listOf(CFGNode.MemoryAccess(CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)))),
                    any(),
                    any()
                )
            }
            unmockkStatic(::generateCall)
        }

        @Test
        fun `throws if requested access to variable that is not accessible`() {
            // given
            val fDef =
                unitFunctionDefinition(
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
            val fDef = unitFunctionDefinition("f", emptyList(), lit(42))
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
                fHandler.generateVariableAccess(Variable.PrimitiveVariable())
            }
        }
    }
}
