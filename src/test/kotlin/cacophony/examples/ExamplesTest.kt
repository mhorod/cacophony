package cacophony.examples

import cacophony.examples.ExampleRunner
import cacophony.examples.TestDiagnostics
import cacophony.examples.loadExamples
import cacophony.examples.runExample
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.Input
import com.karumi.kotlinsnapshot.matchWithSnapshot
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

class CacophonyExamplesTest {
    class CacophonyExampleRunner : ExampleRunner {
        override fun run(
            input: Input,
            diagnostics: TestDiagnostics,
        ): ExampleResult {
            val pipeline = CacophonyPipeline(diagnostics)
            val AST = pipeline.generateAST(input)
            return ExampleResult(AST, diagnostics)
        }
    }

    @ParameterizedTest
    @MethodSource("examples")
    fun `pipeline produces correct errors and AST`(path: Path) {
        val exampleResult = runExample(
            path,
            lexerRunner,
        )
        exampleResult.matchWithSnapshot("${path.fileName}")
    }

    companion object {
        @JvmStatic
        fun examples() = loadExamples()

        private val lexerRunner = CacophonyExampleRunner()
    }
}
