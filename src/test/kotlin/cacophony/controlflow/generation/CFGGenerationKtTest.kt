package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.pipeline.CacophonyPipeline
import cacophony.semantic.UseTypeAnalysisResult
import cacophony.semantic.VariableUseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression
import cacophony.semantic.syntaxtree.Statement
import cacophony.semantic.syntaxtree.Type
import cacophony.utils.Location
import cacophony.utils.StringInput
import io.mockk.mockk
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.math.exp

class CFGGenerationKtTest {
    class TestFunctionHandler: FunctionHandler {
        val varRegisters = mutableMapOf<Definition.VariableDeclaration, Register>()

        override fun getFunctionDeclaration(): Definition.FunctionDeclaration {
            TODO()
        }

        override fun generateCall(
            arguments: List<CFGNode>,
            result: Register?,
            respectStackAlignment: Boolean,
        ): List<CFGNode> {
            TODO()
        }

        override fun generateVariableAccess(variable: Variable): CFGNode.LValue = CFGNode.VariableUse(
            when (variable) {
                is Variable.AuxVariable -> Register.VirtualRegister()
                is Variable.SourceVariable -> varRegisters[variable.definition]!!
            }
        )

        override fun getVariableAllocation(variable: Variable): VariableAllocation {
            TODO()
        }

        override fun getVariableFromDefinition(varDef: Definition): Variable {
            TODO()
        }
    }

    private fun printCFGAsGraphviz(cfg: CFGFragment) {
        var nextId = 0
        val ids = cfg.vertices.mapValues { nextId++ }

        println("strict digraph {")
        cfg.vertices.forEach { (label, vertex) ->
            val id = ids[label]
            if (label == cfg.initialLabel)
                println("  node$id [label=\"${vertex.tree}\", style=\"filled\", fillcolor=\"green\"]")
            else if (vertex is CFGVertex.Final)
                println("  node$id [label=\"${vertex.tree}\", style=\"filled\", fillcolor=\"red\"]")
            else
                println("  node$id [label=\"${vertex.tree}\"]")

            when (vertex) {
                is CFGVertex.Conditional -> {
                    println("  node$id -> node${ids[vertex.trueDestination]}")
                    println("  node$id -> node${ids[vertex.falseDestination]}")
                }
                is CFGVertex.Jump -> println("  node$id -> node${ids[vertex.destination]}")
                is CFGVertex.Final -> { /* final has no edges */ }
            }
            println()
        }

        println("}")
    }

    @Test
    fun `test cfg generation of simple additions`() {
        val location = Location(0)
        val range = Pair(location, location)
        val expr = lit(1) add lit(2) add lit(3) add lit(4) add lit(5)
        val mockType: Type = mockk()
        val function = Definition.FunctionDeclaration(mockRange(), "f", null, emptyList(), mockType, expr)

        val useTypeMap: UseTypeAnalysisResult =
            emptyMap<Expression, Map<Definition, VariableUseType>>().withDefault { emptyMap() }

        val handler = TestFunctionHandler()
        val handlers = mapOf(function to handler)

        val cfg = generateCFG(emptyMap(), useTypeMap, handlers)[function]!!
        printCFGAsGraphviz(cfg)
    }

    @Test
    fun `test cfg on simple conditional`() {
        val location = Location(0)
        val range = Pair(location, location)
        val conditional = ifThenElse(lnot(lit(true)), lit(6), lit(7))
        val mockType: Type = mockk()
        val function = Definition.FunctionDeclaration(mockRange(), "f", null, emptyList(), mockType, conditional)

        val useTypeMap: UseTypeAnalysisResult =
            emptyMap<Expression, Map<Definition, VariableUseType>>().withDefault { emptyMap() }

        val handler = TestFunctionHandler()
        val handlers = mapOf(function to handler)

        val cfg = generateCFG(emptyMap(), useTypeMap, handlers)[function]!!
        printCFGAsGraphviz(cfg)
    }

    @Test
    fun `test cfg on if with deep condition`() {
        val program = "let f = [] -> Int => if (false || (false || (false || (false || (false || (false || (false || true))))))) && (true && (true && (true && (true && (true && (true && (true && (false)))))))) then 12 else 24;"
        val pipeline = CacophonyPipeline(mockk())
        val ast = pipeline.generateAST(StringInput(program))
        val functions = pipeline.analyzeFunctions(ast)


        val useTypeMap: UseTypeAnalysisResult =
            emptyMap<Expression, Map<Definition, VariableUseType>>().withDefault { emptyMap() }

        val handler = TestFunctionHandler(  )
        val handlers = functions.mapValues { TestFunctionHandler() }

        val cfg = generateCFG(emptyMap(), useTypeMap, handlers)
        printCFGAsGraphviz(cfg.values.first())
    }


