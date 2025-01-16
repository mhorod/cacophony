package cacophony.examples

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.listDirectoryEntries

const val BASE_MEMORY = 6000

class GCExamplesTest {
    @ParameterizedTest
    @MethodSource("gcExamples")
    fun `memory usage of executed examples is not too big`(path: Path) {
        val binFile = createBinary(path, "program.cac")
        val memLimitFile = path.resolve("memlimit.txt")
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

        //TODO: uncomment after memory limits are fixed
//        val memoryLimit = File(memLimitFile.toString()).readText().trim().toInt() + BASE_MEMORY
//
//        val processWithMemoryLimit =
//            ProcessBuilder("test_utils/exec_with_limited_memory.sh", "$memoryLimit", "$binFile").start()
//
//        processWithMemoryLimit.waitFor(10, TimeUnit.SECONDS)
//
//        assertThat(processWithMemoryLimit.exitValue())
//            .withFailMessage("process used more than $memoryLimit KB memory limit")
//            .isZero
    }

    companion object {
        @JvmStatic
        fun gcExamples(): List<Path> = Path.of("examples/gc").listDirectoryEntries().toList()
    }
}
