package cacophony.examples

import cacophony.semantic.syntaxtree.AST
import cacophony.utils.FileInput
import cacophony.utils.Input
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

data class ExampleResult(val AST: AST, val diagnostics: TestDiagnostics )

interface ExampleRunner {
    fun run(
        input: Input,
        diagnostics: TestDiagnostics,
    ): ExampleResult
}

fun runExample(
    path: Path,
    runner: ExampleRunner,
): ExampleResult {
    val diagnostics = TestDiagnostics()
    val input = FileInput(path.toString())
    return runner.run(input, diagnostics)
}

fun runExamples(
    paths: List<Path>,
    runner: ExampleRunner,
) {
    paths.forEach { runExample(it, runner) }
}

fun getPathsMatching(
    root: Path,
    re: Regex,
): List<Path> {
    return Files
        .walk(root)
        .filter { re.matches(it.toString()) }
        .filter { it.isRegularFile() }
        .toList()
}
