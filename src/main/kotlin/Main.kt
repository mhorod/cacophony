import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyGrammar
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.parser.LLOneParser
import cacophony.parser.Utils
import cacophony.utils.FileInput
import cacophony.utils.SimpleDiagnostics

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

    val terminals = tokens.map { token -> ParseTree.Leaf(Utils.lexerTokenToParserToken(token)) }
    val analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol> =
        AnalyzedGrammar.fromGrammar(
            emptyList(),
            CacophonyGrammar.dummyGrammar1,
        )
    val parser = LLOneParser.fromAnalyzedGrammar(analyzedGrammar)

    try {
        parser.process(terminals, diagnostics)
        println("Parsing successful!")
    } catch (t: Throwable) {
        for (error in diagnostics.getErrors()) {
            println(error.message)
        }
    }
}
