package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.*
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StaticFunctionRelationsTest {
    @Test
    fun `should find variable in function`() {
        // given
        // f => let a
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", aDeclaration)
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to mockk(), aDeclaration to aVariable, funF to fVariable))

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
                            setOf(fVariable),
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funF = unitFunctionDefinition("f", block(aDeclaration, varAUse))
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to mockk(), aDeclaration to aVariable, funF to fVariable),
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
                            setOf(fVariable),
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val varAWrite = variableWrite(varAUse)
        val funF = unitFunctionDefinition("f", block(aDeclaration, varAWrite))
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to mockk(), aDeclaration to aVariable, funF to fVariable),
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
                            setOf(fVariable),
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse1 = variableUse("a")
        val varAWrite = variableWrite(varAUse1)

        val varAUse2 = variableUse("a")
        val funF = unitFunctionDefinition("f", block(aDeclaration, varAWrite, varAUse2))
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse1 to aDeclaration, varAUse2 to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to mockk(), aDeclaration to aVariable, funF to fVariable),
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
                            setOf(fVariable),
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val bDeclaration = variableDeclaration("b", Empty(mockRange()))
        val cDeclaration = variableDeclaration("c", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.PrimitiveVariable()
        val cVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", block(aDeclaration, bDeclaration, cDeclaration))
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to mockk(),
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
                            setOf(fVariable),
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
        val funFVariable = Variable.FunctionVariable("f", mockk(), mockk())

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
                    program to mockk(),
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
                            setOf(funFVariable),
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
        val funG = unitFunctionDefinition("g", Empty(mockRange()))
        val gVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val funF = unitFunctionDefinition("f", funG)
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to mockk(), funF to fVariable, funG to gVariable))

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
                            setOf(fVariable),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(gVariable),
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
        val funG = unitFunctionDefinition("g", Empty(mockRange()))
        val gVariable = Variable.FunctionVariable("g", mockk(), mockk())
        val funH = unitFunctionDefinition("h", Empty(mockRange()))
        val hVariable = Variable.FunctionVariable("h", mockk(), mockk())
        val funF = unitFunctionDefinition("f", block(funG, funH))
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(mapOf(program to mockk(), funF to fVariable, funG to gVariable, funH to hVariable))

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
                            setOf(fVariable),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(gVariable, hVariable),
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val funF = unitFunctionDefinition("f", Empty(mockRange()))
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(program to mockk(), funF to fVariable, aDeclaration to aVariable))

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
                            setOf(fVariable, aVariable),
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val bDeclaration = variableDeclaration("b", Empty(mockRange()))
        val cDeclaration = variableDeclaration("C", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.PrimitiveVariable()
        val cVariable = Variable.PrimitiveVariable()
        val funH = unitFunctionDefinition("h", block(cDeclaration))
        val hVariable = Variable.FunctionVariable("h", mockk(), mockk())
        val funG = unitFunctionDefinition("g", block(bDeclaration, funH))
        val gVariable = Variable.FunctionVariable("g", mockk(), mockk())
        val funF = unitFunctionDefinition("f", block(aDeclaration, funG))
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())

        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to mockk(),
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
                        setOf(fVariable),
                        emptySet(),
                    ),
                funF.value to
                    StaticFunctionRelations(
                        program.value,
                        1,
                        setOf(gVariable, aVariable),
                        emptySet(),
                    ),
                funG.value to
                    StaticFunctionRelations(
                        funF.value,
                        2,
                        setOf(hVariable, bVariable),
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funH = unitFunctionDefinition("h", varAUse)
        val hVariable = Variable.FunctionVariable("h", mockk(), mockk())
        val funG = unitFunctionDefinition("g", funH)
        val gVariable = Variable.FunctionVariable("g", mockk(), mockk())
        val funJ = unitFunctionDefinition("j", block())
        val jVariable = Variable.FunctionVariable("j", mockk(), mockk())
        val varGUse = variableUse("g")
        val funI = unitFunctionDefinition("i", block(funJ, call(varGUse)))
        val iVariable = Variable.FunctionVariable("i", mockk(), mockk())
        val funFoo = unitFunctionDefinition("foo", block(aDeclaration, funG, funI))
        val fooVariable = Variable.FunctionVariable("foo", mockk(), mockk())
        val varFooUse = variableUse("foo")
        val funMain = unitFunctionDefinition("main", call(varFooUse))
        val mainVariable = Variable.FunctionVariable("main", mockk(), mockk())

        val ast = astOf(funFoo, funMain)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration, varGUse to funG, varFooUse to funFoo)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    program to mockk(),
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
                        setOf(fooVariable, mainVariable),
                        emptySet(),
                    ),
                funFoo.value to
                    StaticFunctionRelations(
                        program.value,
                        1,
                        setOf(gVariable, iVariable, aVariable),
                        emptySet(),
                    ),
                funG.value to
                    StaticFunctionRelations(
                        funFoo.value,
                        2,
                        setOf(hVariable),
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
                        setOf(jVariable),
                        setOf(UsedVariable(gVariable, VariableUseType.READ)),
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
                        setOf(UsedVariable(fooVariable, VariableUseType.READ)),
                    ),
            ),
        )
    }

    @Test
    fun `variable defined and used in inner is not visible in outer`() {
        // given
        // f => ( g => (let a; a;) )
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val aVariable = Variable.PrimitiveVariable()
        val varAUse = variableUse("a")
        val funG = unitFunctionDefinition("g", block(aDeclaration, varAUse))
        val gVariable = Variable.FunctionVariable("g", mockk(), mockk())
        val funF = unitFunctionDefinition("f", funG)
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to mockk(), funF to fVariable, funG to gVariable, aDeclaration to aVariable),
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
                            setOf(fVariable),
                            emptySet(),
                        ),
                    funF.value to
                        StaticFunctionRelations(
                            program.value,
                            1,
                            setOf(gVariable),
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
        val fVariable = Variable.FunctionVariable("f", mockk(), mockk())
        val ast = astOf(aDeclaration, bDeclaration, fDeclaration)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(program to mockk(), fDeclaration to fVariable, aDeclaration to aVariable, bDeclaration to bVariable),
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
                            setOf(fVariable, aVariable, bVariable),
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
