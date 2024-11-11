package cacophony.pipeline

import cacophony.diagnostics.Diagnostics
import cacophony.grammars.ParseTree
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.CacophonyParser
import cacophony.semantic.CallGraph
import cacophony.semantic.FunctionAnalysisResult
import cacophony.semantic.NameResolutionResult
import cacophony.semantic.ResolvedVariables
import cacophony.semantic.TypeCheckingResult
import cacophony.semantic.syntaxtree.AST
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.CompileException
import cacophony.utils.Input

class CacophonyPipeline(
    val diagnostics: Diagnostics,
    private val logger: CacophonyLogger? = null,
    private val lexer: CacophonyLexer = cachedLexer,
    private val parser: CacophonyParser = cachedParser,
) {
    companion object {
        private val cachedLexer = CacophonyLexer()
        private val cachedParser = CacophonyParser()
    }

    private fun <T> assertEmptyDiagnosticsAfter(action: () -> T): T {
        val x = action()
        if (diagnostics.getErrors().isNotEmpty()) {
            throw diagnostics.fatal()
        }
        return x
    }

    // run the full pipeline
    fun process(input: Input): FunctionAnalysisResult = analyzeFunctions(input)

    private fun lex(input: Input): List<Token<TokenCategorySpecific>> {
        val tokens =
            try {
                assertEmptyDiagnosticsAfter { lexer.process(input, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedLexing()
                throw e
            }
        logger?.logSuccessfulLexing(tokens)
        return tokens
    }

    private fun parse(input: Input): ParseTree<CacophonyGrammarSymbol> {
        val terminals = lex(input).map { token -> ParseTree.Leaf(CacophonyGrammarSymbol.fromLexerToken(token)) }
        val parseTree =
            try {
                assertEmptyDiagnosticsAfter { parser.process(terminals, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedParsing()
                throw e
            }
        logger?.logSuccessfulParsing(parseTree)
        return parseTree
    }

    fun generateAST(input: Input): AST {
        val parseTree = parse(input)
        val ast =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.generateAST(parseTree, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedAstGeneration()
                throw e
            }
        logger?.logSuccessfulAstGeneration(ast)
        return ast
    }

    private fun resolveNames(ast: AST): NameResolutionResult {
        val result =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.resolveNames(ast, diagnostics) }
            } catch (e: CompileException) {
                logger?.logFailedNameResolution()
                throw e
            }
        logger?.logSuccessfulNameResolution(result)
        return result
    }

    fun resolveOverloads(ast: AST): ResolvedVariables {
        val nr = resolveNames(ast)
        return resolveOverloads(ast, nr)
    }

    private fun resolveOverloads(
        ast: AST,
        nr: NameResolutionResult,
    ): ResolvedVariables {
        val result =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.resolveOverloads(ast, diagnostics, nr) }
            } catch (e: CompileException) {
                logger?.logFailedOverloadResolution()
                throw e
            }
        logger?.logSuccessfulOverloadResolution(result)
        return result
    }

    fun checkTypes(
        ast: AST,
        resolvedVariables: ResolvedVariables,
    ): TypeCheckingResult {
        val types =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.checkTypes(ast, diagnostics, resolvedVariables) }
            } catch (e: CompileException) {
                logger?.logFailedTypeChecking()
                throw e
            }
        logger?.logSuccessfulTypeChecking(types)
        return types
    }

    private fun generateCallGraph(
        ast: AST,
        resolvedVariables: ResolvedVariables,
    ): CallGraph {
        val callGraph =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.generateCallGraph(ast, diagnostics, resolvedVariables) }
            } catch (e: CompileException) {
                logger?.logFailedCallGraphGeneration()
                throw e
            }
        logger?.logSuccessfulCallGraphGeneration(callGraph)
        return callGraph
    }

    private fun analyzeFunctions(input: Input): FunctionAnalysisResult = analyzeFunctions(generateAST(input))

    private fun analyzeFunctions(ast: AST): FunctionAnalysisResult {
        val resolvedFunctions = resolveOverloads(ast)
        return analyzeFunctions(ast, resolvedFunctions)
    }

    private fun analyzeFunctions(
        ast: AST,
        resolvedVariables: ResolvedVariables,
    ): FunctionAnalysisResult {
        val types = checkTypes(ast, resolvedVariables)
        return analyzeFunctions(ast, resolvedVariables, types)
    }

    fun analyzeFunctions(
        ast: AST,
        resolvedVariables: ResolvedVariables,
        types: TypeCheckingResult,
    ): FunctionAnalysisResult {
        val callGraph = generateCallGraph(ast, resolvedVariables)
        return analyzeFunctions(ast, resolvedVariables, types, callGraph)
    }

    private fun analyzeFunctions(
        ast: AST,
        resolvedVariables: ResolvedVariables,
        types: TypeCheckingResult,
        callGraph: CallGraph,
    ): FunctionAnalysisResult {
        val result =
            try {
                assertEmptyDiagnosticsAfter { cacophony.semantic.analyzeFunctions(ast, resolvedVariables, callGraph) }
            } catch (e: CompileException) {
                logger?.logFailedFunctionAnalysis()
                throw e
            }
        logger?.logSuccessfulFunctionAnalysis(result)
        return result
    }
}
