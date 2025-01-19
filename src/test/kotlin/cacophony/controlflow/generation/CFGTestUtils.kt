package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.SimpleCallGenerator
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Struct
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk

object MockFunctionParts {
    val prologue: CFGNode = CFGNode.Comment("prologue")
    val epilogue: CFGNode = CFGNode.Comment("epilogue")
}

internal fun generateSimplifiedCFG(
    ast: AST,
    realPrologue: Boolean = false,
    realEpilogue: Boolean = false,
    fullCallSequences: Boolean = false,
): ProgramCFG {
    val pipeline = testPipeline()
    val analyzedAST = pipeline.analyzeAst(ast)
    val stubbedFunctionHandlers =
        analyzedAST.functionHandlers.mapValues { (_, handler) ->
            val stubbedHandler = spyk(handler)
            if (!realPrologue)
                every { stubbedHandler.generatePrologue() } returns listOf(MockFunctionParts.prologue)
            if (!realEpilogue)
                every { stubbedHandler.generateEpilogue() } returns listOf(MockFunctionParts.epilogue)
            stubbedHandler
        }
    val mockAnalyzedAST = spyk(analyzedAST)
    every { mockAnalyzedAST.functionHandlers } returns stubbedFunctionHandlers
    val callGenerator =
        if (fullCallSequences)
            SimpleCallGenerator()
        else {
            val generator: CallGenerator = mockk()
            every { generator.generateCallFrom(any(), any(), any(), any()) } answers {
                listOf(CFGNode.Call(arg<Definition.FunctionDefinition>(1)))
            }
            generator
        }
    return pipeline.generateControlFlowGraph(mockAnalyzedAST, callGenerator, mockk(), mockk())
}

fun singleFragmentCFG(definition: Definition.FunctionDefinition, body: CFGFragmentBuilder.() -> Unit): ProgramCFG =
    cfg { fragment(definition, body) }

internal fun standaloneCFGFragment(definition: Definition.FunctionDefinition, body: CFGFragmentBuilder.() -> Unit): CFGFragment =
    singleFragmentCFG(definition, body)[definition]!!

internal fun singleWrappedFragmentCFG(definition: Definition.FunctionDefinition, body: CFGFragmentBuilder.() -> Unit): ProgramCFG =
    cfg { wrappedCFGFragment(definition, body) }

internal fun CFGBuilder.wrappedCFGFragment(definition: Definition.FunctionDefinition, body: CFGFragmentBuilder.() -> Unit) =
    fragment(definition) {
        "entry" does jump("bodyEntry") { MockFunctionParts.prologue }
        body()
        "bodyExit" does jump("exit") { MockFunctionParts.epilogue }
        "exit" does final { returnNode(definition.returnType.size()) }
    }

internal fun standaloneWrappedCFGFragment(definition: Definition.FunctionDefinition, body: CFGFragmentBuilder.() -> Unit): CFGFragment =
    singleWrappedFragmentCFG(definition, body)[definition]!!

// {a = 7, b = true}
fun simpleStruct(int: Int = 7, bool: Boolean = true): Struct =
    structDeclaration(
        structField("a") to lit(int),
        structField("b") to lit(bool),
    )

fun simpleType(): BaseType.Structural = structType("a" to intType(), "b" to boolType())

// {a = 5, b = {a = 7, b = true} }
fun nestedStruct(): Struct =
    structDeclaration(
        structField("a") to lit(5),
        structField("b") to simpleStruct(),
    )

fun nestedType(): BaseType.Structural =
    structType(
        "a" to intType(),
        "b" to simpleType(),
    )
