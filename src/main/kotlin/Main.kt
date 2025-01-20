import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyLogger
import cacophony.pipeline.CacophonyPipeline
import cacophony.pipeline.Params
import cacophony.utils.CompileException
import cacophony.utils.FileInput
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.nameWithoutExtension

class Main : CliktCommand() {
    val file by argument()
    private val logParsing by option("--log-parsing").flag()
    private val logAST by option("--log-ast").flag()
    private val logAnalysis by option("--log-analysis").flag()
    private val logNameRes by option("--log-names").flag()
    private val logOverloads by option("--log-overloads").flag()
    private val logTypes by option("--log-types").flag()
    private val logEscapeAnalysis by option("--log-escape-analysis").flag()
    private val logVariables by option("--log-variables").flag()
    private val logCallGraph by option("--log-callgraph").flag()
    private val logFunctions by option("--log-functions").flag()
    private val logCFG by option("--log-cfg").flag()
    private val logCover by option("--log-cover").flag()
    private val logRegs by option("--log-regs").flag()
    private val logAsm by option("--log-asm").flag()
    private val verbose by option("-v", "--verbose").flag()
    private val saveToFiles by option("-s", "--save").flag()
    private val clearPrevious by option("-c", "--clear").flag()

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        echo("Compiling $file")

        val input = FileInput(file)
        val outputName = Paths.get(file).nameWithoutExtension

        if (!Files.exists(Params.outputParentDir))
            Files.createDirectory(Params.outputParentDir)
        val outputDir = Params.outputParentDir.resolve(outputName)
        if (Files.exists(outputDir) && clearPrevious)
            outputDir.deleteRecursively()
        if (!Files.exists(outputDir))
            Files.createDirectory(outputDir)
        if (!Files.isDirectory(outputDir))
            throw IllegalArgumentException("Path '$outputDir' already exists and is not a directory")
        val diagnostics = CacophonyDiagnostics(input)
        val logger =
            CacophonyLogger(
                verbose || logParsing,
                verbose || logAST,
                verbose || logAnalysis || logNameRes,
                verbose || logAnalysis || logOverloads,
                verbose || logAnalysis || logTypes,
                verbose || logAnalysis || logEscapeAnalysis,
                verbose || logAnalysis || logVariables,
                verbose || logAnalysis || logCallGraph,
                verbose || logAnalysis || logFunctions,
                verbose || logCFG,
                verbose || logCover,
                verbose || logRegs,
                verbose || logAsm,
                if (saveToFiles) outputDir else null,
            )

        try {
            CacophonyPipeline(diagnostics, logger).compile(input, outputName, outputDir)
        } catch (t: CompileException) {
            echo(diagnostics.extractErrors().joinToString("\n"))
        }
    }
}

fun main(args: Array<String>) = Main().main(args)
