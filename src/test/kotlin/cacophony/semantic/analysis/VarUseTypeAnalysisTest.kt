package cacophony.semantic.analysis

import cacophony.semantic.*
import cacophony.semantic.syntaxtree.Empty
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VarUseTypeAnalysisTest {
    @Test
    fun `analysis of empty expression`() {
        val empty = Empty(mockRange())
        val ast = astOf(empty)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(programResolvedName(ast)),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                ast to emptyMap(),
                empty to emptyMap(),
            ),
        )
    }

    @Test
    fun `declaration is not usage`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val ast = astOf(declaration)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(programResolvedName(ast)),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsEntry(
            declaration,
            emptyMap(),
        )
    }

    @Test
    fun `read only access`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val varUse = variableUse("a")
        val ast = astOf(declaration, varUse)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                varUse to mapOf(declaration to VariableUseType.READ),
                declaration to mapOf(),
            ),
        )
    }

    @Test
    fun `only information about variables visible in current scope`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val varUse = variableUse("a")
        val ast = astOf(declaration, varUse)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                ast to mapOf(),
            ),
        )
    }

    @Test
    fun `information about variables usage in nested blocks`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val varUse = variableUse("a")
        val block = block(varUse)
        val ast = astOf(declaration, block)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                block to mapOf(declaration to VariableUseType.READ),
            ),
        )
    }

    @Test
    fun `write usage`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val varUse = variableUse("a")
        val write = variableWrite(varUse)
        val block = block(write)
        val ast = astOf(declaration, block)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                write to mapOf(declaration to VariableUseType.WRITE),
                block to mapOf(declaration to VariableUseType.WRITE),
            ),
        )
    }

    @Test
    fun `read-write usage`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val varUse1 = variableUse("a")
        val varUse2 = variableUse("a")
        val write = variableWrite(varUse1)
        val block = block(write, varUse2)
        val ast = astOf(declaration, block)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse1 to declaration,
                    varUse2 to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                write to mapOf(declaration to VariableUseType.WRITE),
                varUse2 to mapOf(declaration to VariableUseType.READ),
                block to mapOf(declaration to VariableUseType.READ_WRITE),
            ),
        )
    }

    @Test
    fun `more variables and blocks`() {
        val xDeclaration = variableDeclaration("x", Empty(mockRange()))
        val yDeclaration = variableDeclaration("y", Empty(mockRange()))
        val zDeclaration = variableDeclaration("z", Empty(mockRange()))
        val xUse1 = variableUse("x")
        val xUse2 = variableUse("x")
        val yUse = variableUse("y")
        val zUse = variableUse("z")
        val xWrite = variableWrite(xUse1)
        val zWrite = variableWrite(zUse)
        val nestedBlock = block(xUse2, yUse, zWrite)
        val block = block(zDeclaration, xWrite, nestedBlock)
        val ast = astOf(xDeclaration, yDeclaration, block)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    xUse1 to xDeclaration,
                    xUse2 to xDeclaration,
                    yUse to yDeclaration,
                    zUse to zDeclaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                nestedBlock to
                    mapOf(
                        xDeclaration to VariableUseType.READ,
                        yDeclaration to VariableUseType.READ,
                        zDeclaration to VariableUseType.WRITE,
                    ),
                block to
                    mapOf(
                        xDeclaration to VariableUseType.READ_WRITE,
                        yDeclaration to VariableUseType.READ,
                    ),
            ),
        )
    }

    @Test
    fun `function declaration is not usage`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val varUse1 = variableUse("a")
        val varUse2 = variableUse("a")
        val write = variableWrite(varUse1)
        val readWriteBlock = block(write, varUse2)
        val argument = arg("x")
        val fDeclaration = functionDeclaration("f", listOf(argument), readWriteBlock)
        val ast = astOf(declaration, fDeclaration)
        val program = program(ast)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse1 to declaration,
                    varUse2 to declaration,
                ),
                mapOf(
                    programFunctionAnalysis(ast),
                    fDeclaration to
                        analyzedFunction(
                            fDeclaration,
                            1,
                            setOf(
                                AnalyzedVariable(declaration, program, VariableUseType.READ_WRITE),
                            ),
                        ),
                ),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                fDeclaration to mapOf(),
            ),
        )
    }

    @Test
    fun `function call is read-only usage on arguments`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val argument = arg("x")
        val fDeclaration = functionDeclaration("f", listOf(argument), block())
        val fUse = variableUse("f")
        val aUse = variableUse("a")
        val call = call(fUse, aUse)
        val ast = astOf(declaration, fDeclaration, call)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    aUse to declaration,
                    fUse to fDeclaration,
                ),
                mapOf(
                    programFunctionAnalysis(ast),
                    fDeclaration to
                        analyzedFunction(
                            fDeclaration,
                            1,
                            setOf(),
                        ),
                ),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                call to
                    mapOf(
                        declaration to VariableUseType.READ,
                        fDeclaration to VariableUseType.READ,
                    ),
            ),
        )
    }

    @Test
    fun `nested function using outer variables`() {
        val declaration = variableDeclaration("a", Empty(mockRange()))
        val varUse1 = variableUse("a")
        val varUse2 = variableUse("a")
        val write = variableWrite(varUse1)
        val readWriteBlock = block(write, varUse2)
        val fDeclaration = functionDeclaration("f", readWriteBlock)
        val fUse = variableUse("f")
        val call = call(fUse)
        val ast = astOf(declaration, fDeclaration, call)
        val program = program(ast)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse1 to declaration,
                    varUse2 to declaration,
                    fUse to fDeclaration,
                ),
                mapOf(
                    programFunctionAnalysis(ast),
                    fDeclaration to
                        analyzedFunction(
                            fDeclaration,
                            1,
                            setOf(
                                AnalyzedVariable(declaration, program, VariableUseType.READ_WRITE),
                            ),
                        ),
                ),
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                call to
                    mapOf(
                        fDeclaration to VariableUseType.READ,
                        declaration to VariableUseType.READ_WRITE,
                    ),
            ),
        )
    }

    @Test
    fun `nested function call has information about variables in current scope`() {
        val aDeclaration = variableDeclaration("a", Empty(mockRange()))
        val bDeclaration = variableDeclaration("b", Empty(mockRange()))
        val aUse = variableUse("a")
        val bUse = variableUse("b")
        val gDeclaration = functionDeclaration("g", block(aUse, bUse))
        val gUse = variableUse("g")
        val gCall = call(gUse)
        val fDeclaration =
            functionDeclaration("f", block(bDeclaration, gDeclaration, gCall))
        val fUse = variableUse("f")
        val fCall = call(fUse)
        val ast = astOf(aDeclaration, fDeclaration, fCall)
        val program = program(ast)
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    aUse to aDeclaration,
                    bUse to bDeclaration,
                    fUse to fDeclaration,
                    gUse to gDeclaration,
                ),
                mapOf(
                    programFunctionAnalysis(ast),
                    fDeclaration to
                        analyzedFunction(
                            fDeclaration,
                            1,
                            setOf(
                                AnalyzedVariable(aDeclaration, program, VariableUseType.READ),
                            ),
                        ),
                    gDeclaration to
                        analyzedFunction(
                            gDeclaration,
                            2,
                            setOf(
                                AnalyzedVariable(aDeclaration, program, VariableUseType.READ),
                                AnalyzedVariable(bDeclaration, fDeclaration, VariableUseType.READ),
                            ),
                        ),
                ),
            )
        println(result[fDeclaration.body])
        assertThat(result).containsAllEntriesOf(
            mapOf(
                gCall to
                    mapOf(
                        gDeclaration to VariableUseType.READ,
                        aDeclaration to VariableUseType.READ,
                        bDeclaration to VariableUseType.READ,
                    ),
                fCall to
                    mapOf(
                        fDeclaration to VariableUseType.READ,
                        aDeclaration to VariableUseType.READ,
                    ),
            ),
        )
    }
}
