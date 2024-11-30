package cacophony.semantic.analysis

import cacophony.semantic.program
import cacophony.semantic.programStaticRelation
import cacophony.semantic.syntaxtree.Empty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StaticFunctionRelationsTest {
    @Test
    fun `should find variable in function`() {
        // given
        // f => let a
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val funF = cacophony.semantic.functionDeclaration("f", varA)
        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
                            1,
                            setOf(varA),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find variable read`() {
        // given
        // f => let a
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val varAUse = cacophony.semantic.variableUse("a")
        val funF = cacophony.semantic.functionDeclaration("f", cacophony.semantic.block(varA, varAUse))
        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
                            1,
                            setOf(varA),
                            setOf(UsedVariable(varAUse, VariableUseType.READ)),
                        ),
                ),
            )
    }

    @Test
    fun `should find variable write`() {
        // given
        // f => (let a; a = ())
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val varAUse = cacophony.semantic.variableUse("a")
        val varAWrite = cacophony.semantic.variableWrite(varAUse)
        val funF = cacophony.semantic.functionDeclaration("f", cacophony.semantic.block(varA, varAWrite))
        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
                            1,
                            setOf(varA),
                            setOf(UsedVariable(varAUse, VariableUseType.WRITE)),
                        ),
                ),
            )
    }

    @Test
    fun `should find variable read and write`() {
        // given
        // f => (let a; a = (); a)
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val varAUse1 = cacophony.semantic.variableUse("a")
        val varAWrite = cacophony.semantic.variableWrite(varAUse1)

        val varAUse2 = cacophony.semantic.variableUse("a")
        val funF = cacophony.semantic.functionDeclaration("f", cacophony.semantic.block(varA, varAWrite, varAUse2))
        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
                            1,
                            setOf(varA),
                            setOf(
                                UsedVariable(varAUse1, VariableUseType.WRITE),
                                UsedVariable(varAUse2, VariableUseType.READ),
                            ),
                        ),
                ),
            )
    }

    @Test
    fun `should find multiple variables in block`() {
        // given
        // f => ( let a; let b; let c )
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val varB = cacophony.semantic.variableDeclaration("b", Empty(cacophony.semantic.mockRange()))
        val varC = cacophony.semantic.variableDeclaration("c", Empty(cacophony.semantic.mockRange()))
        val funF = cacophony.semantic.functionDeclaration("f", cacophony.semantic.block(varA, varB, varC))
        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to programStaticRelation(),
                    funF to
                        StaticFunctionRelations(
                            program,
                            1,
                            setOf(varA, varB, varC),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find nested function`() {
        // given
        // f => ( g => () )
        val funG = cacophony.semantic.functionDeclaration("g", Empty(cacophony.semantic.mockRange()))
        val funF = cacophony.semantic.functionDeclaration("f", funG)
        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

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
        val funG = cacophony.semantic.functionDeclaration("g", Empty(cacophony.semantic.mockRange()))
        val funH = cacophony.semantic.functionDeclaration("h", Empty(cacophony.semantic.mockRange()))
        val funF = cacophony.semantic.functionDeclaration("f", cacophony.semantic.block(funG, funH))
        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

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
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val funF = cacophony.semantic.functionDeclaration("f", Empty(cacophony.semantic.mockRange()))
        val ast = cacophony.semantic.astOf(varA, funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    program to
                        StaticFunctionRelations(
                            null,
                            0,
                            setOf(varA),
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
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val varB = cacophony.semantic.variableDeclaration("b", Empty(cacophony.semantic.mockRange()))
        val varC = cacophony.semantic.variableDeclaration("C", Empty(cacophony.semantic.mockRange()))
        val funH = cacophony.semantic.functionDeclaration("h", cacophony.semantic.block(varC))
        val funG = cacophony.semantic.functionDeclaration("g", cacophony.semantic.block(varB, funH))
        val funF = cacophony.semantic.functionDeclaration("f", cacophony.semantic.block(varA, funG))

        val ast = cacophony.semantic.astOf(funF)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                program to programStaticRelation(),
                funF to
                    StaticFunctionRelations(
                        program,
                        1,
                        setOf(varA),
                        emptySet(),
                    ),
                funG to
                    StaticFunctionRelations(
                        funF,
                        2,
                        setOf(varB),
                        emptySet(),
                    ),
                funH to
                    StaticFunctionRelations(
                        funG,
                        3,
                        setOf(varC),
                        emptySet(),
                    ),
            ),
        )
    }

    @Test
    fun `should find relations in complex nested functions`() {
        // given
        // (foo => (let a; g => h => a; i => (j => (); g())); main => foo())
        val varA = cacophony.semantic.variableDeclaration("a", Empty(cacophony.semantic.mockRange()))
        val varAUse = cacophony.semantic.variableUse("a")
        val funH = cacophony.semantic.functionDeclaration("h", varAUse)
        val funG = cacophony.semantic.functionDeclaration("g", funH)
        val funJ = cacophony.semantic.functionDeclaration("j", cacophony.semantic.block())
        val varGUse = cacophony.semantic.variableUse("g")
        val funI = cacophony.semantic.functionDeclaration("i", cacophony.semantic.block(funJ, cacophony.semantic.call(varGUse)))
        val funFoo = cacophony.semantic.functionDeclaration("foo", cacophony.semantic.block(varA, funG, funI))
        val varFooUse = cacophony.semantic.variableUse("foo")
        val funMain = cacophony.semantic.functionDeclaration("main", cacophony.semantic.call(varFooUse))

        val ast = cacophony.semantic.astOf(funFoo, funMain)
        val program = program(ast)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                program to programStaticRelation(),
                funFoo to
                    StaticFunctionRelations(
                        program,
                        1,
                        setOf(varA),
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
                        setOf(UsedVariable(varAUse, VariableUseType.READ)),
                    ),
                funI to
                    StaticFunctionRelations(
                        funFoo,
                        2,
                        emptySet(),
                        setOf(UsedVariable(varGUse, VariableUseType.READ)),
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
                        setOf(UsedVariable(varFooUse, VariableUseType.READ)),
                    ),
            ),
        )
    }
}
