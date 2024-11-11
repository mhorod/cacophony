package cacophony.examples

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

fun loadExamples(): List<Path> = loadCorrectExamples() + loadIncorrectExamples()

fun loadCorrectExamples(): List<Path> = getPathsMatching(Path.of("examples/correct"), Regex(".*"))

fun loadIncorrectExamples(): List<Path> = getPathsMatching(Path.of("examples/incorrect"), Regex(".*"))

fun getPathsMatching(
    root: Path,
    re: Regex,
): List<Path> =
    Files
        .walk(root)
        .filter { re.matches(it.toString()) }
        .filter { it.isRegularFile() }
        .toList()
