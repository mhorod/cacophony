package cacophony.examples

import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.lexer.CacophonyLexer
import cacophony.parser.CacophonyParser
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.FileInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatPath
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.listDirectoryEntries

class IOExamplesTest {
    @Disabled
    @ParameterizedTest
    @MethodSource("ioExamples")
    fun `io tests print expected output on given input`(path: Path) {
        val inputPath = path.resolve("input.txt")
        val expectedOutputPath = path.resolve("output.txt")
        val actualOutputPath = path.resolve("actual_output.txt")

        val programPath = path.resolve("program.cac")
        val binDir = path.resolve("bin")

        val input = FileInput(programPath.toString())
        val diagnostics = CacophonyDiagnostics(input)
        val pipeline = CacophonyPipeline(diagnostics, null, CacophonyLexer(), CacophonyParser())

        assertThatCode { pipeline.compile(input, programPath, binDir) }.doesNotThrowAnyException()

        ProcessBuilder(binDir.resolve("program.cac.bin").toString())
            .redirectInput(inputPath.toFile())
            .redirectOutput(actualOutputPath.toFile())
            .start()
            .waitFor(10, TimeUnit.SECONDS)

        assertThatPath(actualOutputPath).exists()

        val expectedOutput = expectedOutputPath.toFile().readText()
        val actualOutput = actualOutputPath.toFile().readText()

        assertThat(actualOutput).isEqualTo(expectedOutput)
    }

    companion object {
        @JvmStatic
        fun ioExamples(): List<Path> {
            return Path.of("examples/io").listDirectoryEntries().toList()
        }
    }
}
