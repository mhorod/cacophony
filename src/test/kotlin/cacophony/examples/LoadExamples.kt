package cacophony.examples

import java.nio.file.Path

fun loadExamples(): List<Path> = getPathsMatching(Path.of("examples"), Regex(".*"))

fun loadCorrectExamples(): List<Path> = getPathsMatching(Path.of("examples/correct"), Regex(".*"))
