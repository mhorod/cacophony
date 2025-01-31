package cacophony.controlflow.functions

// import cacophony.*
// import cacophony.controlflow.*
// import cacophony.controlflow.generation.SimpleLayout
// import cacophony.semantic.analysis.*
// import cacophony.semantic.createVariablesMap
// import cacophony.semantic.syntaxtree.*
// import io.mockk.*
// import org.assertj.core.api.Assertions.assertThat
// import org.junit.jupiter.api.Assertions.*
// import org.junit.jupiter.api.Nested
// import org.junit.jupiter.api.Test

class FunctionHandlerTest {
//    private fun makeDefaultHandler(
//        function: Definition.FunctionDefinition,
//        analyzedFunction: AnalyzedFunction,
//        definitions: Map<Definition, Variable.PrimitiveVariable> = emptyMap(),
//        ancestorFunctionHandlers: List<FunctionHandler> = emptyList(),
//        escapeAnalysis: EscapeAnalysisResult = emptySet(),
//    ): FunctionHandlerImpl {
//        val callConvention = mockk<CallConvention>()
//        every { callConvention.preservedRegisters() } returns emptyList()
//        return FunctionHandlerImpl(
//            function,
//            analyzedFunction,
//            ancestorFunctionHandlers,
//            callConvention,
//            createVariablesMap(definitions),
//            escapeAnalysis,
//        )
//    }
//
//    private fun unitFunctionDefinition(): Definition.FunctionDefinition {
//        val funDef = mockk<Definition.FunctionDefinition>()
//        every { funDef.arguments } returns emptyList()
//        every { funDef.returnType } returns BaseType.Basic(mockRange(), "Unit")
//        return funDef
//    }
//
//    @Test
//    fun `initialization registers static link`() {
//        val funDef = unitFunctionDefinition()
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        val auxVariables = mutableSetOf<Variable>()
//        every { analyzedFunction.auxVariables } returns auxVariables
//        every { analyzedFunction.variables } returns emptySet()
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
//        every { analyzedFunction.declaredVariables() } returns emptyList()
//
//        val handler = makeDefaultHandler(funDef, analyzedFunction)
//
//        assertThat(auxVariables).contains(handler.getStaticLink())
//
//        val allocation = handler.getVariableAllocation(handler.getStaticLink())
//        require(allocation is VariableAllocation.OnStack)
//        assertThat(allocation.offset).isEqualTo(8)
//    }
//
//    @Test
//    fun `variable not used in nested function goes to virtual register`() {
//        // setup
//        val funDef = unitFunctionDefinition()
//        val varDef = mockk<Definition>()
//        every { varDef.identifier } returns "x"
//        val analyzedVariable = mockk<AnalyzedVariable>()
//        val variable = Variable.PrimitiveVariable()
//        every { analyzedVariable.origin } returns variable
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns setOf(analyzedVariable)
//        every { analyzedFunction.auxVariables } returns mutableSetOf()
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
//        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)
//
//        // run
//        val handler = makeDefaultHandler(funDef, analyzedFunction)
//        val allocation = handler.getVariableAllocation(variable)
//        // check
//        require(allocation is VariableAllocation.InRegister)
//        assert(allocation.register is Register.VirtualRegister)
//    }
//
//    @Test
//    fun `variable used in nested function goes on stack`() {
//        // setup
//        val funDef = unitFunctionDefinition()
//        val variable = Variable.PrimitiveVariable()
//        val analyzedVariable = mockk<AnalyzedVariable>()
//        every { analyzedVariable.origin } returns variable
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns setOf(analyzedVariable)
//        every { analyzedFunction.auxVariables } returns mutableSetOf()
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(variable)
//        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable)
//
//        // run
//        val handler = makeDefaultHandler(funDef, analyzedFunction)
//        val allocation = handler.getVariableAllocation(variable)
//        // check
//        require(allocation is VariableAllocation.OnStack)
//        assertEquals(16, allocation.offset)
//    }
//
//    @Test
//    fun `multiple variables, stack and virtual registers`() {
//        // setup
//        val funDef = unitFunctionDefinition()
//        val varDef1 = mockk<Definition>()
//        val variable1 = Variable.PrimitiveVariable()
//        every { varDef1.identifier } returns "x1"
//        val varDef2 = mockk<Definition>()
//        val variable2 = Variable.PrimitiveVariable()
//        every { varDef2.identifier } returns "x2"
//        val varDef3 = mockk<Definition>()
//        val variable3 = Variable.PrimitiveVariable()
//        every { varDef3.identifier } returns "x3"
//        val analyzedVariable1 = mockk<AnalyzedVariable>()
//        every { analyzedVariable1.origin } returns variable1
//        val analyzedVariable2 = mockk<AnalyzedVariable>()
//        every { analyzedVariable2.origin } returns variable2
//        val analyzedVariable3 = mockk<AnalyzedVariable>()
//        every { analyzedVariable3.origin } returns variable3
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns setOf(analyzedVariable1, analyzedVariable2, analyzedVariable3)
//        every { analyzedFunction.auxVariables } returns mutableSetOf()
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(variable1, variable3)
//        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVariable1, analyzedVariable2, analyzedVariable3)
//
//        // run
//        val handler = makeDefaultHandler(funDef, analyzedFunction)
//        val allocation1 = handler.getVariableAllocation(variable1)
//        val allocation2 = handler.getVariableAllocation(variable2)
//        val allocation3 = handler.getVariableAllocation(variable3)
//        // check
//        require(allocation1 is VariableAllocation.OnStack)
//        require(allocation2 is VariableAllocation.InRegister)
//        require(allocation3 is VariableAllocation.OnStack)
//        assertEquals(16, allocation1.offset)
//        assert(allocation2.register is Register.VirtualRegister)
//        assertEquals(24, allocation3.offset)
//    }
//
//    @Test
//    fun `returns correct function declaration`() {
//        // given
//        val fDef =
//            unitFunctionDefinition(
//                "f",
//                emptyList(),
//                empty(),
//            )
//        val fAnalyzed =
//            AnalyzedFunction(
//                fDef,
//                null,
//                emptySet(),
//                emptyList(),
//                mutableSetOf(),
//                0,
//                emptySet(),
//            )
//
//        val fHandler = makeDefaultHandler(fDef, fAnalyzed)
//
//        // when
//        val declaration = fHandler.getFunctionDeclaration()
//
//        // then
//        assertThat(declaration).isEqualTo(fDef)
//    }
//
//    @Test
//    fun `stack size is correctly calculated`() {
//        // setup
//        val argumentDef = mockk<Definition.FunctionArgument>()
//        val argVariable = Variable.PrimitiveVariable()
//        every { argumentDef.identifier } returns "x"
//        val analyzedArgumentVariable = mockk<AnalyzedVariable>()
//        every { analyzedArgumentVariable.origin } returns argVariable
//
//        val ownVariableDef = mockk<Definition>()
//        val ownVariable = Variable.PrimitiveVariable()
//        every { ownVariableDef.identifier } returns "y"
//        val analyzedOwnVariable = mockk<AnalyzedVariable>()
//        every { analyzedOwnVariable.origin } returns ownVariable
//
//        val nestedVariableDef = mockk<Definition>()
//        val nestedVariable = Variable.PrimitiveVariable()
//        every { nestedVariableDef.identifier } returns "z"
//        val analyzedNestedVariable = mockk<AnalyzedVariable>()
//        every { analyzedNestedVariable.origin } returns nestedVariable
//
//        val noArgFunDef = mockk<Definition.FunctionDefinition>()
//        every { noArgFunDef.arguments } returns emptyList()
//        every { noArgFunDef.returnType } returns BaseType.Basic(mockRange(), "Unit")
//        val unaryFunDef = mockk<Definition.FunctionDefinition>()
//        every { unaryFunDef.arguments } returns listOf(argumentDef)
//        every { unaryFunDef.returnType } returns BaseType.Basic(mockRange(), "Unit")
//
//        val staticLinkVariable = mockk<Variable.PrimitiveVariable>()
//
//        val noArgAnalyzedFunction = mockk<AnalyzedFunction>()
//        every { noArgAnalyzedFunction.variables } returns emptySet()
//        every { noArgAnalyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { noArgAnalyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
//        every { noArgAnalyzedFunction.declaredVariables() } returns emptyList()
//        every { noArgAnalyzedFunction.arguments } returns emptyList()
//
//        val unaryAnalyzedFunction = mockk<AnalyzedFunction>()
//        every { unaryAnalyzedFunction.variables } returns setOf(analyzedArgumentVariable, analyzedOwnVariable, analyzedNestedVariable)
//        every { unaryAnalyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { unaryAnalyzedFunction.variablesUsedInNestedFunctions } returns setOf(nestedVariable)
//        every { unaryAnalyzedFunction.declaredVariables() } returns listOf(analyzedOwnVariable)
//        every { noArgAnalyzedFunction.arguments } returns listOf(argumentDef)
//
//        val unaryAnalyzedFunctionWithNestedUsage = mockk<AnalyzedFunction>()
//        every { unaryAnalyzedFunctionWithNestedUsage.variables } returns
//            setOf(analyzedArgumentVariable, analyzedOwnVariable, analyzedNestedVariable)
//        every { unaryAnalyzedFunctionWithNestedUsage.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { unaryAnalyzedFunctionWithNestedUsage.variablesUsedInNestedFunctions } returns setOf(nestedVariable, ownVariable)
//        every { unaryAnalyzedFunctionWithNestedUsage.declaredVariables() } returns listOf(analyzedOwnVariable)
//        every { unaryAnalyzedFunctionWithNestedUsage.arguments } returns listOf(argumentDef)
//
//        // run
//        val noArgFunctionHandler =
//            makeDefaultHandler(
//                noArgFunDef,
//                noArgAnalyzedFunction,
//                mapOf(
//                    argumentDef to argVariable,
//                    ownVariableDef to ownVariable,
//                ),
//            )
//        val unaryFunctionHandler =
//            makeDefaultHandler(
//                unaryFunDef,
//                unaryAnalyzedFunction,
//                mapOf(
//                    argumentDef to argVariable,
//                    ownVariableDef to ownVariable,
//                    nestedVariableDef to nestedVariable,
//                ),
//            )
//        val unaryWithNestedUsageFunctionHandler =
//            makeDefaultHandler(
//                unaryFunDef,
//                unaryAnalyzedFunctionWithNestedUsage,
//                mapOf(
//                    argumentDef to argVariable,
//                    ownVariableDef to ownVariable,
//                    nestedVariableDef to nestedVariable,
//                ),
//            )
//        // check
//        assertEquals(16, noArgFunctionHandler.getStackSpace().value)
//        assertEquals(16, unaryFunctionHandler.getStackSpace().value)
//        assertEquals(24, unaryWithNestedUsageFunctionHandler.getStackSpace().value)
//    }
//
//    @Test
//    fun `allocateFrameVariable creates variable allocation`() {
//        val funDef =
//            unitFunctionDefinition()
//
//        val staticLinkVariable = mockk<Variable>()
//
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns emptySet()
//        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
//        every { analyzedFunction.declaredVariables() } returns emptyList()
//
//        val handler = makeDefaultHandler(funDef, analyzedFunction)
//        assertEquals(16, handler.getStackSpace().value)
//
//        var primVariable = Variable.PrimitiveVariable()
//        handler.allocateFrameVariable(primVariable)
//
//        var allocation = handler.getVariableAllocation(primVariable)
//        require(allocation is VariableAllocation.OnStack)
//        assertEquals(16, allocation.offset)
//
//        primVariable = Variable.PrimitiveVariable()
//        handler.allocateFrameVariable(primVariable)
//
//        allocation = handler.getVariableAllocation(primVariable)
//        require(allocation is VariableAllocation.OnStack)
//        assertEquals(24, allocation.offset)
//    }
//
//    @Test
//    fun `registering variables increases stack space`() {
//        val funDef = unitFunctionDefinition()
//        val staticLinkVariable = mockk<Variable>()
//
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns emptySet()
//        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
//        every { analyzedFunction.declaredVariables() } returns emptyList()
//
//        val handler = makeDefaultHandler(funDef, analyzedFunction)
//        assertEquals(16, handler.getStackSpace().value)
//
//        handler.allocateFrameVariable(Variable.PrimitiveVariable())
//        assertEquals(24, handler.getStackSpace().value)
//
//        handler.registerVariableAllocation(Variable.PrimitiveVariable(), VariableAllocation.OnStack(32))
//        assertEquals(40, handler.getStackSpace().value)
//
//        handler.allocateFrameVariable(Variable.PrimitiveVariable())
//        assertEquals(48, handler.getStackSpace().value)
//    }
//
//    @Test
//    fun `escaped variable has via pointer allocation, no usage in nested`() {
//        val funDef = unitFunctionDefinition()
//        val staticLinkVariable = mockk<Variable>()
//
//        val ownVariableDef = mockk<Definition>()
//        val ownVariable = Variable.PrimitiveVariable()
//        every { ownVariableDef.identifier } returns "x"
//        val analyzedOwnVariable = mockk<AnalyzedVariable>()
//        every { analyzedOwnVariable.origin } returns ownVariable
//
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns setOf(analyzedOwnVariable)
//        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns emptySet()
//        every { analyzedFunction.declaredVariables() } returns listOf(analyzedOwnVariable)
//
//        val handler = makeDefaultHandler(funDef, analyzedFunction, escapeAnalysis = setOf(ownVariable))
//        val assignedAllocation = handler.getVariableAllocation(ownVariable)
//        require(assignedAllocation is VariableAllocation.ViaPointer)
//        require(assignedAllocation.pointer is VariableAllocation.InRegister)
//        assertThat(assignedAllocation.offset).isZero()
//    }
//
//    @Test
//    fun `escaped variable has via pointer allocation, usage in nested`() {
//        val funDef = unitFunctionDefinition()
//        val staticLinkVariable = mockk<Variable>()
//
//        val ownVariableDef = mockk<Definition>()
//        val ownVariable = Variable.PrimitiveVariable()
//        every { ownVariableDef.identifier } returns "x"
//        val analyzedOwnVariable = mockk<AnalyzedVariable>()
//        every { analyzedOwnVariable.origin } returns ownVariable
//
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns setOf(analyzedOwnVariable)
//        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(ownVariable)
//        every { analyzedFunction.declaredVariables() } returns listOf(analyzedOwnVariable)
//
//        val handler = makeDefaultHandler(funDef, analyzedFunction, escapeAnalysis = setOf(ownVariable))
//        val assignedAllocation = handler.getVariableAllocation(ownVariable)
//        require(assignedAllocation is VariableAllocation.ViaPointer)
//        assertThat(assignedAllocation.offset).isZero()
//        val insideAlloc = assignedAllocation.pointer
//        require(insideAlloc is VariableAllocation.OnStack)
//        assertThat(insideAlloc.offset).isEqualTo(16)
//    }
//
//    @Test
//    fun `one escaped no nested, one escaped nested`() {
//        val funDef = unitFunctionDefinition()
//        val staticLinkVariable = mockk<Variable>()
//
//        val defVar1 = mockk<Definition>()
//        val var1 = Variable.PrimitiveVariable()
//        every { defVar1.identifier } returns "x"
//        val analyzedVar1 = mockk<AnalyzedVariable>()
//        every { analyzedVar1.origin } returns var1
//
//        val defVar2 = mockk<Definition>()
//        val var2 = Variable.PrimitiveVariable()
//        every { defVar2.identifier } returns "y"
//        val analyzedVar2 = mockk<AnalyzedVariable>()
//        every { analyzedVar2.origin } returns var2
//
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns setOf(analyzedVar1, analyzedVar2)
//        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(var1)
//        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVar1, analyzedVar2)
//
//        val handler = makeDefaultHandler(funDef, analyzedFunction, escapeAnalysis = setOf(var1, var2))
//
//        val var1Alloc = handler.getVariableAllocation(var1)
//        require(var1Alloc is VariableAllocation.ViaPointer)
//        assertThat(var1Alloc.offset).isZero()
//        val insideAlloc = var1Alloc.pointer
//        require(insideAlloc is VariableAllocation.OnStack)
//        assertThat(insideAlloc.offset).isEqualTo(16)
//
//        val var2Alloc = handler.getVariableAllocation(var2)
//        require(var2Alloc is VariableAllocation.ViaPointer)
//        require(var2Alloc.pointer is VariableAllocation.InRegister)
//        assertThat(var2Alloc.offset).isZero()
//    }
//
//    @Test
//    fun `4 kinds`() {
//        val funDef = unitFunctionDefinition()
//        val staticLinkVariable = mockk<Variable>()
//
//        // escape nested
//        val defVar1 = mockk<Definition>()
//        val var1 = Variable.PrimitiveVariable()
//        every { defVar1.identifier } returns "x"
//        val analyzedVar1 = mockk<AnalyzedVariable>()
//        every { analyzedVar1.origin } returns var1
//
//        // escape no nested
//        val defVar2 = mockk<Definition>()
//        val var2 = Variable.PrimitiveVariable()
//        every { defVar2.identifier } returns "y"
//        val analyzedVar2 = mockk<AnalyzedVariable>()
//        every { analyzedVar2.origin } returns var2
//
//        // no escape nested
//        val defVar3 = mockk<Definition>()
//        val var3 = Variable.PrimitiveVariable()
//        every { defVar3.identifier } returns "z"
//        val analyzedVar3 = mockk<AnalyzedVariable>()
//        every { analyzedVar3.origin } returns var3
//
//        // no escape no nested
//        val defVar4 = mockk<Definition>()
//        val var4 = Variable.PrimitiveVariable()
//        every { defVar4.identifier } returns "t"
//        val analyzedVar4 = mockk<AnalyzedVariable>()
//        every { analyzedVar4.origin } returns var4
//
//        val analyzedFunction = mockk<AnalyzedFunction>()
//        every { analyzedFunction.variables } returns setOf(analyzedVar1, analyzedVar2, analyzedVar3, analyzedVar4)
//        every { analyzedFunction.auxVariables } returns mutableSetOf(staticLinkVariable)
//        every { analyzedFunction.variablesUsedInNestedFunctions } returns setOf(var1, var3)
//        every { analyzedFunction.declaredVariables() } returns listOf(analyzedVar1, analyzedVar2, analyzedVar3, analyzedVar4)
//
//        val handler = makeDefaultHandler(funDef, analyzedFunction, escapeAnalysis = setOf(var1, var2))
//
//        val var1Alloc = handler.getVariableAllocation(var1)
//        require(var1Alloc is VariableAllocation.ViaPointer)
//        assertThat(var1Alloc.offset).isZero()
//        val insideAlloc = var1Alloc.pointer
//        require(insideAlloc is VariableAllocation.OnStack)
//
//        val var2Alloc = handler.getVariableAllocation(var2)
//        require(var2Alloc is VariableAllocation.ViaPointer)
//        require(var2Alloc.pointer is VariableAllocation.InRegister)
//        assertThat(var2Alloc.offset).isZero()
//
//        val var3Alloc = handler.getVariableAllocation(var3)
//        require(var3Alloc is VariableAllocation.OnStack)
//
//        // pointer to var1 and var3 must be on stack, we do not care in which order
//        assertThat(listOf(insideAlloc.offset, var3Alloc.offset)).containsExactlyInAnyOrder(16, 24)
//
//        val var4Alloc = handler.getVariableAllocation(var4)
//        require(var4Alloc is VariableAllocation.InRegister)
//    }
//
//    @Nested
//    inner class GenerateVariableAccess {
//        @Test
//        fun `generates variable access to its own source variable allocated in a register`() {
//            // let f = [] -> Int => (
//            //     let x = 10; #allocated in virtual register
//            //     x #variable access
//            // )
//
//            // given
//            val xDef = variableDeclaration("x", lit(10))
//            val xVariable = Variable.PrimitiveVariable()
//            val fDef =
//                unitFunctionDefinition(
//                    "f",
//                    emptyList(),
//                    block(
//                        xDef,
//                        variableUse("x"),
//                    ),
//                )
//            val xAnalyzed = AnalyzedVariable(xVariable, fDef, VariableUseType.READ_WRITE)
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    null,
//                    setOf(xAnalyzed),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    emptySet(),
//                )
//            val xAllocation = Register.VirtualRegister()
//
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed, mapOf(xDef to xVariable))
//            fHandler.registerVariableAllocation(
//                xVariable,
//                VariableAllocation.InRegister(xAllocation),
//            )
//
//            // when
//            val xAccess = fHandler.generateVariableAccess(xVariable)
//
//            // then
//            assertThat(xAccess).isEqualTo(CFGNode.RegisterUse(xAllocation))
//        }
//
//        @Test
//        fun `generates variable access to its own source variable allocated on stack`() {
//            // let f = [] -> Int => (
//            //     let x: Int = 10; #allocated on stack
//            //     x #variable access
//            // )
//
//            // given
//            val xDef = variableDeclaration("x", lit(10))
//            val xVariable = Variable.PrimitiveVariable()
//            val fDef =
//                unitFunctionDefinition(
//                    "f",
//                    emptyList(),
//                    block(
//                        xDef,
//                        variableUse("x"),
//                    ),
//                )
//            val xAnalyzed = AnalyzedVariable(xVariable, fDef, VariableUseType.READ_WRITE)
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    null,
//                    setOf(xAnalyzed),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    emptySet(),
//                )
//
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed, mapOf(xDef to xVariable))
//            fHandler.registerVariableAllocation(
//                xVariable,
//                VariableAllocation.OnStack(24),
//            )
//
//            // when
//            val xAccess = fHandler.generateVariableAccess(xVariable)
//
//            // then
//            assertThat(xAccess).isEqualTo(
//                CFGNode.MemoryAccess(
//                    // [rbp - 24]
//                    CFGNode.Subtraction(
//                        CFGNode.RegisterUse(Register.FixedRegister(HardwareRegister.RBP)),
//                        CFGNode.ConstantKnown(24),
//                    ),
//                ),
//            )
//        }
//
//        @Test
//        fun `generates variable access to source variable of ancestor function`() {
//            // let h = [] -> Int => (
//            //     let x = 10; #allocated on stack
//            //     let g = [] -> Int => (
//            //         let f = [] -> Int => (
//            //             x #variable access
//            //         );
//            //     );
//            // )
//
//            // given
//            val xDef = variableDeclaration("x", lit(10))
//            val xVariable = Variable.PrimitiveVariable()
//            val fDef =
//                unitFunctionDefinition(
//                    "f",
//                    emptyList(),
//                    variableUse("x"),
//                )
//            val gDef =
//                unitFunctionDefinition(
//                    "g",
//                    emptyList(),
//                    fDef,
//                )
//            val hDef =
//                unitFunctionDefinition(
//                    "h",
//                    emptyList(),
//                    block(xDef, gDef),
//                )
//
//            val xAnalyzed = AnalyzedVariable(xVariable, hDef, VariableUseType.READ_WRITE)
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    ParentLink(gDef, true),
//                    setOf(xAnalyzed),
//                    emptyList(),
//                    mutableSetOf(),
//                    2,
//                    emptySet(),
//                )
//            val gAnalyzed =
//                AnalyzedFunction(
//                    gDef,
//                    ParentLink(hDef, true),
//                    setOf(xAnalyzed),
//                    emptyList(),
//                    mutableSetOf(),
//                    1,
//                    setOf(xVariable),
//                )
//            val hAnalyzed =
//                AnalyzedFunction(
//                    hDef,
//                    null,
//                    setOf(xAnalyzed),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    setOf(xVariable),
//                )
//
//            val definitionMap = mapOf<Definition, Variable.PrimitiveVariable>(xDef to xVariable)
//            val hHandler = makeDefaultHandler(hDef, hAnalyzed, definitionMap, emptyList())
//            val gHandler = makeDefaultHandler(gDef, gAnalyzed, definitionMap, listOf(hHandler))
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed, definitionMap, listOf(gHandler, hHandler))
//
//            hHandler.registerVariableAllocation(
//                xVariable,
//                VariableAllocation.OnStack(24),
//            )
//
//            // when
//            val xAccess = fHandler.generateVariableAccess(xVariable)
//
//            // then
//            assertThat(xAccess).isEqualTo(
//                // [[[rbp - 8] - 8] - 24]
//                memoryAccess(
//                    memoryAccess(
//                        memoryAccess(
//                            registerUse(rbp) sub integer(8),
//                        ) sub integer(8),
//                    ) sub integer(24),
//                ),
//            )
//        }
//
//        @Test
//        fun `generates variable access to its own static link`() {
//            // let f = [] -> Int => 42 #request static link of f
//
//            // given
//            val fDef =
//                unitFunctionDefinition(
//                    "f",
//                    emptyList(),
//                    lit(42),
//                )
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    null,
//                    emptySet(),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    emptySet(),
//                )
//
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed)
//
//            // when
//            val staticLinkAccess = fHandler.generateVariableAccess(fHandler.getStaticLink())
//
//            // then
//            assertThat(staticLinkAccess).isEqualTo(
//                // [rbp - 8]
//                memoryAccess(registerUse(rbp) sub integer(8)),
//            )
//        }
//
//        @Test
//        fun `generates variable access to static link of its ancestor`() {
//            // let g = [] -> Int => (
//            //   let f = [] -> Int => 42 #request static link of g
//            //                           #should recursively request static link of f
//            // );
//
//            // given
//            val fDef =
//                unitFunctionDefinition(
//                    "f",
//                    emptyList(),
//                    lit(42),
//                )
//            val gDef =
//                unitFunctionDefinition(
//                    "g",
//                    emptyList(),
//                    fDef,
//                )
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    ParentLink(gDef, true),
//                    emptySet(),
//                    emptyList(),
//                    mutableSetOf(),
//                    1,
//                    emptySet(),
//                )
//            val gAnalyzed =
//                AnalyzedFunction(
//                    gDef,
//                    null,
//                    emptySet(),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    emptySet(),
//                )
//
//            val gHandler = makeDefaultHandler(gDef, gAnalyzed, emptyMap(), emptyList())
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed, emptyMap(), listOf(gHandler))
//
//            // when
//            val staticLinkAccess = fHandler.generateVariableAccess(gHandler.getStaticLink())
//
//            // then
//            assertThat(staticLinkAccess).isEqualTo(
//                // [[rbp - 8] - 8]
//                memoryAccess(memoryAccess(registerUse(rbp) sub integer(8)) sub integer(8)),
//            )
//        }
//
//        @Test
//        fun `calls its sibling with static link to parent `() {
//            // let g = [] -> Int => (
//            //   let h = [] -> Int => 42
//            //   let f = [] -> Int => h[]  # should pass a static link to parent
//            // );
//
//            // given
//            val hDef =
//                unitFunctionDefinition(
//                    "h",
//                    emptyList(),
//                    lit(42),
//                )
//            val fDef =
//                unitFunctionDefinition(
//                    "f",
//                    emptyList(),
//                    call(variableUse("h")),
//                )
//            val gDef =
//                unitFunctionDefinition(
//                    "g",
//                    emptyList(),
//                    block(hDef, fDef),
//                )
//            val hAnalyzed =
//                AnalyzedFunction(
//                    hDef,
//                    ParentLink(gDef, true),
//                    emptySet(),
//                    emptyList(),
//                    mutableSetOf(),
//                    1,
//                    emptySet(),
//                )
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    ParentLink(gDef, true),
//                    emptySet(),
//                    emptyList(),
//                    mutableSetOf(),
//                    1,
//                    emptySet(),
//                )
//            val gAnalyzed =
//                AnalyzedFunction(
//                    gDef,
//                    null,
//                    emptySet(),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    emptySet(),
//                )
//
//            val gHandler = makeDefaultHandler(gDef, gAnalyzed, emptyMap(), emptyList())
//            val hHandler = makeDefaultHandler(hDef, hAnalyzed, emptyMap(), listOf(gHandler))
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed, emptyMap(), listOf(gHandler))
//
//            mockkStatic(::generateCall)
//            // when
//            generateCallFrom(fHandler, hDef, hHandler, emptyList(), null)
//            verify {
//                generateCall(
//                    any(),
//                    match {
//                        if (it.size != 1) false
//                        else {
//                            val l = it.first()
//                            l is SimpleLayout &&
//                                l.access == memoryAccess(registerUse(rbp) sub integer(8))
//                        }
//                    },
//                    any(),
//                    any(),
//                )
//            }
//            unmockkStatic(::generateCall)
//        }
//
//        @Test
//        fun `throws if requested access to variable that is not accessible`() {
//            // given
//            val fDef =
//                unitFunctionDefinition(
//                    "f",
//                    emptyList(),
//                    lit(42),
//                )
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    null,
//                    emptySet(),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    emptySet(),
//                )
//
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed, emptyMap(), emptyList())
//
//            // when & then
//            org.junit.jupiter.api.assertThrows<GenerateVariableAccessException> {
//                fHandler.generateVariableAccess(
//                    Variable.PrimitiveVariable(),
//                )
//            }
//        }
//
//        @Test
//        fun `throws if requested generation access of variable other than source variable or static link`() {
//            // given
//            val fDef = unitFunctionDefinition("f", emptyList(), lit(42))
//            val fAnalyzed =
//                AnalyzedFunction(
//                    fDef,
//                    null,
//                    setOf(),
//                    emptyList(),
//                    mutableSetOf(),
//                    0,
//                    emptySet(),
//                )
//            val fHandler = makeDefaultHandler(fDef, fAnalyzed)
//
//            // when & then
//            org.junit.jupiter.api.assertThrows<GenerateVariableAccessException> {
//                fHandler.generateVariableAccess(Variable.PrimitiveVariable())
//            }
//        }
//    }
}
