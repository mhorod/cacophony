package cacophony.examples

import cacophony.testPipeline
import cacophony.utils.FileInput
import cacophony.utils.TreePrinter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries

// TODO - Once we finish implementing lambdas move the tests to correct/io examples and remove this class
class LambdaExpressionsTest {
    @ParameterizedTest
    @MethodSource("examples")
    fun `parse lambda expression`(path: Path) {
        val pipeline = testPipeline()
        val input = FileInput(path.toString())
        try {
            val ast = pipeline.generateAst(input)
            println(TreePrinter(StringBuilder()).printTree(ast))
        } catch (e: Exception) {
            println(e)
            println(pipeline.diagnostics.getErrors())
        }
        assert(pipeline.diagnostics.getErrors().isEmpty())
    }

    companion object {
        @JvmStatic
        fun examples(): List<Path> = Path.of("examples/not_yet_correct/lambdas").listDirectoryEntries().toList()
    }
}
