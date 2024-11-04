package cacophony.examples

import java.nio.file.Path

fun loadExamples(): List<Path> {
    return getPathsMatching(Path.of("examples"), Regex(".*"))
}
