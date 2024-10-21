import cacophony.lexer.CacophonyLexer
import cacophony.utils.FileInput
import cacophony.utils.SimpleDiagnostics

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Cacophony Compiler requires one argument <file>")
        return
    }
    println("Compiling ${args[0]}")

    val input = FileInput(args[0])
    val diagnostics = SimpleDiagnostics()
    val tokens = CacophonyLexer().process(input, diagnostics)
    println("Errors: ${diagnostics.getErrors()}")
    println("Tokens: $tokens")
}
