import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyLogger
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.CompileException
import cacophony.utils.FileInput
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Paths

class Main : CliktCommand() {
    val file by argument()
    val outputFile: String? by option("-o", "--output")
    val logParsing by option("--log-parsing").flag()
    val logAST by option("--log-ast").flag()
    val logAnalysis by option("--log-analysis").flag()
    val logCFG by option("--log-cfg").flag()
    val logCover by option("--log-cover").flag()
    val logRegs by option("--log-regs").flag()
    val logAsm by option("--log-asm").flag()
    val verbose by option("-v", "--verbose").flag()

    override fun run() {
        echo("Compiling $file")

        val input = FileInput(file)
        val output = Paths.get(outputFile ?: file)
        val diagnostics = CacophonyDiagnostics(input)
        val logger =
            CacophonyLogger(
                verbose || logParsing,
                verbose || logAST,
                verbose || logAnalysis,
                verbose || logCFG,
                verbose || logCover,
                verbose || logRegs,
                verbose || logAsm,
            )

        try {
            CacophonyPipeline(diagnostics, logger).compile(input, output)
        } catch (t: CompileException) {
            echo(diagnostics.extractErrors().joinToString("\n"))
        }
    }
}

fun main(args: Array<String>) = Main().main(args)
