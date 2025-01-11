package cacophony.examples

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
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
    }

    companion object {
        @JvmStatic
        fun gcExamples(): List<Path> = Path.of("examples/gc").listDirectoryEntries().toList()
    }
}
