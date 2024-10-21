package cacophony.examples

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

@Serializable
data class IncorrectExampleDescription(val path: String, val errors: Errors) {
    @Serializable
    data class Errors(val lexerErrors: Boolean)

    fun withRootPath(root: Path): IncorrectExampleDescription {
        return IncorrectExampleDescription(root.resolve(path).toString(), errors)
    }
}

@Serializable
data class IncorrectExampleDescriptions(val examples: List<IncorrectExampleDescription>) {
    fun withRootPath(root: Path): IncorrectExampleDescriptions {
        return IncorrectExampleDescriptions(examples.map { it.withRootPath(root) })
    }

    companion object {
        fun loadFromFile(path: Path): IncorrectExampleDescriptions {
            val content = Files.readAllBytes(path)
            val descriptions: IncorrectExampleDescriptions = Json.decodeFromString(content.decodeToString())
            return descriptions.withRootPath(path.parent)
        }
    }
}
