import cacophony.pipeline.CacophonyLogger
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.CompileException
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

    try {
        CacophonyPipeline(diagnostics, CacophonyLogger()).process(input)
    } catch (t: CompileException) {
        for (error in diagnostics.getErrors()) {
            println(error.message)
        }
    }
}
