package cacophony.semantic

import cacophony.semantic.syntaxtree.Empty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StaticFunctionRelationsTest {
    @Test
    fun `should find variable in function`() {
        // given
        // f => let a
        val varA = variableDeclaration("a", Empty(mockRange()))
        val funF = functionDeclaration("f", varA)
        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
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
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse = variableUse("a")
        val funF = functionDeclaration("f", block(varA, varAUse))
        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
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
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse = variableUse("a")
        val varAWrite = variableWrite(varAUse)
        val funF = functionDeclaration("f", block(varA, varAWrite))
        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
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
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse1 = variableUse("a")
        val varAWrite = variableWrite(varAUse1)

        val varAUse2 = variableUse("a")
        val funF = functionDeclaration("f", block(varA, varAWrite, varAUse2))
        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
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
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varB = variableDeclaration("b", Empty(mockRange()))
        val varC = variableDeclaration("c", Empty(mockRange()))
        val funF = functionDeclaration("f", block(varA, varB, varC))
        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
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
        val funG = functionDeclaration("g", Empty(mockRange()))
        val funF = functionDeclaration("f", funG)
        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
                            emptySet(),
                            emptySet(),
                        ),
                    funG to
                        StaticFunctionRelations(
                            funF,
                            1,
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
        val funG = functionDeclaration("g", Empty(mockRange()))
        val funH = functionDeclaration("h", Empty(mockRange()))
        val funF = functionDeclaration("f", block(funG, funH))
        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
                            emptySet(),
                            emptySet(),
                        ),
                    funG to
                        StaticFunctionRelations(
                            funF,
                            1,
                            emptySet(),
                            emptySet(),
                        ),
                    funH to
                        StaticFunctionRelations(
                            funF,
                            1,
                            emptySet(),
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should ignore top level variables`() {
        // given
        // (let a; f => ())
        val varA = variableDeclaration("a", Empty(mockRange()))
        val funF = functionDeclaration("f", Empty(mockRange()))
        val ast = astOf(varA, funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        StaticFunctionRelations(
                            null,
                            0,
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
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varB = variableDeclaration("b", Empty(mockRange()))
        val varC = variableDeclaration("C", Empty(mockRange()))
        val funH = functionDeclaration("h", block(varC))
        val funG = functionDeclaration("g", block(varB, funH))
        val funF = functionDeclaration("f", block(varA, funG))

        val ast = astOf(funF)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                funF to
                    StaticFunctionRelations(
                        null,
                        0,
                        setOf(varA),
                        emptySet(),
                    ),
                funG to
                    StaticFunctionRelations(
                        funF,
                        1,
                        setOf(varB),
                        emptySet(),
                    ),
                funH to
                    StaticFunctionRelations(
                        funG,
                        2,
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
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse = variableUse("a")
        val funH = functionDeclaration("h", varAUse)
        val funG = functionDeclaration("g", funH)
        val funJ = functionDeclaration("j", block())
        val varGUse = variableUse("g")
        val funI = functionDeclaration("i", block(funJ, call(varGUse)))
        val funFoo = functionDeclaration("foo", block(varA, funG, funI))
        val varFooUse = variableUse("foo")
        val funMain = functionDeclaration("main", call(varFooUse))

        val ast = astOf(funFoo, funMain)

        // when
        val relations = findStaticFunctionRelations(ast)

        // then
        assertThat(relations).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                funFoo to
                    StaticFunctionRelations(
                        null,
                        0,
                        setOf(varA),
                        emptySet(),
                    ),
                funG to
                    StaticFunctionRelations(
                        funFoo,
                        1,
                        emptySet(),
                        emptySet(),
                    ),
                funH to
                    StaticFunctionRelations(
                        funG,
                        2,
                        setOf(),
                        setOf(UsedVariable(varAUse, VariableUseType.READ)),
                    ),
                funI to
                    StaticFunctionRelations(
                        funFoo,
                        1,
                        emptySet(),
                        setOf(UsedVariable(varGUse, VariableUseType.READ)),
                    ),
                funJ to
                    StaticFunctionRelations(
                        funI,
                        2,
                        emptySet(),
                        emptySet(),
                    ),
                funMain to
                    StaticFunctionRelations(
                        null,
                        0,
                        emptySet(),
                        setOf(UsedVariable(varFooUse, VariableUseType.READ)),
                    ),
            ),
        )
    }
}
