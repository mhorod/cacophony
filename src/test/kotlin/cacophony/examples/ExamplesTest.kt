package cacophony.examples

import cacophony.examples.ExampleRunner
import cacophony.examples.TestDiagnostics
import cacophony.examples.loadExamples
import cacophony.examples.runExample
import cacophony.grammars.ParseTree
import cacophony.parser.CacophonyGrammarSymbol
import cacophony.pipeline.CacophonyPipeline
import cacophony.semantic.syntaxtree.AST
import cacophony.utils.Input
import cacophony.utils.Location
import cacophony.utils.TreePrinter
import com.karumi.kotlinsnapshot.matchWithSnapshot
import io.mockk.InternalPlatformDsl.toStr
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.lang.StringBuilder
import java.nio.file.Path

class CacophonyExamplesTest {
    class CacophonyExampleRunner : ExampleRunner {
        override fun run(
            input: Input,
            diagnostics: TestDiagnostics,
        ): ExampleResult {
            val pipeline = CacophonyPipeline(diagnostics)
            var AST: AST? = null
            var exceptionMessage: String = ""
            try {
                AST = pipeline.generateAST(input)
            } catch (e: java.lang.Exception) {
                exceptionMessage = e.toStr()
            }
            return ExampleResult(AST, diagnostics, exceptionMessage)
        }
    }

    @ParameterizedTest
    @MethodSource("examples")
    fun `pipeline produces correct errors and AST`(path: Path) {
        var prefix: String = if (path.startsWith("examples/correct")) "correct" else "incorrect"
        val exampleResult = runExample(
            path,
            lexerRunner,
        )

        exampleResult.matchWithSnapshot("${prefix}/${path.fileName}")

    }

    companion object {
        @JvmStatic
        fun examples() = loadExamples()

        private val lexerRunner = CacophonyExampleRunner()
    }
}
