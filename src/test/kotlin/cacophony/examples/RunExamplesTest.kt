package cacophony.examples;

import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyPipeline
import cacophony.utils.FileInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.condition.DisabledIf
import kotlin.jvm.JvmStatic;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.readText

class RunExamplesTest {
    fun runBinary(binary: Path, inputFile: Path): Pair<Int, String> {
        val outputFile = createTempFile().toFile().apply { deleteOnExit() }
        val returnCode = ProcessBuilder(binary.toString())
            .redirectInput(inputFile.toFile())
            .redirectOutput(outputFile)
            .start()
            .waitFor()
        return Pair(returnCode, outputFile.readText())
    }

    @ParameterizedTest
    @MethodSource("outputs")
    fun `run examples`(outputPath: Path) {
        val testName = outputPath.name.removeSuffix(".output")
        val inputPath = Paths.get("${outputPath.parent}/${testName}.input")
        val path = Paths.get("${outputPath.parent.parent}/${testName}.cac")

        val fileInput = FileInput(path.toString())
        val diagnostics = CacophonyDiagnostics(fileInput)
        val pipeline = CacophonyPipeline(diagnostics)

        val asmFile = createTempFile().apply { toFile().deleteOnExit() }
        val objFile = createTempFile().apply { toFile().deleteOnExit() }
        val binFile = createTempFile().apply { toFile().deleteOnExit() }

        pipeline.compile(fileInput, asmFile, objFile, binFile)
        val actual = runBinary(binFile, inputPath).run { "Return code: $first\n$second" }
        val expected = outputPath.readText()

        assertThat(actual).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        fun outputs() = loadOutputs().filterNot { it.endsWith("ffi-read-write.output") }
    }
}
