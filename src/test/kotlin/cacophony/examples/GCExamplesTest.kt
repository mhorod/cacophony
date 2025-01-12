package cacophony.examples

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.listDirectoryEntries

class GCExamplesTest {
    @ParameterizedTest
    @MethodSource("gcExamples")
    fun `memory usage of executed examples is not too big`(path: Path) {
        val binFile = createBinary(path, "program.cac")
        val memlimitFile = path.resolve("memlimit.txt")

        val process =
            ProcessBuilder(binFile.toString())
                .start()

        process.waitFor(10, TimeUnit.SECONDS)

        assertThat(process.exitValue())
            .withFailMessage("process terminated by segmentation fault")
            .isNotEqualTo(139)
        assertThat(process.exitValue())
            .withFailMessage("process ended with non-zero exit value ${process.exitValue()}")
            .isZero

        val memoryLimit = File(memlimitFile.toString()).readText().trim().toInt()

        val processWithMemoryLimit =
            ProcessBuilder("test_utils/exec_with_limited_memory.sh", "$memoryLimit", "$binFile").start()

        processWithMemoryLimit.waitFor(10, TimeUnit.SECONDS)

        assertThat(processWithMemoryLimit.exitValue())
            .withFailMessage("process used more than $memoryLimit KB memory limit")
            .isZero
    }

    // WARNING! This might change if total size of statically linked code goes bigger
    companion object {
        @JvmStatic
        fun gcExamples(): List<Path> = Path.of("examples/gc").listDirectoryEntries().toList()
    }
}
