package cacophony.examples

import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyLogger
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.FileInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatPath
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

class IOExamplesTest {
    @ParameterizedTest
    @MethodSource("ioExamples")
    fun `io tests print expected output on given input`(path: Path) {
        val inputPath = path.resolve("input.txt")
        val expectedOutputPath = path.resolve("output.txt")
        val actualOutputPath = createTempFile().apply { toFile().deleteOnExit() }

        val programPath = path.resolve("program.cac")
        val asmFile = createTempFile().apply { toFile().deleteOnExit() }
        val objFile = createTempFile().apply { toFile().deleteOnExit() }
        val binFile = createTempFile().apply { toFile().deleteOnExit() }

        val input = FileInput(programPath.toString())
        val diagnostics = CacophonyDiagnostics(input)
        val pipeline = CacophonyPipeline(diagnostics, CacophonyLogger())

        assertThatCode { pipeline.compile(input, asmFile, objFile, binFile) }.doesNotThrowAnyException()

        val process =
            ProcessBuilder(binFile.toString())
                .redirectInput(inputPath.toFile())
                .redirectOutput(actualOutputPath.toFile())
                .start()

        process.waitFor(10, TimeUnit.SECONDS)
        assertThat(process.exitValue()).isZero()

        assertThatPath(actualOutputPath).exists()

        val expectedOutput = expectedOutputPath.readText()
        val actualOutput = actualOutputPath.readText()

        assertThat(actualOutput).isEqualTo(expectedOutput)
    }

    companion object {
        @JvmStatic
        fun ioExamples(): List<Path> {
            return Path.of("examples/io").listDirectoryEntries().toList()
        }
    }
}
