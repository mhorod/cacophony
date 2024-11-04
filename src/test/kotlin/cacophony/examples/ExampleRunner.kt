package cacophony.examples

import cacophony.semantic.syntaxtree.AST
import cacophony.utils.FileInput
import cacophony.utils.Input
import cacophony.utils.TreePrinter
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

data class ExampleResult(
    val AST: AST?,
    val diagnostics: TestDiagnostics,
    val exceptionMessage: String,
) {
    // Although it's not used, it may be helpful during tests debugging.
    override fun toString(): String {
        val treePrinter = TreePrinter(StringBuilder())
        return "ExampleResult(\n" +
            "AST=${if (AST != null) treePrinter.printTree(AST) else ""}, \n" +
            "diagnostics=${diagnostics.errors()}, \n" +
            "exceptionMessage=$exceptionMessage\n" +
            ")"
    }
}

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
