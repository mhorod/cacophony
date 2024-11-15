package cacophony.semantic

import cacophony.*
import cacophony.semantic.syntaxtree.Empty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FunctionAnalysisTest {
    @Test
    fun `should analyze empty function`() {
        // given
        // f => {}
        val funF = functionDeclaration("f", block())
        val ast = astOf(funF)

        // when
        val results = analyzeFunctions(ast, emptyMap(), callGraph())

        // then
        assertThat(results)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        AnalyzedFunction(
                            null,
                            emptySet(),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                ),
            )
    }

    // @Test
    fun `should analyze function with unused variable`() {
        // given
        // f => let a
        val varA = variableDeclaration("a", Empty(mockRange()))
        val funF = functionDeclaration("f", block(varA))
        val ast = astOf(funF)

        // when
        val results = analyzeFunctions(ast, emptyMap(), emptyMap())

        // then
        assertThat(results)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        AnalyzedFunction(
                            null,
                            setOf(AnalyzedVariable(varA, funF, VariableUseType.UNUSED)),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                ),
            )
    }

    // @Test
    fun `should analyze function with read variable`() {
        // given
        // f => (let a; a)
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse = variableUse("a")
        val funF = functionDeclaration("f", block(varA, varAUse))
        val ast = astOf(funF)

        // when
        val result = analyzeFunctions(ast, mapOf(varAUse to varA), callGraph())

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        AnalyzedFunction(
                            null,
                            setOf(AnalyzedVariable(varA, funF, VariableUseType.READ)),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                ),
            )
    }

    // @Test
    fun `should analyze function with written variable`() {
        // given
        // f => (let a; a = ())
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse = variableUse("a")
        val varAWrite = variableWrite(varAUse)
        val funF = functionDeclaration("f", block(varA, varAWrite))
        val ast = astOf(funF)

        // when
        val result = analyzeFunctions(ast, mapOf(varAUse to varA), callGraph())

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        AnalyzedFunction(
                            null,
                            setOf(AnalyzedVariable(varA, funF, VariableUseType.WRITE)),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                ),
            )
    }

    // @Test
    fun `should analyze function with read and written variable`() {
        // given
        // f => (let a; a = (); a)
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse1 = variableUse("a")
        val varAWrite = variableWrite(varAUse1)
        val varAUse2 = variableUse("a")
        val funF = functionDeclaration("f", block(varA, varAWrite, varAUse2))
        val ast = astOf(funF)

        // when
        val result = analyzeFunctions(ast, mapOf(varAUse1 to varA, varAUse2 to varA), callGraph())

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        AnalyzedFunction(
                            null,
                            setOf(AnalyzedVariable(varA, funF, VariableUseType.READ_WRITE)),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with nested function`() {
        // given
        // f => (g => (); g())
        val funG = functionDeclaration("g", block())
        val varGUse = variableUse("g")
        val funF = functionDeclaration("f", block(funG, call(varGUse)))
        val ast = astOf(funF)
        val callGraph = callGraph(funF to funG)

        // when
        val result = analyzeFunctions(ast, emptyMap(), callGraph)

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        AnalyzedFunction(
                            null,
                            emptySet(),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                    funG to
                        AnalyzedFunction(
                            ParentLink(funF, false),
                            emptySet(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should analyze function with nested function using parent variable`() {
        // given
        // f => (let a; g => a)
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse = variableUse("a")
        val funG = functionDeclaration("g", varAUse)
        val funF = functionDeclaration("f", block(varA, funG))
        val ast = astOf(funF)
        val callGraph = callGraph()

        // when
        val result = analyzeFunctions(ast, mapOf(varAUse to varA), callGraph)

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funF to
                        AnalyzedFunction(
                            null,
                            setOf(),
                            mutableSetOf(),
                            0,
                            setOf(varA),
                        ),
                    funG to
                        AnalyzedFunction(
                            ParentLink(funF, true),
                            setOf(AnalyzedVariable(varA, funF, VariableUseType.READ)),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    // @Test
    fun `should find transitive parent link usages`() {
        // given
        // (foo => (let a; g => h => a; i => (j => (); g())); main => foo())
        val varA = variableDeclaration("a", Empty(mockRange()))
        val varAUse = variableUse("a")
        val funH = functionDeclaration("h", varAUse)
        val funG = functionDeclaration("g", funH)
        val funJ = functionDeclaration("j", block())
        val funI = functionDeclaration("i", block(funJ, call(variableUse("g"))))
        val funFoo = functionDeclaration("foo", block(varA, funG, funI))
        val funMain = functionDeclaration("main", call(variableUse("foo")))

        val ast = astOf(funFoo, funMain)

        // when
        val result =
            analyzeFunctions(
                ast,
                mapOf(varAUse to varA),
                callGraph(funI to funG, funMain to funFoo),
            )

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funFoo to
                        AnalyzedFunction(
                            null,
                            setOf(),
                            mutableSetOf(),
                            0,
                            setOf(varA),
                        ),
                    funG to
                        AnalyzedFunction(
                            ParentLink(funFoo, true),
                            emptySet(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                    funH to
                        AnalyzedFunction(
                            ParentLink(funG, true),
                            setOf(AnalyzedVariable(varA, funFoo, VariableUseType.READ)),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                    funJ to
                        AnalyzedFunction(
                            ParentLink(funI, false),
                            emptySet(),
                            mutableSetOf(),
                            2,
                            emptySet(),
                        ),
                    funI to
                        AnalyzedFunction(
                            ParentLink(funFoo, true),
                            emptySet(),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                    funMain to
                        AnalyzedFunction(
                            null,
                            emptySet(),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                ),
            )
    }

    // @Test
    fun `should return separate results for distinct variables with equal names`() {
        // given
        // foo => (let a; bar => (let a; a = ()); a)
        val fooVarA = variableDeclaration("a", Empty(mockRange()))
        val fooVarAUse = variableUse("a")
        val barVarA = variableDeclaration("a", Empty(mockRange()))
        val barVarAUse = variableUse("a")

        val funBar = functionDeclaration("bar", block(barVarA, variableWrite(barVarAUse)))
        val funFoo = functionDeclaration("foo", block(fooVarA, funBar, fooVarAUse))
        val ast = astOf(funFoo)

        // when
        val result = analyzeFunctions(ast, mapOf(fooVarAUse to fooVarA, barVarAUse to barVarA), callGraph())

        // then
        assertThat(result)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    funFoo to
                        AnalyzedFunction(
                            null,
                            setOf(
                                // AnalyzedVariable(fooVarA, funFoo, VariableUseType.READ)
                            ),
                            mutableSetOf(),
                            0,
                            emptySet(),
                        ),
                    funBar to
                        AnalyzedFunction(
                            ParentLink(funFoo, false),
                            setOf(
                                // AnalyzedVariable(barVarA, funBar, VariableUseType.WRITE),
                            ),
                            mutableSetOf(),
                            1,
                            emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun `should find uses of parent function argument`() {
        // given
        // f[a, b] => (g => (a; b = ()))
        val argA = arg("a")
        val argB = arg("b")
        val varAUse = variableUse("a")
        val varBUse = variableUse("b")
        val funG = functionDeclaration("g", block(varAUse, variableWrite(varBUse)))
        val funF = functionDeclaration("f", listOf(argA, argB), funG)

        val ast = astOf(funF)

        // when
        val result =
            analyzeFunctions(
                ast,
                mapOf(varAUse to argA, varBUse to argB),
                callGraph(),
            )

        // then
        assertThat(result).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                funF to
                    AnalyzedFunction(
                        null,
                        setOf(),
                        mutableSetOf(),
                        0,
                        setOf(argA, argB),
                    ),
                funG to
                    AnalyzedFunction(
                        ParentLink(funF, true),
                        setOf(
                            AnalyzedVariable(argA, funF, VariableUseType.READ),
                            AnalyzedVariable(argB, funF, VariableUseType.WRITE),
                        ),
                        mutableSetOf(),
                        1,
                        emptySet(),
                    ),
            ),
        )
    }
}
