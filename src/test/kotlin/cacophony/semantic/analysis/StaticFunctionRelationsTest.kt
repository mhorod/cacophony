package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.*
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.*
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
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
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
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable), mapOf(varAUse to aVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
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
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable), mapOf(varAUse to aVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
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
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse1 to aDeclaration, varAUse2 to aDeclaration)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(aDeclaration to aVariable),
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
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
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
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
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
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
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
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val bDeclaration = variableDeclaration("b", Empty(mockRange()))
        val cDeclaration = variableDeclaration("c", Empty(mockRange()))
        val dDeclaration = variableDeclaration("d", Empty(mockRange()))
        val eDeclaration = variableDeclaration("e", Empty(mockRange()))
        val fDeclaration = variableDeclaration("f", Empty(mockRange()))
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
                    sDeclaration to sVariable,
                    aDeclaration to aVariable,
                    bDeclaration to bVariable,
                    cDeclaration to cVariable,
                    dDeclaration to dVariable,
                    eDeclaration to eVariable,
                    fDeclaration to fVariable,
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
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
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
        val funF = unitFunctionDefinition("f", funG)
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(emptyMap())

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
                            1,
                            emptySet(),
                            emptySet(),
                        ),
                    funG to
                        StaticFunctionRelations(
                            funF,
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
        val funH = unitFunctionDefinition("h", Empty(mockRange()))
        val funF = unitFunctionDefinition("f", block(funG, funH))
        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(emptyMap())

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
                            1,
                            emptySet(),
                            emptySet(),
                        ),
                    funG to
                        StaticFunctionRelations(
                            funF,
                            2,
                            emptySet(),
                            emptySet(),
                        ),
                    funH to
                        StaticFunctionRelations(
                            funF,
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
        val ast = astOf(aDeclaration, funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(aVariable),
                            emptySet(),
                        ),
                    funF to
                        StaticFunctionRelations(
                            program,
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
        val funG = unitFunctionDefinition("g", block(bDeclaration, funH))
        val funF = unitFunctionDefinition("f", block(aDeclaration, funG))

        val ast = astOf(funF)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = emptyMap()
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
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
                program to programStaticRelation(),
                funF to
                    StaticFunctionRelations(
                        program,
                        1,
                        setOf(aVariable),
                        emptySet(),
                    ),
                funG to
                    StaticFunctionRelations(
                        funF,
                        2,
                        setOf(bVariable),
                        emptySet(),
                    ),
                funH to
                    StaticFunctionRelations(
                        funG,
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
        val funG = unitFunctionDefinition("g", funH)
        val funJ = unitFunctionDefinition("j", block())
        val varGUse = variableUse("g")
        val funI = unitFunctionDefinition("i", block(funJ, call(varGUse)))
        val funFoo = unitFunctionDefinition("foo", block(aDeclaration, funG, funI))
        val varFooUse = variableUse("foo")
        val funMain = unitFunctionDefinition("main", call(varFooUse))

        val ast = astOf(funFoo, funMain)
        val program = program(ast)
        val resolvedVariables: ResolvedVariables = mapOf(varAUse to aDeclaration, varGUse to funG, varFooUse to funFoo)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(aDeclaration to aVariable), mapOf(varAUse to aVariable))

        // when
        val relations = findStaticFunctionRelations(ast, resolvedVariables, variablesMap)

        // then
        assertThat(relations).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                program to programStaticRelation(),
                funFoo to
                    StaticFunctionRelations(
                        program,
                        1,
                        setOf(aVariable),
                        emptySet(),
                    ),
                funG to
                    StaticFunctionRelations(
                        funFoo,
                        2,
                        emptySet(),
                        emptySet(),
                    ),
                funH to
                    StaticFunctionRelations(
                        funG,
                        3,
                        setOf(),
                        setOf(UsedVariable(aVariable, VariableUseType.READ)),
                    ),
                funI to
                    StaticFunctionRelations(
                        funFoo,
                        2,
                        emptySet(),
                        emptySet(),
                    ),
                funJ to
                    StaticFunctionRelations(
                        funI,
                        3,
                        emptySet(),
                        emptySet(),
                    ),
                funMain to
                    StaticFunctionRelations(
                        program,
                        1,
                        emptySet(),
                        emptySet(),
                    ),
            ),
        )
    }
}
