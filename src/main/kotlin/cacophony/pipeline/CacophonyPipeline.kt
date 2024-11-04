package cacophony.pipeline

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
import cacophony.utils.Diagnostics
import cacophony.utils.Input

class CacophonyPipeline(
    val diagnostics: Diagnostics,
    val logger: CacophonyLogger? = null,
    private val lexer: CacophonyLexer = cachedLexer,
    private val parser: CacophonyParser = cachedParser,
) {
    companion object {
        private val cachedLexer = CacophonyLexer()
        private val cachedParser = CacophonyParser()
    }

    // run the full pipeline
    fun process(input: Input): FunctionAnalysisResult = analyzeFunctions(input)

    fun lex(input: Input): List<Token<TokenCategorySpecific>> {
        val tokens =
            try {
                lexer.process(input, diagnostics)
            } catch (e: CompileException) {
                logger?.logFailedLexing()
                throw e
            }
        logger?.logSuccessfulLexing(tokens)
        return tokens
    }

    fun parse(input: Input): ParseTree<CacophonyGrammarSymbol> {
        val terminals = lex(input).map { token -> ParseTree.Leaf(CacophonyGrammarSymbol.fromLexerToken(token)) }
        val parseTree =
            try {
                parser.process(terminals, diagnostics)
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
                cacophony.semantic.generateAST(parseTree, diagnostics)
            } catch (e: CompileException) {
                logger?.logFailedAstGeneration()
                throw e
            }
        logger?.logSuccessfulAstGeneration(ast)
        return ast
    }

    fun resolveNames(input: Input): NameResolutionResult = resolveNames(generateAST(input))

    fun resolveNames(ast: AST): NameResolutionResult {
        val result =
            try {
                cacophony.semantic.resolveNames(ast, diagnostics)
            } catch (e: CompileException) {
                logger?.logFailedNameResolution()
                throw e
            }
        logger?.logSuccessfulNameResolution(result)
        return result
    }

    fun resolveOverloads(input: Input): ResolvedVariables = resolveOverloads(generateAST(input))

    fun resolveOverloads(ast: AST): ResolvedVariables {
        val nr = resolveNames(ast)
        return resolveOverloads(ast, nr)
    }

    fun resolveOverloads(
        ast: AST,
        nr: NameResolutionResult,
    ): ResolvedVariables {
        val result =
            try {
                cacophony.semantic.resolveOverloads(ast, diagnostics, nr)
            } catch (e: CompileException) {
                logger?.logFailedOverloadResolution()
                throw e
            }
        logger?.logSuccessfulOverloadResolution(result)
        return result
    }

    fun checkTypes(input: Input): TypeCheckingResult = checkTypes(generateAST(input))

    fun checkTypes(ast: AST): TypeCheckingResult {
        val nr = resolveNames(ast)
        return checkTypes(ast, resolveOverloads(ast, nr))
    }

    fun checkTypes(
        ast: AST,
        resolvedVariables: ResolvedVariables,
    ): TypeCheckingResult {
        val types =
            try {
                cacophony.semantic.checkTypes(ast, diagnostics, resolvedVariables)
            } catch (e: CompileException) {
                logger?.logFailedTypeChecking()
                throw e
            }
        logger?.logSuccessfulTypeChecking(types)
        return types
    }

    fun generateCallGraph(input: Input): CallGraph = generateCallGraph(generateAST(input))

    fun generateCallGraph(ast: AST): CallGraph {
        val resolvedVariables = resolveOverloads(ast)
        return generateCallGraph(ast, resolvedVariables)
    }

    fun generateCallGraph(
        ast: AST,
        resolvedVariables: ResolvedVariables,
    ): CallGraph {
        val callGraph =
            try {
                cacophony.semantic.generateCallGraph(ast, diagnostics, resolvedVariables)
            } catch (e: CompileException) {
                logger?.logFailedCallGraphGeneration()
                throw e
            }
        logger?.logSuccessfulCallGraphGeneration(callGraph)
        return callGraph
    }

    fun analyzeFunctions(input: Input): FunctionAnalysisResult = analyzeFunctions(generateAST(input))

    fun analyzeFunctions(ast: AST): FunctionAnalysisResult {
        val resolvedFunctions = resolveOverloads(ast)
        return analyzeFunctions(ast, resolvedFunctions)
    }

    fun analyzeFunctions(
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

    fun analyzeFunctions(
        ast: AST,
        resolvedVariables: ResolvedVariables,
        types: TypeCheckingResult,
        callGraph: CallGraph,
    ): FunctionAnalysisResult {
        val result =
            try {
                cacophony.semantic.analyzeFunctions(ast, resolvedVariables, callGraph)
            } catch (e: CompileException) {
                logger?.logFailedFunctionAnalysis()
                throw e
            }
        logger?.logSuccessfulFunctionAnalysis(result)
        return result
    }
}
