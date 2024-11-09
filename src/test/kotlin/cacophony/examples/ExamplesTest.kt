package cacophony.examples

import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyParser
import cacophony.pipeline.CacophonyPipeline
import cacophony.semantic.syntaxtree.AST
import cacophony.utils.*
import com.karumi.kotlinsnapshot.matchWithSnapshot
import io.mockk.InternalPlatformDsl.toStr
import org.assertj.core.api.Assertions.assertThat
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
        val prefix: String = if (path.startsWith("examples/correct")) "correct" else "incorrect"
        val exampleResult =
            runExample(
                path,
                lexerRunner,
            )

        exampleResult.matchWithSnapshot("$prefix/${path.fileName}")
    }

    @ParameterizedTest
    @MethodSource("correctExamples")
    fun `correct examples compile without errors`(path: Path) {
        val input = FileInput(path.toString())
        val diagnostics = SimpleDiagnostics(input)
        CacophonyPipeline(diagnostics, null, lexer, parser).process(input)
        diagnostics.getErrors().forEach {
            println(it.message)
        }
        assertThat(diagnostics.getErrors()).isEmpty()
    }

    companion object {
        @JvmStatic
        fun examples() = loadExamples()

        @JvmStatic
        fun correctExamples() = loadCorrectExamples()

        private val lexer = CacophonyLexer()
        private val parser = CacophonyParser()

        private val lexerRunner = CacophonyExampleRunner()
    }
}
