package cacophony.lexer

import cacophony.examples.AssertNoErrors
import cacophony.examples.IncorrectExampleDescription
import cacophony.examples.Runner
import cacophony.examples.TestDiagnostics
import cacophony.examples.assertionFromDescription
import cacophony.examples.loadCorrectExamples
import cacophony.examples.loadIncorrectExamples
import cacophony.examples.runExample
import cacophony.utils.Input
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

class LexerExampleRunner {
    class LexerRunner : Runner {
        override fun run(
            input: Input,
            diagnostics: TestDiagnostics,
        ) {
            // TODO: Call lexer when implemented
        }
    }

    @ParameterizedTest
    @MethodSource("correctExamples")
    fun `lexer lexes correct examples without errors`(path: Path) {
        runExample(
            path,
            LexerRunner(),
            AssertNoErrors(),
        )
    }

    @ParameterizedTest
    @MethodSource("incorrectExamples")
    fun `lexer lexes incorrect examples with described errors`(description: IncorrectExampleDescription) {
        runExample(
            Path.of(description.path),
            LexerRunner(),
            assertionFromDescription(description),
        )
    }

    companion object {
        @JvmStatic
        fun correctExamples() = loadCorrectExamples()

        @JvmStatic
        fun incorrectExamples() = loadIncorrectExamples()
    }
}