    @Test
    fun `test cfg on nested ifs in condition`() {
        val program = "let f = [] -> Int => if if true then false else true then 10 else 20;"
        val pipeline = CacophonyPipeline(mockk())
        val ast = pipeline.generateAST(StringInput(program))
        val functions = pipeline.analyzeFunctions(ast)


        val useTypeMap: UseTypeAnalysisResult =
            emptyMap<Expression, Map<Definition, VariableUseType>>().withDefault { emptyMap() }

        val handler = TestFunctionHandler(  )
        val handlers = functions.mapValues { TestFunctionHandler() }

        val cfg = generateCFG(emptyMap(), useTypeMap, handlers)
        printCFGAsGraphviz(cfg.values.first())
    }

    @Test
    fun `test cfg on nested ifs in branches`() {
        val program = "let f = [] -> Int => if true then (if (if false then false else true) then 11 else 12) else (if true then 13  else 15);"
        val pipeline = CacophonyPipeline(mockk())
        val ast = pipeline.generateAST(StringInput(program))
        val functions = pipeline.analyzeFunctions(ast)


        val useTypeMap: UseTypeAnalysisResult =
            emptyMap<Expression, Map<Definition, VariableUseType>>().withDefault { emptyMap() }

        val handler = TestFunctionHandler(  )
        val handlers = functions.mapValues { TestFunctionHandler() }

        val cfg = generateCFG(emptyMap(), useTypeMap, handlers)
        printCFGAsGraphviz(cfg.values.first())
    }

    @Test
    fun `test cfg with a single variable`() {
        // let f = [] => (let x = 11; x = 22)
        val xDef = variableDeclaration("x", lit(11))
        val xUse = variableUse("x")
        val xWrite = variableWrite(xUse, lit(22))
        val fBlock = block(xDef, xWrite)
        val fDef = functionDeclaration("f", fBlock)

        val useTypeMap: UseTypeAnalysisResult = mapOf(
            fBlock to mapOf(xDef to VariableUseType.READ_WRITE),
            xDef to mapOf(xDef to VariableUseType.WRITE),
            xWrite to mapOf(xDef to VariableUseType.WRITE),
        )


        val handler = TestFunctionHandler()
        handler.varRegisters[xDef] = Register.FixedRegister(X64Register.RSI)
        val handlers = mapOf(fDef to handler)
        val resolvedVariables = mapOf(xUse to xDef)
        val cfg = generateCFG(resolvedVariables, useTypeMap, handlers)
        printCFGAsGraphviz(cfg.values.first())
    }

    @Test
    fun `test cfg of if with a single variable`() {
        // let f = [] => (let x = true; if x then 10 else 20)
        val xDef = variableDeclaration("x", lit(true))
        val xUse = variableUse("x")
        val thenBranch = lit(10)
        val elseBranch = lit(20)
        val ifThenElse = ifThenElse(xUse, thenBranch, elseBranch)
        val fBlock = block(xDef, ifThenElse)
        val fDef = functionDeclaration("f", fBlock)

        val useTypeMap: UseTypeAnalysisResult = mapOf(
            fBlock to mapOf(xDef to VariableUseType.READ_WRITE),
            xDef to mapOf(xDef to VariableUseType.WRITE),
            ifThenElse to mapOf(xDef to VariableUseType.READ),
            thenBranch to emptyMap(),
            elseBranch to emptyMap()
        )


        val handler = TestFunctionHandler()
        handler.varRegisters[xDef] = Register.FixedRegister(X64Register.RSI)
        val handlers = mapOf(fDef to handler)
        val resolvedVariables = mapOf(xUse to xDef)
        val cfg = generateCFG(resolvedVariables, useTypeMap, handlers)
        printCFGAsGraphviz(cfg.values.first())
    }

    @Test
    fun `test cfg of addition with clashing side effects`() {
        // let f = [] => (let x = 1; (x = 2) + (x = 3));
        val xDef = variableDeclaration("x", lit(1))
        val xUseLeft = variableUse("x")
        val xUseRight = variableUse("x")

        val two = lit(2)
        val xWriteLeft = variableWrite(xUseLeft, two)

        val three = lit(3)
        val xWriteRight = variableWrite(xUseRight, three)

        val addition = xWriteLeft add xWriteRight

        val fBlock = block(xDef, addition)
        val fDef = functionDeclaration("f", fBlock)

        val useTypeMap: UseTypeAnalysisResult = mapOf(
            fBlock to mapOf(xDef to VariableUseType.WRITE),
            xDef to mapOf(xDef to VariableUseType.WRITE),
            xWriteLeft to mapOf(xDef to VariableUseType.WRITE),
            xWriteRight to mapOf(xDef to VariableUseType.WRITE),
            addition to mapOf(xDef to VariableUseType.WRITE),
            two to emptyMap(),
            three to emptyMap(),
            )

        val handler = TestFunctionHandler()
        handler.varRegisters[xDef] = Register.FixedRegister(X64Register.RSI)
        val handlers = mapOf(fDef to handler)
        val resolvedVariables = mapOf(xUseLeft to xDef, xUseRight to xDef)
        val cfg = generateCFG(resolvedVariables, useTypeMap, handlers)
        printCFGAsGraphviz(cfg.values.first())
    }
}