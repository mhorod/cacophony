package cacophony.examples

import org.assertj.core.api.Assertions.assertThat
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

        val binFile = createBinary(path, "program.cac")

        val process =
            ProcessBuilder(binFile.toString())
                .redirectInput(inputPath.toFile())
                .redirectOutput(actualOutputPath.toFile())
                .start()

        process.waitFor(10, TimeUnit.SECONDS)
        assertThat(process.exitValue()).isZero()

        assertThatPath(actualOutputPath).exists()

        val expectedOutput = expectedOutputPath.readText().filterNot { it == '\r' }
        val actualOutput = actualOutputPath.readText().filterNot { it == '\r' }

        assertThat(actualOutput).isEqualTo(expectedOutput)
    }

    companion object {
        @JvmStatic
        fun ioExamples(): List<Path> = Path.of("examples/io").listDirectoryEntries().toList()
    }
}
