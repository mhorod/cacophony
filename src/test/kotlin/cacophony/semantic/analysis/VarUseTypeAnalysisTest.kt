package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import cacophony.controlflow.generation.MakeBinaryExpression
import cacophony.controlflow.generation.TestOperators
import cacophony.semantic.*
import cacophony.semantic.syntaxtree.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

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
        val declaration = variableDeclaration("a")
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
        val declaration = variableDeclaration("a")
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
        val declaration = variableDeclaration("a")
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
        val declaration = variableDeclaration("a")
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
        val sDeclaration = variableDeclaration("s")
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
        val xDeclaration = variableDeclaration("x")
        val sDeclaration = variableDeclaration("s")
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
        val declaration = variableDeclaration("a")
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
        val declaration = variableDeclaration("a")
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
        val xDeclaration = variableDeclaration("x")
        val yDeclaration = variableDeclaration("y")
        val zDeclaration = variableDeclaration("z")
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
        val declaration = variableDeclaration("a")
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
        val declaration = variableDeclaration("a")
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
        val declaration = variableDeclaration("a")
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
        val aDeclaration = variableDeclaration("a")
        val bDeclaration = variableDeclaration("b")
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
        val aDeclaration = variableDeclaration("a")
        val bDeclaration = variableDeclaration("b")
        val cDeclaration = variableDeclaration("c")
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
        val pDeclaration = variableDeclaration("p")
        val qDeclaration = variableDeclaration("q")
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
                mapOf(pUse1 to pVariable, pUse2 to pVariable, qUse1 to qVariable, qUse2 to qVariable, dereference to Variable.Heap),
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
        val pDeclaration = variableDeclaration("p")
        val qDeclaration = variableDeclaration("q")
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
                mapOf(pUse1 to pVariable, pUse2 to pVariable, qUse1 to qVariable, qUse2 to qVariable, dereference to Variable.Heap),
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

    @ParameterizedTest
    @MethodSource("arithmeticAssignmentOperators")
    fun `should find uses and heap read-write in dereference used as lvalue in compound assignment`(makeExpr: MakeBinaryExpression) {
        val pDeclaration = variableDeclaration("p")
        val pVariable = Variable.PrimitiveVariable()
        val pUse = variableUse("p")
        val qDeclaration = variableDeclaration("q", pUse)
        val qVariable = Variable.PrimitiveVariable()
        val qUse = variableUse("q")

        // @(let q = p; q) op= 42
        val dereference = deref(block(qDeclaration, qUse))
        val assignment = makeExpr(dereference, lit(42))
        val ast = astOf(pDeclaration, assignment)
        val program = program(ast)
        val variablesMap =
            createVariablesMap(
                mapOf(pDeclaration to pVariable, qDeclaration to qVariable),
                mapOf(pUse to pVariable, qUse to qVariable, dereference to Variable.Heap),
            )
        val result =
            analyzeVarUseTypes(
                program,
                mapOf(programResolvedName(ast), pUse to pDeclaration, qUse to qDeclaration),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                assignment to
                    mapOf(
                        pVariable to VariableUseType.READ,
                        Variable.Heap to VariableUseType.READ_WRITE,
                    ),
            ),
        )
    }

    @Test
    fun `variable whose value was allocated is not considered used in dereference`() {
        // let x = 15; let p = $x;
        val xDeclaration = variableDeclaration("x", lit(15))
        val xVariable = Variable.PrimitiveVariable()
        val xUse = variableUse("x")
        val pDeclaration = variableDeclaration("p", alloc(xUse))
        val pVariable = Variable.PrimitiveVariable()
        val pUse1 = variableUse("p")
        val deref1 = deref(pUse1)
        val pUse2 = variableUse("p")
        val deref2 = deref(pUse2)
        val pUse3 = variableUse("p")
        val deref3 = deref(pUse3)
        // @p + 1; @p = 2; @p += 3
        val ast =
            astOf(
                xDeclaration,
                pDeclaration,
                deref1 add lit(1),
                deref2 assign lit(2),
                deref3 addeq lit(3),
            )
        val program = program(ast)
        val variablesMap =
            createVariablesMap(
                mapOf(xDeclaration to xVariable, pDeclaration to pVariable),
                mapOf(
                    xUse to xVariable,
                    pUse1 to pVariable,
                    pUse2 to pVariable,
                    pUse3 to pVariable,
                    deref1 to Variable.Heap,
                    deref2 to Variable.Heap,
                    deref3 to Variable.Heap,
                ),
            )
        val result =
            analyzeVarUseTypes(
                program,
                mapOf(programResolvedName(ast), xUse to xDeclaration, pUse1 to pDeclaration, pUse2 to pDeclaration, pUse3 to pDeclaration),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                deref1 to mapOf(pVariable to VariableUseType.READ, Variable.Heap to VariableUseType.READ),
                deref2 to mapOf(pVariable to VariableUseType.READ, Variable.Heap to VariableUseType.WRITE),
                deref3 to mapOf(pVariable to VariableUseType.READ, Variable.Heap to VariableUseType.READ_WRITE),
            ),
        )
        assertThat(result[program]).doesNotContainEntry(xVariable, VariableUseType.WRITE)
        assertThat(result[program]).doesNotContainEntry(xVariable, VariableUseType.READ_WRITE)
    }

    @Test
    fun `dereference followed by field accesses uses just the pointer (read) and the heap (read, write, read-write)`() {
        val pDeclaration = variableDeclaration("p")
        val pVariable = Variable.PrimitiveVariable()
        val pUse1 = variableUse("p")
        val deref1 = deref(pUse1)
        val expr1 = deref1 dot "a" dot "b" add lit(1)
        val pUse2 = variableUse("p")
        val deref2 = deref(pUse2)
        val expr2 = deref2 dot "c" dot "d" assign lit(2)
        val pUse3 = variableUse("p")
        val deref3 = deref(pUse3)
        val expr3 = deref3 dot "x" dot "y" addeq lit(3)
        val pUse4 = variableUse("p")
        val deref4 = deref(pUse4)
        val pUse5 = variableUse("p")
        val deref5 = deref(pUse5)
        val expr4 = ifThenElse(lit(true), deref4, deref5) dotConst "u" dotConst "v" add lit(4)
        /*
         * Note:
         *   In the below snippet, the parentheses around @p just for grouping, they are not blocks
         *   The one around if-then-else is a block
         * let p; (@p).a.b = 1; (@p).c.d = 2; (@p).x.y += 3; (if true then @p else @p).u.v + 4
         */
        val ast = astOf(pDeclaration, expr1, expr2, expr3, expr4)
        val program = program(ast)
        // Note that fields are accessed after the dereference, so they are assumed to be on heap and do not need to be mapped to variables
        val variablesMap =
            createVariablesMap(
                mapOf(pDeclaration to pVariable),
                mapOf(
                    pUse1 to pVariable,
                    pUse2 to pVariable,
                    pUse3 to pVariable,
                    pUse4 to pVariable,
                    pUse5 to pVariable,
                    deref1 to Variable.Heap,
                    deref2 to Variable.Heap,
                    deref3 to Variable.Heap,
                    deref4 to Variable.Heap,
                    deref5 to Variable.Heap,
                ),
            )
        val result =
            analyzeVarUseTypes(
                program,
                mapOf(
                    programResolvedName(ast),
                    pUse1 to pDeclaration,
                    pUse2 to pDeclaration,
                    pUse3 to pDeclaration,
                    pUse4 to pDeclaration,
                    pUse5 to pDeclaration,
                ),
                mapOf(programFunctionAnalysis(ast)),
                variablesMap,
            )
        assertThat(result).containsAllEntriesOf(
            mapOf(
                expr1 to mapOf(pVariable to VariableUseType.READ, Variable.Heap to VariableUseType.READ),
                expr2 to mapOf(pVariable to VariableUseType.READ, Variable.Heap to VariableUseType.WRITE),
                expr3 to mapOf(pVariable to VariableUseType.READ, Variable.Heap to VariableUseType.READ_WRITE),
                expr4 to mapOf(pVariable to VariableUseType.READ, Variable.Heap to VariableUseType.READ),
            ),
        )
    }

    @Test
    fun `foreign function calls are treated pessimistically`() {
        /*
         * foreign f: [] -> Unit;
         * f[]
         */
        val fDeclaration = foreignFunctionDeclaration("f", emptyList(), unitType())
        val fUse = variableUse("f")
        val fCall = call(fUse)
        val ast = astOf(fDeclaration, fCall)
        val program = program(ast)
        val result =
            analyzeVarUseTypes(
                program,
                mapOf(programResolvedName(ast), fUse to fDeclaration),
                mapOf(programFunctionAnalysis(ast)),
                createVariablesMap(emptyMap(), emptyMap()),
            )
        assertThat(result).containsAllEntriesOf(mapOf(fCall to mapOf(Variable.Heap to VariableUseType.READ_WRITE)))
    }

    private companion object {
        @JvmStatic
        private fun arithmeticAssignmentOperators(): List<Arguments> = TestOperators.arithmeticAssignmentOperators()
    }
}
