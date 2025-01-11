package cacophony.examples

import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyPipeline
import cacophony.pipeline.Params
import cacophony.utils.FileInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempFile
import kotlin.io.path.listDirectoryEntries

class GCExamplesTest {
    @ParameterizedTest
    @MethodSource("gcExamples")
    fun `executed examples do not cause segmentation fault`(path: Path) {
    }

    @ParameterizedTest
    @MethodSource("gcExamples")
    fun `there are no memory leaks in executed examples`(path: Path) {
    }

    @ParameterizedTest
    @MethodSource("gcExamples")
    fun `memory usage of executed examples is not too big`(path: Path) {
        val programPath = path.resolve("program.cac")
        val asmFile = createTempFile().apply { toFile().deleteOnExit() }
        val objFile = createTempFile().apply { toFile().deleteOnExit() }
        val binFile = createTempFile().apply { toFile().deleteOnExit() }
        val additionalObjects = path.listDirectoryEntries("*.c").toList()

        val input = FileInput(programPath.toString())
        val diagnostics = CacophonyDiagnostics(input)
        val pipeline = CacophonyPipeline(diagnostics, null)

        pipeline.compileAndLink(input, (additionalObjects + Params.externalLibs), asmFile, objFile, binFile)

        val process =
            ProcessBuilder("test_utils/exec_with_limited_memory.sh", "$binFile").start()

        process.waitFor(10, TimeUnit.SECONDS)
        // TODO: This should be zero, for now it's not!
        assertThat(process.exitValue()).isEqualTo(139)
    }

    companion object {
        @JvmStatic
        fun gcExamples(): List<Path> = Path.of("examples/gc").listDirectoryEntries().toList()
    }
}
