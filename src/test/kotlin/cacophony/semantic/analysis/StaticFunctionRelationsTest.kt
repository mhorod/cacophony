package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.*
import cacophony.semantic.syntaxtree.*
import cacophony.semantic.types.ResolvedVariables
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StaticFunctionRelationsTest {
    @Test
    fun `should find variable in function`() {
        // given
        // f => let a
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", aDeclaration)
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to progVariable, aDeclaration to aVariable, funF to fVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(aVariable),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find variable read`() {
        // given
        // f => let a, a
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funF = unitFunctionDefinition("f", block(aDeclaration, varAUse))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, aDeclaration to aVariable, funF to fVariable),
                mapOf(varAUse to aVariable),
            )

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(aVariable),
                            setOf(UsedVariable(aVariable, VariableUseType.READ)),
                        ),
                ),
            )
    }

    @Test
    fun `should find variable write`() {
        // given
        // f => (let a; a = ())
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val varAWrite = variableWrite(varAUse)
        val funF = unitFunctionDefinition("f", block(aDeclaration, varAWrite))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, aDeclaration to aVariable, funF to fVariable),
                mapOf(varAUse to aVariable),
            )

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(aVariable),
                            setOf(UsedVariable(aVariable, VariableUseType.WRITE)),
                        ),
                ),
            )
    }

    @Test
    fun `should find variable read and write`() {
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
        val funF = unitFunctionDefinition("f", block(aDeclaration, varAWrite, varAUse2))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse1 to aDeclaration, varAUse2 to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to progVariable, aDeclaration to aVariable, funF to fVariable),
                mapOf(
                    varAUse1 to aVariable,
                    varAUse2 to aVariable,
                ),
            )

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(aVariable),
                            setOf(
                                UsedVariable(aVariable, VariableUseType.WRITE),
                                UsedVariable(aVariable, VariableUseType.READ),
                            ),
                        ),
                ),
            )
    }

    @Test
    fun `should find multiple variables in block`() {
        // given
        // f => ( let a; let b; let c )
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val bDeclaration = variableDeclaration("b", Empty(mockRange()))
        val cDeclaration = variableDeclaration("c", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.PrimitiveVariable()
        val cVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", block(aDeclaration, bDeclaration, cDeclaration))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to progVariable,
                    funF to fVariable,
                    aDeclaration to aVariable,
                    bDeclaration to bVariable,
                    cDeclaration to cVariable,
                ),
            )

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(aVariable, bVariable, cVariable),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find struct variables`() {
        // given
        //       s
        //      / \
        //     /   \
        //    a     b
        //    |    / \
        //    c   d   e
        //        |
        //        f
        //
        //
        // f => ( let s, s.a = s.b.d)
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val sDeclaration = variableDeclaration("s", Empty(mockRange()))
        val fVariable = Variable.PrimitiveVariable()
        val eVariable = Variable.PrimitiveVariable()
        val dVariable = Variable.StructVariable(mapOf("f" to fVariable))
        val cVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.StructVariable(mapOf("d" to dVariable, "e" to eVariable))
        val aVariable = Variable.StructVariable(mapOf("c" to cVariable))
        val sVariable = Variable.StructVariable(mapOf("a" to aVariable, "b" to bVariable))

        val varSUse1 = variableUse("s")
        val varSUse2 = variableUse("s")

        val saLValue =
            FieldRef.LValue(
                mockRange(),
                varSUse1,
                "a",
            )
        val sbLValue =
            FieldRef.LValue(
                mockRange(),
                varSUse2,
                "b",
            )
        val sbdLValue =
            FieldRef.LValue(
                mockRange(),
                sbLValue,
                "d",
            )

        val funF =
            unitFunctionDefinition(
                "f",
                block(
                    sDeclaration,
                    OperatorBinary.Assignment(
                        mockRange(),
                        saLValue,
                        sbdLValue,
                    ),
                ),
            )
        val fCode = primVar()
        val fLink = primVar()
        val funFVariable = Variable.FunctionVariable("f", fCode, fLink)

        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables =
            mapOf(
                varSUse1 to sDeclaration,
                varSUse2 to sDeclaration,
            )
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to progVariable,
                    funF to funFVariable,
                    sDeclaration to sVariable,
                ),
                mapOf(
                    varSUse1 to sVariable,
                    varSUse2 to sVariable,
                    saLValue to aVariable,
                    sbLValue to bVariable,
                    sbdLValue to dVariable,
                ),
            )

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(funFVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(sVariable, aVariable, bVariable, cVariable, dVariable, eVariable, fVariable),
                            setOf(
                                UsedVariable(sVariable, VariableUseType.READ),
                                UsedVariable(bVariable, VariableUseType.READ),
                                UsedVariable(dVariable, VariableUseType.READ),
                                UsedVariable(fVariable, VariableUseType.READ),
                                UsedVariable(sVariable, VariableUseType.WRITE),
                                UsedVariable(aVariable, VariableUseType.WRITE),
                                UsedVariable(cVariable, VariableUseType.WRITE),
                            ),
                        ),
                ),
            )
    }

    @Test
    fun `should find nested function`() {
        // given
        // f => ( g => () )
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val funG = unitFunctionDefinition("g", Empty(mockRange()))
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funF = unitFunctionDefinition("f", funG)
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to progVariable, funF to fVariable, funG to gVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(gVariable, gCode, gLink),
                            emptySet(),
                        ),
                    funG.value to
                        StaticFunctionRelations(
                            funF.value,
                            2,
                            emptySet(),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find multiple nested functions in block`() {
        // given
        // f => ( g => (); h => () )
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val funG = unitFunctionDefinition("g", Empty(mockRange()))
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funH = unitFunctionDefinition("h", Empty(mockRange()))
        val hCode = primVar()
        val hLink = primVar()
        val hVariable = Variable.FunctionVariable("h", hCode, hLink)
        val funF = unitFunctionDefinition("f", block(funG, funH))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(mapOf(program to progVariable, funF to fVariable, funG to gVariable, funH to hVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(gVariable, hVariable, gCode, gLink, hCode, hLink),
                            emptySet(),
                        ),
                    funG.value to
                        StaticFunctionRelations(
                            funF.value,
                            2,
                            emptySet(),
                            emptySet(),
                        ),
                    funH.value to
                        StaticFunctionRelations(
                            funF.value,
                            2,
                            emptySet(),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze top level variables`() {
        // given
        // (let a; f => ())
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", Empty(mockRange()))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to progVariable, funF to fVariable, aDeclaration to aVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink, aVariable),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            emptySet(),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find variables and functions in nested functions`() {
        // given
        // f => (let a; g => ( let b; h => ( let c ) ) )
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val bDeclaration = variableDeclaration("b", Empty(mockRange()))
        val cDeclaration = variableDeclaration("C", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.PrimitiveVariable()
        val cVariable = Variable.PrimitiveVariable()
        val funH = unitFunctionDefinition("h", block(cDeclaration))
        val hCode = primVar()
        val hLink = primVar()
        val hVariable = Variable.FunctionVariable("h", hCode, hLink)
        val funG = unitFunctionDefinition("g", block(bDeclaration, funH))
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funF = unitFunctionDefinition("f", block(aDeclaration, funG))
        val fCode = primVar()
        val fLink = primVar()
        val fVariable = Variable.FunctionVariable("f", fCode, fLink)

        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to progVariable,
                    funF to fVariable,
                    funG to gVariable,
                    funH to hVariable,
                    aDeclaration to aVariable,
                    bDeclaration to bVariable,
                    cDeclaration to cVariable,
                ),
            )

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                program.value to
                    StaticFunctionRelations(
                        null,
                        0,
                        setOf(fVariable, fCode, fLink),
                        emptySet(),
                    ),
                funF.value to
                    StaticFunctionRelations(
                        program.value,
                        1,
                        setOf(gVariable, gCode, gLink, aVariable),
                        emptySet(),
                    ),
                funG.value to
                    StaticFunctionRelations(
                        funF.value,
                        2,
                        setOf(hVariable, hCode, hLink, bVariable),
                        emptySet(),
                    ),
                funH.value to
                    StaticFunctionRelations(
                        funG.value,
                        3,
                        setOf(cVariable),
                        emptySet(),
                    ),
            ),
        )
    }

    @Test
    fun `should find relations in complex nested functions`() {
        // given
        // (foo => (let a; g => h => a; i => (j => (); g())); main => foo())
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
        val funG = unitFunctionDefinition("g", funH)
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funJ = unitFunctionDefinition("j", block())
        val jCode = primVar()
        val jLink = primVar()
        val jVariable = Variable.FunctionVariable("j", jCode, jLink)
        val varGUse = variableUse("g")
        val funI = unitFunctionDefinition("i", block(funJ, call(varGUse)))
        val iCode = primVar()
        val iLink = primVar()
        val iVariable = Variable.FunctionVariable("i", iCode, iLink)
        val funFoo = unitFunctionDefinition("foo", block(aDeclaration, funG, funI))
        val fooCode = primVar()
        val fooLink = primVar()
        val fooVariable = Variable.FunctionVariable("foo", fooCode, fooLink)
        val varFooUse = variableUse("foo")
        val funMain = unitFunctionDefinition("main", call(varFooUse))
        val mainCode = primVar()
        val mainLink = primVar()
        val mainVariable = Variable.FunctionVariable("main", mainCode, mainLink)

        val ast = astOf(funFoo, funMain)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration, varGUse to funG, varFooUse to funFoo)
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
                    varGUse to gVariable,
                    varFooUse to fooVariable,
                ),
            )

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                program.value to
                    StaticFunctionRelations(
                        null,
                        0,
                        setOf(fooVariable, fooCode, fooLink, mainVariable, mainCode, mainLink),
                        emptySet(),
                    ),
                funFoo.value to
                    StaticFunctionRelations(
                        program.value,
                        1,
                        setOf(gVariable, gCode, gLink, iVariable, iCode, iLink, aVariable),
                        emptySet(),
                    ),
                funG.value to
                    StaticFunctionRelations(
                        funFoo.value,
                        2,
                        setOf(hVariable, hCode, hLink),
                        emptySet(),
                    ),
                funH.value to
                    StaticFunctionRelations(
                        funG.value,
                        3,
                        setOf(),
                        setOf(UsedVariable(aVariable, VariableUseType.READ)),
                    ),
                funI.value to
                    StaticFunctionRelations(
                        funFoo.value,
                        2,
                        setOf(jVariable, jCode, jLink),
                        setOf(
                            UsedVariable(gVariable, VariableUseType.READ),
                            UsedVariable(gCode, VariableUseType.READ),
                            UsedVariable(gLink, VariableUseType.READ),
                        ),
                    ),
                funJ.value to
                    StaticFunctionRelations(
                        funI.value,
                        3,
                        emptySet(),
                        emptySet(),
                    ),
                funMain.value to
                    StaticFunctionRelations(
                        program.value,
                        1,
                        emptySet(),
                        setOf(
                            UsedVariable(fooVariable, VariableUseType.READ),
                            UsedVariable(fooCode, VariableUseType.READ),
                            UsedVariable(fooLink, VariableUseType.READ),
                        ),
                    ),
            ),
        )
    }

    @Test
    fun `variable defined and used in inner is not visible in outer`() {
        // given
        // f => ( g => (let a; a;) )
        val progCode = primVar()
        val progLink = primVar()
        val progVariable = Variable.FunctionVariable("program", progCode, progLink)
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funG = unitFunctionDefinition("g", block(aDeclaration, varAUse))
        val gCode = primVar()
        val gLink = primVar()
        val gVariable = Variable.FunctionVariable("g", gCode, gLink)
        val funF = unitFunctionDefinition("f", funG)
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
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(gVariable, gCode, gLink),
                            emptySet(),
                        ),
                    funG.value to
                        StaticFunctionRelations(
                            funF.value,
                            2,
                            setOf(aVariable),
                            setOf(UsedVariable(aVariable, VariableUseType.READ)),
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
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program.value to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(fVariable, fCode, fLink, aVariable, bVariable),
                            emptySet(),
                        ),
                    lambdaA to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            emptySet(),
                            setOf(UsedVariable(aVariable, VariableUseType.READ)),
                        ),
                    lambdaB to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            emptySet(),
                            setOf(UsedVariable(bVariable, VariableUseType.READ)),
                        ),
                ),
            )
    }
}
