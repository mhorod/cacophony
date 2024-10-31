import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyGrammar
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.LLOneParser
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
    println("Tokens: $tokens")

    val terminals = tokens.map { token -> ParseTree.Leaf(CacophonyGrammarSymbol.fromLexerToken(token)) }
    val analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol> =
        AnalyzedGrammar.fromGrammar(
            emptyList(),
            CacophonyGrammar.dummyGrammar1,
        )
    val parser = LLOneParser.fromAnalyzedGrammar(analyzedGrammar)

    try {
        println("Parsing successful!")
        val parseTree = parser.process(terminals, diagnostics)
        println(TreePrinter(StringBuilder()).printTree(parseTree))
    } catch (t: Throwable) {
        for (error in diagnostics.getErrors()) {
            println(error.message)
        }
    }
}
