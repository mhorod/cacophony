package cacophony.examples

import cacophony.utils.Input
import cacophony.utils.Location
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

interface Runner {
    fun run(
        input: Input,
        diagnostics: TestDiagnostics,
    )
}

interface DiagnosticsAssertion {
    fun check(diagnostics: TestDiagnostics)
}

class FileInput(path: Path) : Input {
    private val content: String
    private var location: Int = 0

    init {
        content = File(path.toString()).readText()
    }

    override fun next(): Char? {
        return if (location < content.length) {
            content[location]
        } else {
            null
        }
    }

    override fun getLocation(): Location {
        return Location(location)
    }

    override fun setLocation(loc: Location) {
        location = loc.value
    }

    override fun locationToString(loc: Location): String {
        return loc.value.toString()
    }
}

fun runExample(
    path: Path,
    runner: Runner,
    assertion: DiagnosticsAssertion,
) {
    val diagnostics = TestDiagnostics()
    val input = FileInput(path)
    runner.run(input, diagnostics)
    assertion.check(diagnostics)
}

fun runExamples(
    paths: List<Path>,
    runner: Runner,
    assertion: DiagnosticsAssertion,
) {
    paths.forEach { runExample(it, runner, assertion) }
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
