package cacophony.examples

import java.nio.file.Path

fun loadCorrectExamples(): List<Path> {
    return getPathsMatching(Path.of("examples/correct"), Regex(".*"))
}

fun loadIncorrectExamples(): List<IncorrectExampleDescription> {
    return getPathsMatching(
        Path.of("examples/incorrect"),
        Regex(".*descriptions.json"),
    ).flatMap { IncorrectExampleDescriptions.loadFromFile(it).examples }
}
