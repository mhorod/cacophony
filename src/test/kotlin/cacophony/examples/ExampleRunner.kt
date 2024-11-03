package cacophony.examples

import cacophony.utils.FileInput
import cacophony.utils.Input
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

interface ExampleRunner {
    fun run(
        input: Input,
        diagnostics: TestDiagnostics,
    )
}

interface DiagnosticsAssertion {
    fun check(diagnostics: TestDiagnostics)
}

fun runExample(
    path: Path,
    runner: ExampleRunner,
    assertion: DiagnosticsAssertion,
) {
    val diagnostics = TestDiagnostics()
    val input = FileInput(path.toString())

    println("The file im gonna feed the parser with!!!")
    println(input)

    runner.run(input, diagnostics)
    assertion.check(diagnostics)
}

fun runExamples(
    paths: List<Path>,
    runner: ExampleRunner,
    assertion: DiagnosticsAssertion,
) {
    paths.forEach { runExample(it, runner, assertion) }
}

fun getPathsMatching(
    root: Path,
    re: Regex,
): List<Path> =
    Files
        .walk(root)
        .filter { re.matches(it.toString()) }
        .filter { it.isRegularFile() }
        .toList()
