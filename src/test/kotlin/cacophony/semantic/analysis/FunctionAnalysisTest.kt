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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val funF = unitFunctionDefinition("f", block())
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to progVariable, funF to fVariable))

        // when
        val results = analyzeFunctions(ast, resolvedVariables, variablesMap)

        // then
        assertThat(results)
            .containsAllEntriesOf(
                mapOf(
                    program.value
                        to
                        analyzedFunction(
                            program,
                            0,
                            setOf(
                                AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),
                            ),
                        ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", block(aDeclaration))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to progVariable, funF to fVariable, aDeclaration to aVariable))

        // when
        val results = analyzeFunctions(ast, resolvedVariables, variablesMap)

        // then
        assertThat(results)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value
                        to
                        analyzedFunction(
                            program,
                            0,
                            setOf(
                                AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),
                            ),
                        ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funF = unitFunctionDefinition("f", block(varAUse))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, funF to fVariable, aDeclaration to aVariable),
                mapOf(varAUse to aVariable),
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
                    program(ast).value to
                        AnalyzedFunction(
                            program(ast).value,
                            null,
                            setOf(
                                AnalyzedVariable(aVariable, program(ast).value, VariableUseType.READ),
                                AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),
                            ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val varAWrite = variableWrite(varAUse)
        val funF = unitFunctionDefinition("f", block(varAWrite))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, funF to fVariable, aDeclaration to aVariable),
                mapOf(varAUse to aVariable),
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
                            setOf(
                                AnalyzedVariable(aVariable, program.value, VariableUseType.WRITE),
                                AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),
                            ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse1 = variableUse("a")
        val varAWrite = variableWrite(varAUse1)
        val varAUse2 = variableUse("a")
        val funF = unitFunctionDefinition("f", block(varAWrite, varAUse2))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables =
            mapOf(
                varAUse1 to aDeclaration,
                varAUse2 to aDeclaration,
            )
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, funF to fVariable, aDeclaration to aVariable),
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
                            setOf(
                                AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(aVariable, program.value, VariableUseType.READ_WRITE),
                            ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val funG = unitFunctionDefinition("g", block())
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val varGUse = variableUse("g")
        val funF = unitFunctionDefinition("f", block(funG, call(varGUse)))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varGUse to funG)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, funF to fVariable, funG to gVariable),
                mapOf(varGUse to gVariable),
            )

        // when
        val result = analyzeFunctions(ast, resolvedVariables, variablesMap)

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value
                        to
                        analyzedFunction(
                            program,
                            0,
                            setOf(
                                AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),),
                        ),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(
                                program.value,
                                false,
                            ),
                            setOf(
                                AnalyzedVariable(gVariable, funF.value, VariableUseType.READ),
                                AnalyzedVariable(gCode, funF.value, VariableUseType.READ),
                                AnalyzedVariable(gLink, funF.value, VariableUseType.READ),),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funG = unitFunctionDefinition("g", varAUse)
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funF = unitFunctionDefinition("f", block(aDeclaration, funG))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, funF to fVariable, funG to gVariable, aDeclaration to aVariable),
                mapOf(varAUse to aVariable),
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
                            setOf(
                                AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            setOf(),
                        ),
                    funF.value to
                        AnalyzedFunction(
                            funF.value,
                            ParentLink(program.value, false),
                            setOf(
                                AnalyzedVariable(gVariable, funF.value, VariableUseType.UNUSED),
                                AnalyzedVariable(gCode, funF.value, VariableUseType.UNUSED),
                                AnalyzedVariable(gLink, funF.value, VariableUseType.UNUSED),
                                AnalyzedVariable(aVariable, funF.value, VariableUseType.READ),
                            ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funH = unitFunctionDefinition("h", varAUse)
        val hCode = primVar()
        val hLink = primVar()
        val hVariable = Variable.FunctionVariable("h", hCode, hLink)
        val funHUse = variableUse("h")
        val funHCall = call(funHUse)
        val funG = unitFunctionDefinition("g", block(funH, funHCall))
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funGUse = variableUse("g")
        val funJ = unitFunctionDefinition("j", block())
        val jCode = primVar()
        val jLink = primVar()
        val jVariable = Variable.FunctionVariable("j", jCode, jLink)
        val funI = unitFunctionDefinition("i", block(funJ, call(funGUse)))
        val iCode = primVar()
        val iLink = primVar()
        val iVariable = Variable.FunctionVariable("i", iCode, iLink)
        val funFoo = unitFunctionDefinition("foo", block(aDeclaration, funG, funI))
        val fooCode = primVar()
        val fooLink = primVar()
        val fooVariable = Variable.FunctionVariable("foo", fooCode, fooLink)
        val funFooUse = variableUse("foo")
        val funMain = unitFunctionDefinition("main", call(funFooUse))
        val mainCode = primVar()
        val mainLink = primVar()
        val mainVariable = Variable.FunctionVariable("main", mainCode, mainLink)

        val ast = astOf(funFoo, funMain)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration, funHUse to funH, funGUse to funG, funFooUse to funFoo)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to progVariable,
                    funH to hVariable,
                    funG to gVariable,
                    funJ to jVariable,
                    funI to iVariable,
                    funFoo to fooVariable,
                    funMain to mainVariable,
                    aDeclaration to aVariable,
                ),
                mapOf(
                    varAUse to aVariable,
                    funGUse to gVariable,
                    funHUse to hVariable,
                    funFooUse to fooVariable,
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
                            setOf(
                                AnalyzedVariable(mainVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(mainCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(mainLink, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fooVariable, program.value, VariableUseType.READ),
                                AnalyzedVariable(fooCode, program.value, VariableUseType.READ),
                                AnalyzedVariable(fooLink, program.value, VariableUseType.READ),
                            ),
                            emptyList(),
                            mutableSetOf(),
                            0,
                            setOf(fooVariable, fooCode, fooLink),
                        ),
                    funFoo.value to
                        AnalyzedFunction(
                            funFoo.value,
                            ParentLink(program.value, false),
                            setOf(
                                AnalyzedVariable(gVariable, funFoo.value, VariableUseType.READ),
                                AnalyzedVariable(gCode, funFoo.value, VariableUseType.READ),
                                AnalyzedVariable(gLink, funFoo.value, VariableUseType.READ),
                                AnalyzedVariable(iVariable, funFoo.value, VariableUseType.UNUSED),
                                AnalyzedVariable(iCode, funFoo.value, VariableUseType.UNUSED),
                                AnalyzedVariable(iLink, funFoo.value, VariableUseType.UNUSED),
                                AnalyzedVariable(aVariable, funFoo.value, VariableUseType.READ),
                            ),
                            emptyList(),
                            mutableSetOf(),
                            1,
                            setOf(aVariable, gVariable, gCode, gLink),
                        ),
                    funG.value to
                        AnalyzedFunction(
                            funG.value,
                            ParentLink(funFoo.value, true),
                            setOf(
                                AnalyzedVariable(hVariable, funG.value, VariableUseType.READ),
                                AnalyzedVariable(hCode, funG.value, VariableUseType.READ),
                                AnalyzedVariable(hLink, funG.value, VariableUseType.READ),
                                AnalyzedVariable(aVariable, funFoo.value, VariableUseType.READ),
                            ),
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
                            setOf(
                                AnalyzedVariable(gVariable, funFoo.value, VariableUseType.READ),
                                AnalyzedVariable(gCode, funFoo.value, VariableUseType.READ),
                                AnalyzedVariable(gLink, funFoo.value, VariableUseType.READ),
                                AnalyzedVariable(jVariable, funI.value, VariableUseType.UNUSED),
                                AnalyzedVariable(jCode, funI.value, VariableUseType.UNUSED),
                                AnalyzedVariable(jLink, funI.value, VariableUseType.UNUSED),
                            ),
                            emptyList(),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                    funMain.value to
                        AnalyzedFunction(
                            funMain.value,
                            ParentLink(program.value, true), // main uses parent link to access foo
                            setOf(
                                AnalyzedVariable(fooVariable, program.value, VariableUseType.READ),
                                AnalyzedVariable(fooCode, program.value, VariableUseType.READ),
                                AnalyzedVariable(fooLink, program.value, VariableUseType.READ),
                                ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val fooDeclarationA = variableDeclaration("a", Empty(mockRange()))
        val fooVariableA = Variable.PrimitiveVariable()
        val fooVariableAUse = variableUse("a")
        val barDeclarationA = variableDeclaration("a", Empty(mockRange()))
        val barVariableA = Variable.PrimitiveVariable()
        val barVariableAUse = variableUse("a")

        val funBar = unitFunctionDefinition("bar", block(barDeclarationA, variableWrite(barVariableAUse)))
        val barCode = primVar()
        val barLink = primVar()
        val barVariable = Variable.FunctionVariable("bar", barCode, barLink)
        val funFoo = unitFunctionDefinition("foo", block(fooDeclarationA, funBar, fooVariableAUse))
        val fooCode = primVar()
        val fooLink = primVar()
        val fooVariable = Variable.FunctionVariable("foo", fooCode, fooLink)
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
                    program to progVariable,
                    funFoo to fooVariable,
                    funBar to barVariable,
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
                            setOf(
                                AnalyzedVariable(fooVariable, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fooCode, program.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fooLink, program.value, VariableUseType.UNUSED),
                                ),
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
                            setOf(
                                AnalyzedVariable(barVariable, funFoo.value, VariableUseType.UNUSED),
                                AnalyzedVariable(barCode, funFoo.value, VariableUseType.UNUSED),
                                AnalyzedVariable(barLink, funFoo.value, VariableUseType.UNUSED),
                                AnalyzedVariable(fooVariableA, funFoo.value, VariableUseType.READ),
                            ),
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
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val argADeclaration = arg("a")
        val argBDeclaration = arg("b")
        val variableA = Variable.PrimitiveVariable()
        val variableB = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val varBUse = variableUse("b")
        val funG = unitFunctionDefinition("g", block(varAUse, variableWrite(varBUse)))
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funF = unitFunctionDefinition("f", listOf(argADeclaration, argBDeclaration), funG)
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)

        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to argADeclaration, varBUse to argBDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to progVariable,
                    funF to fVariable,
                    funG to gVariable,
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
                program.value
                    to
                    analyzedFunction(
                        program,
                        0,
                        setOf(
                            AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                            AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                            AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),
                            ),
                    ),
                funF.value to
                    AnalyzedFunction(
                        funF.value,
                        ParentLink(program.value, false),
                        setOf(
                            AnalyzedVariable(gVariable, funF.value, VariableUseType.UNUSED),
                            AnalyzedVariable(gCode, funF.value, VariableUseType.UNUSED),
                            AnalyzedVariable(gLink, funF.value, VariableUseType.UNUSED),
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

    @Test
    fun `should find anonymous lambdas`() {
        // given
        // let a; let b;
        // let f = if true then ([] -> Unit => a) else ([] -> Unit => b)
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val bDeclaration = variableDeclaration("b", Empty(mockRange()))
        val bVariable = Variable.PrimitiveVariable()
        val varBUse = variableUse("b")
        val lambdaA = lambda(emptyList(), basicType("Unit"), varAUse)
        val lambdaB = lambda(emptyList(), basicType("Unit"), varBUse)

        val condition = ifThenElse(lit(true), lambdaA, lambdaB)
        val fDeclaration = variableDeclaration("f", condition)
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(aDeclaration, bDeclaration, fDeclaration)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, fDeclaration to fVariable, aDeclaration to aVariable, bDeclaration to bVariable),
                mapOf(varAUse to aVariable, varBUse to bVariable),
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
                program.value to
                    AnalyzedFunction(
                        program.value,
                        null,
                        setOf(
                            AnalyzedVariable(fVariable, program.value, VariableUseType.UNUSED),
                            AnalyzedVariable(fCode, program.value, VariableUseType.UNUSED),
                            AnalyzedVariable(fLink, program.value, VariableUseType.UNUSED),
                            AnalyzedVariable(aVariable, program.value, VariableUseType.READ),
                            AnalyzedVariable(bVariable, program.value, VariableUseType.READ),
                        ),
                        listOf(),
                        mutableSetOf(),
                        0,
                        setOf(aVariable, bVariable),
                    ),
                lambdaA to
                    AnalyzedFunction(
                        lambdaA,
                        ParentLink(program.value, true),
                        setOf(
                            AnalyzedVariable(aVariable, program.value, VariableUseType.READ),
                        ),
                        listOf(),
                        mutableSetOf(),
                        1,
                        emptySet(),
                    ),
                lambdaB to
                    AnalyzedFunction(
                        lambdaB,
                        ParentLink(program.value, true),
                        setOf(
                            AnalyzedVariable(bVariable, program.value, VariableUseType.READ),
                        ),
                        listOf(),
                        mutableSetOf(),
                        1,
                        emptySet(),
                    ),
            ),
        )
    }
}
