package cacophony.pipeline

import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.semantic.CallGraph
import cacophony.semantic.FunctionAnalysisResult
import cacophony.semantic.NameResolutionResult
import cacophony.semantic.ResolvedVariables
import cacophony.semantic.TypeCheckingResult
import cacophony.semantic.VariableUseType
import cacophony.semantic.syntaxtree.AST
import cacophony.token.Token
import cacophony.token.TokenCategorySpecific
import cacophony.utils.TreePrinter

class CacophonyLogger : Logger<Int, TokenCategorySpecific, CacophonyGrammarSymbol> {
    override fun logSuccessfulGrammarAnalysis(analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol>) =
        println("Grammar analysis successful :D")

    override fun logFailedGrammarAnalysis() = println("Grammar analysis failed :(")

    override fun logSuccessfulLexing(tokens: List<Token<TokenCategorySpecific>>) {
        println("Lexing successful :D")
        println("Found tokens:")
        tokens.forEach { println("  $it") }
    }

    override fun logFailedLexing() = println("Lexing failed :(")

    override fun logSuccessfulParsing(parseTree: ParseTree<CacophonyGrammarSymbol>) {
        println("Parsing successful :D")
        println("Parse tree:")
        println(TreePrinter(StringBuilder()).printTree(parseTree))
    }

    override fun logFailedParsing() = println("Parsing failed :(")

    override fun logSuccessfulAstGeneration(ast: AST) {
        println("AST generation successful :D")
        println("AST:")
        println(TreePrinter(StringBuilder()).printTree(ast))
    }

    override fun logFailedAstGeneration() = println("AST generation failed :(")

    override fun logSuccessfulNameResolution(result: NameResolutionResult) {
        println("Name resolution successful :D")
        println("Resolved names:")
        result.forEach { println("  ${it.key.identifier} -> ${it.value}") }
    }

    override fun logFailedNameResolution() = println("Name resolution failed :(")

    override fun logSuccessfulOverloadResolution(result: ResolvedVariables) {
        println("Overload resolution successful :D")
        println("Resolved variables:")
        result.forEach { println("  ${it.key.identifier} -> ${it.value}") }
    }

    override fun logFailedOverloadResolution() = println("Overload resolution failed :(")

    override fun logSuccessfulTypeChecking(result: TypeCheckingResult) {
        println("Type checking successful :D")
        println("Types:")
        result.forEach { println("  ${it.key} : ${it.value}") }
    }

    override fun logFailedTypeChecking() = println("Type checking failed :(")

    override fun logSuccessfulCallGraphGeneration(callGraph: CallGraph) {
        println("Call graph generation successful :D")
        println("Calls:")
        callGraph.forEach { (caller, callees) ->
            println("  $caller (${caller.identifier}/${caller.arguments.size}) calls:")
            callees.forEach { callee ->
                println("    $callee (${callee.identifier}/${callee.arguments.size})")
            }
        }
    }

    override fun logFailedCallGraphGeneration() = println("Call graph generation failed :(")

    override fun logSuccessfulFunctionAnalysis(result: FunctionAnalysisResult) {
        println("Function analysis successful :D")
        result.forEach { (function, analysis) ->
            println("  $function (${function.identifier}/${function.arguments.size}) at static depth ${analysis.staticDepth}:")
            analysis.variables.forEach { variable ->
                val usage =
                    when (variable.useType) {
                        VariableUseType.UNUSED -> "--"
                        VariableUseType.READ -> "r-"
                        VariableUseType.WRITE -> "-w"
                        VariableUseType.READ_WRITE -> "rw"
                    }
                val from =
                    "${variable.definedIn} (${variable.definedIn.identifier}/${variable.definedIn.arguments.size})"
                println("    [$usage] ${variable.declaration} (${variable.declaration.identifier}) from $from")
            }
        }
    }

    override fun logFailedFunctionAnalysis() = println("Function analysis failed :(")
}
