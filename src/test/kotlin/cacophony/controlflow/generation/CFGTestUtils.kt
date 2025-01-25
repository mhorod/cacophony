package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.*
import cacophony.controlflow.functions.CallGenerator
import cacophony.controlflow.functions.CallableHandler
import cacophony.controlflow.functions.CallableHandlers
import cacophony.controlflow.functions.SimpleCallGenerator
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.LambdaExpression
import cacophony.semantic.syntaxtree.Struct
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk

object MockFunctionParts {
    val prologue: CFGNode = CFGNode.Comment("prologue")
    val epilogue: CFGNode = CFGNode.Comment("epilogue")
}

private fun stubCallableHandlers(callableHandlers: CallableHandlers, realPrologue: Boolean, realEpilogue: Boolean): CallableHandlers =
    CallableHandlers(
        callableHandlers.closureHandlers.mapValues { (_, handler) -> stubHandler(handler, realPrologue, realEpilogue) },
        callableHandlers.staticFunctionHandlers.mapValues { (_, handler) -> stubHandler(handler, realPrologue, realEpilogue) },
    )

private inline fun <reified H : CallableHandler> stubHandler(handler: H, realPrologue: Boolean, realEpilogue: Boolean): H {
    val stubbedHandler = spyk(handler)
    if (!realPrologue)
        every { stubbedHandler.generatePrologue() } returns listOf(MockFunctionParts.prologue)
    if (!realEpilogue)
        every { stubbedHandler.generateEpilogue() } returns listOf(MockFunctionParts.epilogue)
    return stubbedHandler
}

internal fun generateSimplifiedCFG(
    ast: AST,
    realPrologue: Boolean = false,
    realEpilogue: Boolean = false,
    fullCallSequences: Boolean = false,
): ProgramCFG {
    val pipeline = testPipeline()
    val analyzedAST = pipeline.analyzeAst(ast)
    val mockAnalyzedAST = spyk(analyzedAST)
    every { mockAnalyzedAST.callableHandlers } returns stubCallableHandlers(analyzedAST.callableHandlers, realPrologue, realEpilogue)
    val callGenerator =
        if (fullCallSequences)
            SimpleCallGenerator()
        else {
            val generator: CallGenerator = mockk()
            every { generator.generateCallFrom(any(), any(), any(), any(), any()) } answers {
                listOf(CFGNode.Call(arg<FunctionLayout>(2).code.access, integer(arg<List<Layout>>(3).size)))
            }
            generator
        }
    return pipeline.generateControlFlowGraph(mockAnalyzedAST, callGenerator, mockk(), mockk())
}

fun singleFragmentCFG(function: LambdaExpression, body: CFGFragmentBuilder.() -> Unit): ProgramCFG = cfg { fragment(function, body) }

internal fun standaloneCFGFragment(function: LambdaExpression, body: CFGFragmentBuilder.() -> Unit): CFGFragment =
    singleFragmentCFG(function, body)[function]!!

internal fun singleWrappedFragmentCFG(function: LambdaExpression, body: CFGFragmentBuilder.() -> Unit): ProgramCFG =
    cfg { wrappedCFGFragment(function, body) }

internal fun singleWrappedFragmentCFG(function: Definition.FunctionDefinition, body: CFGFragmentBuilder.() -> Unit): ProgramCFG =
    cfg { wrappedCFGFragment(function.value, body) }

internal fun CFGBuilder.wrappedCFGFragment(function: LambdaExpression, body: CFGFragmentBuilder.() -> Unit) =
    fragment(function) {
        "entry" does jump("bodyEntry") { MockFunctionParts.prologue }
        body()
        "bodyExit" does jump("exit") { MockFunctionParts.epilogue }
        "exit" does final { returnNode(function.returnType.size()) }
    }

internal fun standaloneWrappedCFGFragment(function: LambdaExpression, body: CFGFragmentBuilder.() -> Unit): CFGFragment =
    singleWrappedFragmentCFG(function, body)[function]!!

internal fun standaloneWrappedCFGFragment(function: Definition.FunctionDefinition, body: CFGFragmentBuilder.() -> Unit): CFGFragment =
    singleWrappedFragmentCFG(function.value, body)[function.value]!!

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
