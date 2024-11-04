package cacophony.examples

import cacophony.pipeline.CacophonyPipeline
import cacophony.semantic.syntaxtree.AST
import cacophony.utils.Input
import com.karumi.kotlinsnapshot.matchWithSnapshot
import io.mockk.InternalPlatformDsl.toStr
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

class ExamplesTest {
    class CacophonyExampleRunner : ExampleRunner {
        override fun run(
            input: Input,
            diagnostics: TestDiagnostics,
        ): ExampleResult {
            val pipeline = CacophonyPipeline(diagnostics)
            var ast: AST? = null
            var exceptionMessage: String = ""
            try {
                ast = pipeline.generateAST(input)
            } catch (e: java.lang.Exception) {
                exceptionMessage = e.toStr()
            }
            return ExampleResult(ast, diagnostics, exceptionMessage)
        }
    }

    @ParameterizedTest
    @MethodSource("examples")
    fun `pipeline produces correct errors and AST`(path: Path) {
        var prefix: String = if (path.startsWith("examples/correct")) "correct" else "incorrect"
        val exampleResult =
            runExample(
                path,
                lexerRunner,
            )

        exampleResult.matchWithSnapshot("$prefix/${path.fileName}")
    }

    companion object {
        @JvmStatic
        fun examples() = loadExamples()

        private val lexerRunner = CacophonyExampleRunner()
    }
}
