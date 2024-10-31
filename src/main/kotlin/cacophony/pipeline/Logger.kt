package cacophony.pipeline

import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.semantic.*
import cacophony.semantic.syntaxtree.AST
import cacophony.token.Token

interface Logger<StateT, TokenT : Enum<TokenT>, GrammarSymbol : Enum<GrammarSymbol>> {
    fun logSuccessfulLexing(tokens: List<Token<TokenT>>)

    fun logFailedLexing()

    fun logSuccessfulGrammarAnalysis(analyzedGrammar: AnalyzedGrammar<StateT, GrammarSymbol>)

    fun logFailedGrammarAnalysis()

    fun logSuccessfulParsing(parseTree: ParseTree<GrammarSymbol>)

    fun logFailedParsing()

    fun logSuccessfulAstGeneration(ast: AST)

    fun logFailedAstGeneration()

    fun logSuccessfulNameResolution(result: NameResolutionResult)

    fun logFailedNameResolution()

    fun logSuccessfulOverloadResolution(result: ResolvedVariables)

    fun logFailedOverloadResolution()

    fun logSuccessfulTypeChecking(result: TypeCheckingResult)

    fun logFailedTypeChecking()

    fun logSuccessfulCallGraphGeneration(callGraph: CallGraph)

    fun logFailedCallGraphGeneration()

    fun logSuccessfulFunctionAnalysis(result: FunctionAnalysisResult)

    fun logFailedFunctionAnalysis()
}
