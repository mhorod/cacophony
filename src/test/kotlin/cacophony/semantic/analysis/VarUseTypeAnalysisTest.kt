package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.semantic.*
import cacophony.semantic.syntaxtree.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VarUseTypeAnalysisTest {
    @Test
    fun `analysis of empty expression`() {
        val empty = Empty(mockRange())
        val ast = astOf(empty)
        val variablesMap: VariablesMap = createVariablesMap()
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(programResolvedName(ast)),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
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
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val ast = astOf(declaration)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(declaration to variable))
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(programResolvedName(ast)),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsEntry(
            declaration,
            emptyMap(),
        )
    }

    @Test
    fun `read only access`() {
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val varUse = variableUse("a")
        val ast = astOf(declaration, varUse)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(declaration to variable), mapOf(varUse to variable))
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                varUse to mapOf(variable to VariableUseType.READ),
                declaration to mapOf(),
            ),
        )
    }

    @Test
    fun `only information about variables visible in current scope`() {
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val varUse = variableUse("a")
        val ast = astOf(declaration, varUse)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(declaration to variable), mapOf(varUse to variable))
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                ast to mapOf(),
            ),
        )
    }

    @Test
    fun `information about variables usage in nested blocks`() {
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val varUse = variableUse("a")
        val block = block(varUse)
        val ast = astOf(declaration, block)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(declaration to variable), mapOf(varUse to variable))
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                block to mapOf(variable to VariableUseType.READ),
            ),
        )
    }

    @Test
    fun `should find struct variables`() {
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
        val sDeclaration = dummyDeclaration("s")
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

        val assignment =
            OperatorBinary.Assignment(
                mockRange(),
                saLValue,
                sbdLValue,
            )
        val ast = astOf(sDeclaration, assignment)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
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

        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varSUse1 to sDeclaration,
                    varSUse2 to sDeclaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result)
            .containsAllEntriesOf(
                mapOf(
                    assignment to
                        mapOf(
                            sVariable to VariableUseType.READ_WRITE,
                            bVariable to VariableUseType.READ,
                            dVariable to VariableUseType.READ,
                            fVariable to VariableUseType.READ,
                            aVariable to VariableUseType.WRITE,
                            cVariable to VariableUseType.WRITE,
                        ),
                ),
            )
    }

    @Test
    fun `should find usages in struct literals`() {
        //       s
        //      / \
        //     a   b
        //
        // f => ( let x, let s, s = {x, 2})
        val xDeclaration = dummyDeclaration("x")
        val sDeclaration = dummyDeclaration("s")
        val xVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.PrimitiveVariable()
        val aVariable = Variable.PrimitiveVariable()
        val sVariable = Variable.StructVariable(mapOf("a" to aVariable, "b" to bVariable))

        val varSUse = variableUse("s")
        val varXUse = variableUse("x")

        val structLiteral =
            Struct(
                mockRange(),
                mapOf(
                    structField("a") to varXUse,
                    structField("b") to Literal.IntLiteral(mockRange(), 2),
                ),
            )

        val assignment =
            OperatorBinary.Assignment(
                mockRange(),
                varSUse,
                structLiteral,
            )
        val ast = astOf(xDeclaration, sDeclaration, assignment)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(
                    xDeclaration to xVariable,
                    sDeclaration to sVariable,
                ),
                mapOf(
                    varSUse to sVariable,
                    varXUse to xVariable,
                ),
            )

        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varSUse to sDeclaration,
                    varXUse to xDeclaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result)
            .containsAllEntriesOf(
                mapOf(
                    assignment to
                        mapOf(
                            sVariable to VariableUseType.WRITE,
                            aVariable to VariableUseType.WRITE,
                            bVariable to VariableUseType.WRITE,
                            xVariable to VariableUseType.READ,
                        ),
                ),
            )
    }

    @Test
    fun `write usage`() {
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val varUse = variableUse("a")
        val write = variableWrite(varUse)
        val block = block(write)
        val ast = astOf(declaration, block)
        val variablesMap: VariablesMap = createVariablesMap(mapOf(declaration to variable), mapOf(varUse to variable))
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                write to mapOf(variable to VariableUseType.WRITE),
                block to mapOf(variable to VariableUseType.WRITE),
            ),
        )
    }

    @Test
    fun `read-write usage`() {
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val varUse1 = variableUse("a")
        val varUse2 = variableUse("a")
        val write = variableWrite(varUse1)
        val block = block(write, varUse2)
        val ast = astOf(declaration, block)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(declaration to variable),
                mapOf(varUse1 to variable, varUse2 to variable),
            )
        val result =
            analyzeVarUseTypes(
                ast,
                mapOf(
                    programResolvedName(ast),
                    varUse1 to declaration,
                    varUse2 to declaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                write to mapOf(variable to VariableUseType.WRITE),
                varUse2 to mapOf(variable to VariableUseType.READ),
                block to mapOf(variable to VariableUseType.READ_WRITE),
            ),
        )
    }

    @Test
    fun `more variables and blocks`() {
        val xDeclaration = dummyDeclaration("x")
        val yDeclaration = dummyDeclaration("y")
        val zDeclaration = dummyDeclaration("z")
        val xVariable = Variable.PrimitiveVariable()
        val yVariable = Variable.PrimitiveVariable()
        val zVariable = Variable.PrimitiveVariable()
        val xUse1 = variableUse("x")
        val xUse2 = variableUse("x")
        val yUse = variableUse("y")
        val zUse = variableUse("z")
        val xWrite = variableWrite(xUse1)
        val zWrite = variableWrite(zUse)
        val nestedBlock = block(xUse2, yUse, zWrite)
        val block = block(zDeclaration, xWrite, nestedBlock)
        val ast = astOf(xDeclaration, yDeclaration, block)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(xDeclaration to xVariable, yDeclaration to yVariable, zDeclaration to zVariable),
                mapOf(xUse1 to xVariable, xUse2 to xVariable, yUse to yVariable, zUse to zVariable),
            )
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
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                nestedBlock to
                    mapOf(
                        xVariable to VariableUseType.READ,
                        yVariable to VariableUseType.READ,
                        zVariable to VariableUseType.WRITE,
                    ),
                block to
                    mapOf(
                        xVariable to VariableUseType.READ_WRITE,
                        yVariable to VariableUseType.READ,
                    ),
            ),
        )
    }

    @Test
    fun `function declaration is not usage`() {
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val varUse1 = variableUse("a")
        val varUse2 = variableUse("a")
        val write = variableWrite(varUse1)
        val readWriteBlock = block(write, varUse2)
        val argument = arg("x")
        val fDeclaration = unitFunctionDefinition("f", listOf(argument), readWriteBlock)
        val ast = astOf(declaration, fDeclaration)
        val program = program(ast)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(declaration to variable),
                mapOf(varUse1 to variable, varUse2 to variable),
            )
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
                                AnalyzedVariable(variable, program, VariableUseType.READ_WRITE),
                            ),
                        ),
                ),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                fDeclaration to mapOf(),
            ),
        )
    }

    @Test
    fun `function call is read-only usage on arguments`() {
        val declaration = dummyDeclaration("a")
        val aVariable = Variable.PrimitiveVariable()
        val argument = arg("x")
        val xVariable = Variable.PrimitiveVariable()
        val fDeclaration = unitFunctionDefinition("f", listOf(argument), block())
        val fUse = variableUse("f")
        val aUse = variableUse("a")
        val call = call(fUse, aUse)
        val ast = astOf(declaration, fDeclaration, call)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(declaration to aVariable, argument to xVariable),
                mapOf(aUse to aVariable),
            )
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
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                call to
                    mapOf(
                        aVariable to VariableUseType.READ,
                    ),
            ),
        )
    }

    @Test
    fun `nested function using outer variables`() {
        val declaration = dummyDeclaration("a")
        val variable = Variable.PrimitiveVariable()
        val varUse1 = variableUse("a")
        val varUse2 = variableUse("a")
        val write = variableWrite(varUse1)
        val readWriteBlock = block(write, varUse2)
        val fDeclaration = unitFunctionDefinition("f", readWriteBlock)
        val fUse = variableUse("f")
        val call = call(fUse)
        val ast = astOf(declaration, fDeclaration, call)
        val program = program(ast)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(declaration to variable),
                mapOf(varUse1 to variable, varUse2 to variable),
            )
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
                                AnalyzedVariable(variable, program, VariableUseType.READ_WRITE),
                            ),
                        ),
                ),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                call to
                    mapOf(
                        variable to VariableUseType.READ_WRITE,
                    ),
            ),
        )
    }

    @Test
    fun `nested function call has information about variables in current scope`() {
        val aDeclaration = dummyDeclaration("a")
        val bDeclaration = dummyDeclaration("b")
        val aVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.PrimitiveVariable()
        val aUse = variableUse("a")
        val bUse = variableUse("b")
        val gDeclaration = unitFunctionDefinition("g", block(aUse, bUse))
        val gUse = variableUse("g")
        val gCall = call(gUse)
        val fDeclaration =
            unitFunctionDefinition("f", block(bDeclaration, gDeclaration, gCall))
        val fUse = variableUse("f")
        val fCall = call(fUse)
        val ast = astOf(aDeclaration, fDeclaration, fCall)
        val program = program(ast)
        val variablesMap: VariablesMap =
            createVariablesMap(
                mapOf(aDeclaration to aVariable, bDeclaration to bVariable),
                mapOf(aUse to aVariable, bUse to bVariable),
            )
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
                                AnalyzedVariable(aVariable, program, VariableUseType.READ),
                            ),
                        ),
                    gDeclaration to
                        analyzedFunction(
                            gDeclaration,
                            2,
                            setOf(
                                AnalyzedVariable(aVariable, program, VariableUseType.READ),
                                AnalyzedVariable(bVariable, fDeclaration, VariableUseType.READ),
                            ),
                        ),
                ),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                gCall to
                    mapOf(
                        aVariable to VariableUseType.READ,
                        bVariable to VariableUseType.READ,
                    ),
                fCall to
                    mapOf(
                        aVariable to VariableUseType.READ,
                    ),
            ),
        )
    }

    @Test
    fun `should find uses and heap write in allocation`() {
        val aDeclaration = dummyDeclaration("a")
        val bDeclaration = dummyDeclaration("b")
        val cDeclaration = dummyDeclaration("c")
        val aVariable = Variable.PrimitiveVariable()
        val bVariable = Variable.PrimitiveVariable()
        val cVariable = Variable.PrimitiveVariable()
        val pVariable = Variable.PrimitiveVariable()
        val aUse = variableUse("a")
        val bUse = variableUse("b")
        val cUse = variableUse("c")

        // let p = $(a + (b = 1) * (c -= 2))
        val allocation =
            alloc(
                block(aUse add (block(bUse assign lit(1)) mul block(cUse subeq lit(2)))),
            )
        val pDeclaration = variableDeclaration("p", allocation)
        val ast = astOf(aDeclaration, bDeclaration, cDeclaration, pDeclaration)
        val program = program(ast)
        val variablesMap =
            createVariablesMap(
                mapOf(aDeclaration to aVariable, bDeclaration to bVariable, cDeclaration to cVariable, pDeclaration to pVariable),
                mapOf(aUse to aVariable, bUse to bVariable, cUse to cVariable),
            )
        val result =
            analyzeVarUseTypes(
                program,
                mapOf(
                    programResolvedName(ast),
                    aUse to aDeclaration,
                    bUse to bDeclaration,
                    cUse to cDeclaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                allocation to
                    mapOf(
                        aVariable to VariableUseType.READ,
                        bVariable to VariableUseType.WRITE,
                        cVariable to VariableUseType.READ_WRITE,
                        Variable.Heap to VariableUseType.WRITE,
                    ),
            ),
        )
    }

    @Test
    fun `should find uses and heap read in dereference used as rvalue`() {
        val pDeclaration = dummyDeclaration("p")
        val qDeclaration = dummyDeclaration("q")
        val pVariable = Variable.PrimitiveVariable()
        val qVariable = Variable.PrimitiveVariable()
        val xVariable = Variable.PrimitiveVariable()
        val pUse1 = variableUse("p")
        val pUse2 = variableUse("p")
        val qUse1 = variableUse("q")
        val qUse2 = variableUse("q")

        // let x = @(if true then p else if false then q = p else q)
        val dereference = deref(block(ifThenElse(lit(true), pUse1, ifThenElse(lit(false), qUse1 assign pUse2, qUse2))))
        val xDeclaration = variableDeclaration("x", dereference)
        val ast = astOf(pDeclaration, qDeclaration, xDeclaration)
        val program = program(ast)
        val variablesMap =
            createVariablesMap(
                mapOf(pDeclaration to pVariable, qDeclaration to qVariable, xDeclaration to xVariable),
                mapOf(pUse1 to pVariable, pUse2 to pVariable, qUse1 to qVariable, qUse2 to qVariable),
            )
        val result =
            analyzeVarUseTypes(
                program,
                mapOf(
                    programResolvedName(ast),
                    pUse1 to pDeclaration,
                    pUse2 to pDeclaration,
                    qUse1 to qDeclaration,
                    qUse2 to qDeclaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                dereference to
                    mapOf(
                        pVariable to VariableUseType.READ,
                        qVariable to VariableUseType.READ_WRITE,
                        Variable.Heap to VariableUseType.READ,
                    ),
            ),
        )
    }

    @Test
    fun `should find uses and heap read in dereference used as lvalue in assignment`() {
        val pDeclaration = dummyDeclaration("p")
        val qDeclaration = dummyDeclaration("q")
        val pVariable = Variable.PrimitiveVariable()
        val qVariable = Variable.PrimitiveVariable()
        val pUse1 = variableUse("p")
        val pUse2 = variableUse("p")
        val qUse1 = variableUse("q")
        val qUse2 = variableUse("q")

        // @(if true then p else if false then p = q else q) = 5
        val dereference = deref(block(ifThenElse(lit(true), pUse1, ifThenElse(lit(false), pUse2 assign qUse1, qUse2))))
        val assignment = dereference assign lit(5)
        val ast = astOf(pDeclaration, qDeclaration, assignment)
        val program = program(ast)
        val variablesMap =
            createVariablesMap(
                mapOf(pDeclaration to pVariable, qDeclaration to qVariable),
                mapOf(pUse1 to pVariable, pUse2 to pVariable, qUse1 to qVariable, qUse2 to qVariable),
            )
        val result =
            analyzeVarUseTypes(
                program,
                mapOf(
                    programResolvedName(ast),
                    pUse1 to pDeclaration,
                    pUse2 to pDeclaration,
                    qUse1 to qDeclaration,
                    qUse2 to qDeclaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                assignment to
                    mapOf(
                        pVariable to VariableUseType.READ_WRITE,
                        qVariable to VariableUseType.READ,
                        Variable.Heap to VariableUseType.WRITE,
                    ),
            ),
        )
    }

    private fun dummyDeclaration(identifier: String): Definition.VariableDeclaration = variableDeclaration(identifier, empty())
}
