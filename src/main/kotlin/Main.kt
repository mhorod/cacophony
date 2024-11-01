import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyGrammar
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.LLOneParser
import cacophony.utils.CompileErrorException
import cacophony.utils.FileInput
import cacophony.utils.SimpleDiagnostics
import cacophony.utils.TreePrinter

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Cacophony Compiler requires one argument <file>")
        return
    }
    println("Compiling ${args[0]}")

    val input = FileInput(args[0])
    val diagnostics = SimpleDiagnostics(input)
    val tokens = CacophonyLexer().process(input, diagnostics)
    println("Tokens: ${tokens.joinToString(separator = "\n")}")

    val terminals = tokens.map { token -> ParseTree.Leaf(CacophonyGrammarSymbol.fromLexerToken(token)) }
    val analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol> =
        AnalyzedGrammar.fromGrammar(
            emptyList(),
            CacophonyGrammar.grammar,
        )
    val parser = LLOneParser.fromAnalyzedGrammar(analyzedGrammar)

    try {
        val parseTree = parser.process(terminals, diagnostics)
        println("Parsing successful!")
        println(TreePrinter(StringBuilder()).printTree(parseTree))
        for (error in diagnostics.getErrors()) {
            println(error.message)
        }
    } catch (t: CompileErrorException) {
        println("Wyjebało się")
        for (error in diagnostics.getErrors()) {
            println(error.message)
        }
    }
}
