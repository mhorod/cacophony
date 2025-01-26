package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.*
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunctionAnalysisTest {
    @Test
    fun `should analyze empty function`() {
        // given
        // f => {}
        val funF = unitFunctionDefinition("f", block())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap()

        // when
        val results = analyzeFunctions(ast, resolvedVariables, variablesMap)

        // then
        assertThat(results)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    programFunctionAnalysis(ast),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(program.value, false),
                            emptySet(),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with unused variable`() {
        // given
        // f => let a
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", block(aDeclaration))
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable))

        // when
        val results = analyzeFunctions(ast, resolvedVariables, variablesMap)

        // then
        assertThat(results)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    programFunctionAnalysis(ast),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(program.value, false),
                            setOf(AnalyzedVariable(aVariable, funF.value, VariableUseType.UNUSED)),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with read variable`() {
        // given
        // f => (let a; a)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funF = unitFunctionDefinition("f", block(varAUse))
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable), mapOf(varAUse to aVariable))

        // when
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program(ast).value to
                        AnalyzedFunction(
                            program(ast).value,
                            null,
                            setOf(AnalyzedVariable(aVariable, program(ast).value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            setOf(aVariable),
                        ),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(program.value, true),
                            setOf(AnalyzedVariable(aVariable, program.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with written variable`() {
        // given
        // f => (let a; a = ())
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val varAWrite = variableWrite(varAUse)
        val funF = unitFunctionDefinition("f", block(varAWrite))
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable), mapOf(varAUse to aVariable))

        // when
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        AnalyzedFunction(
                            program.value,
                            null,
                            setOf(AnalyzedVariable(aVariable, program.value, VariableUseType.WRITE)),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            setOf(aVariable),
                        ),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(program.value, true),
                            setOf(AnalyzedVariable(aVariable, program.value, VariableUseType.WRITE)),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with read and written variable`() {
        // given
        // f => (let a; a = (); a)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse1 = variableUse("a")
        val varAWrite = variableWrite(varAUse1)
        val varAUse2 = variableUse("a")
        val funF = unitFunctionDefinition("f", block(varAWrite, varAUse2))
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables =
            mapOf(
                varAUse1 to aDeclaration,
                varAUse2 to aDeclaration,
            )
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(aDeclaration to aVariable),
                mapOf(varAUse1 to aVariable, varAUse2 to aVariable),
            )

        // when
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        AnalyzedFunction(
                            program.value,
                            null,
                            setOf(AnalyzedVariable(aVariable, program.value, VariableUseType.READ_WRITE)),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            setOf(aVariable),
                        ),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(
                                program.value,
                                true,
                            ),
                            setOf(AnalyzedVariable(aVariable, program.value, VariableUseType.READ_WRITE)),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with nested function`() {
        // given
        // f => (g => (); g())
        val funG = unitFunctionDefinition("g", block())
        val varGUse = variableUse("g")
        val funF = unitFunctionDefinition("f", block(funG, call(varGUse)))
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varGUse to funG)
        val variablesMap: VariablesMap = createVariablesMap()

        // when
        val result = analyzeFunctions(ast, resolvedVariables, variablesMap)

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    programFunctionAnalysis(ast),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(
                                program.value,
                                false,
                            ),
                            emptySet(),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                    funG.value to
                        AnalyzedFunction(
                            funG.value,
                            ParentLink(funF.value, false),
                            emptySet(),
                            emptyList(),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with nested function using parent variable`() {
        // given
        // f => (let a; g => a)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funG = unitFunctionDefinition("g", varAUse)
        val funF = unitFunctionDefinition("f", block(aDeclaration, funG))
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable), mapOf(varAUse to aVariable))

        // when
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        AnalyzedFunction(
                            program.value,
                            null,
                            setOf(),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            setOf(),
                        ),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(program.value, false),
                            setOf(AnalyzedVariable(aVariable, funF.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            setOf(aVariable),
                        ),
                    funG.value to
                        AnalyzedFunction(
                            funG.value,
                            ParentLink(funF.value, true),
                            setOf(AnalyzedVariable(aVariable, funF.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find transitive parent link usages`() {
        // given
        // (foo => (let a; g => (h => a; h()); i => (j => (); g())); main => foo())
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funH = unitFunctionDefinition("h", varAUse)
        val funHUse = variableUse("h")
        val funHCall = call(funHUse)
        val funG = unitFunctionDefinition("g", block(funH, funHCall))
        val funGUse = variableUse("g")
        val funJ = unitFunctionDefinition("j", block())
        val funI = unitFunctionDefinition("i", block(funJ, call(funGUse)))
        val funFoo = unitFunctionDefinition("foo", block(aDeclaration, funG, funI))
        val funFooUse = variableUse("foo")
        val funMain = unitFunctionDefinition("main", call(funFooUse))

        val ast = astOf(funFoo, funMain)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration, funHUse to funH, funGUse to funG, funFooUse to funFoo)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable), mapOf(varAUse to aVariable))

        // when
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        AnalyzedFunction(
                            program.value,
                            null,
                            setOf(),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                    funFoo.value to
                        AnalyzedFunction(
                            funFoo.value,
                            ParentLink(program.value, false),
                            setOf(AnalyzedVariable(aVariable, funFoo.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            setOf(aVariable),
                        ),
                    funG.value to
                        AnalyzedFunction(
                            funG.value,
                            ParentLink(funFoo.value, true),
                            setOf(AnalyzedVariable(aVariable, funFoo.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                    funH.value to
                        AnalyzedFunction(
                            funH.value,
                            ParentLink(funG.value, true),
                            setOf(AnalyzedVariable(aVariable, funFoo.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            3,
                            emptySet(),
                        ),
                    funJ.value to
                        AnalyzedFunction(
                            funJ.value,
                            ParentLink(funI.value, false),
                            emptySet(),
                            emptyList(),
                            mutableSetOf(),
                            3,
                            emptySet(),
                        ),
                    funI.value to
                        AnalyzedFunction(
                            funI.value,
                            ParentLink(funFoo.value, true),
                            setOf(AnalyzedVariable(aVariable, funFoo.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                    funMain.value to
                        AnalyzedFunction(
                            funMain.value,
                            ParentLink(program.value, false),
                            emptySet(),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should return separate results for distinct variables with equal names`() {
        // given
        // foo => (let a; bar => (let a; a = ()); a)
        val fooDeclarationA = variableDeclaration("a", Empty(mockRange()))
        val fooVariableA = Variable.PrimitiveVariable()
        val fooVariableAUse = variableUse("a")
        val barDeclarationA = variableDeclaration("a", Empty(mockRange()))
        val barVariableA = Variable.PrimitiveVariable()
        val barVariableAUse = variableUse("a")

        val funBar = unitFunctionDefinition("bar", block(barDeclarationA, variableWrite(barVariableAUse)))
        val funFoo = unitFunctionDefinition("foo", block(fooDeclarationA, funBar, fooVariableAUse))
        val ast = astOf(funFoo)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables =
            mapOf(
                fooVariableAUse to fooDeclarationA,
                barVariableAUse to barDeclarationA,
            )
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    fooDeclarationA to fooVariableA,
                    barDeclarationA to barVariableA,
                ),
                mapOf(
                    fooVariableAUse to fooVariableA,
                    barVariableAUse to barVariableA,
                ),
            )

        // when
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        AnalyzedFunction(
                            program.value,
                            null,
                            setOf(),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                    funFoo.value to
                        AnalyzedFunction(
                            funFoo.value,
                            ParentLink(
                                program.value,
                                false,
                            ),
                            setOf(AnalyzedVariable(fooVariableA, funFoo.value, VariableUseType.READ)),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                    funBar.value to
                        AnalyzedFunction(
                            funBar.value,
                            ParentLink(funFoo.value, false),
                            setOf(
                                AnalyzedVariable(barVariableA, funBar.value, VariableUseType.WRITE),
                            ),
                            emptyList(),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find uses of parent function argument`() {
        // given
        // f[a, b] => (g => (a; b = ()))
        val argADeclaration = arg("a")
        val argBDeclaration = arg("b")
        val variableA = Variable.PrimitiveVariable()
        val variableB = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val varBUse = variableUse("b")
        val funG = unitFunctionDefinition("g", block(varAUse, variableWrite(varBUse)))
        val funF = unitFunctionDefinition("f", listOf(argADeclaration, argBDeclaration), funG)

        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to argADeclaration, varBUse to argBDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    argADeclaration to variableA,
                    argBDeclaration to variableB,
                ),
                mapOf(
                    varAUse to variableA,
                    varBUse to variableB,
                ),
            )

        // when
        val result =
            analyzeFunctions(
                ast,
                resolvedVariables,
                variablesMap,
            )

        // then
        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                programFunctionAnalysis(ast),
                funF.value to
                    AnalyzedFunction(
                        funF.value,
                        ParentLink(program.value, false),
                        setOf(
                            AnalyzedVariable(variableA, funF.value, VariableUseType.READ),
                            AnalyzedVariable(variableB, funF.value, VariableUseType.WRITE),
                        ),
                        listOf(argADeclaration, argBDeclaration),
                        mutableSetOf(),
                        1,
                        setOf(variableA, variableB),
                    ),
                funG.value to
                    AnalyzedFunction(
                        funG.value,
                        ParentLink(funF.value, true),
                        setOf(
                            AnalyzedVariable(variableA, funF.value, VariableUseType.READ),
                            AnalyzedVariable(variableB, funF.value, VariableUseType.WRITE),
                        ),
                        emptyList(),
                        mutableSetOf(),
                        2,
                        emptySet(),
                    ),
            ),
        )
    }
}
