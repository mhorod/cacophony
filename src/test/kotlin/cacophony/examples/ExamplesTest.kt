package cacophony.examples

import cacophony.diagnostics.CacophonyDiagnostics
import cacophony.pipeline.CacophonyPipeline
import cacophony.semantic.analysis.FunctionAnalysisResult
import cacophony.semantic.names.ResolvedVariables
import cacophony.semantic.syntaxtree.AST
import cacophony.semantic.types.TypeCheckingResult
import cacophony.utils.*
import com.karumi.kotlinsnapshot.matchWithSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

class ExamplesTest {
    data class TestResult(
        var ast: AST? = null,
        var resolvedVariables: ResolvedVariables? = null,
        var types: TypeCheckingResult? = null,
        var analysisResult: FunctionAnalysisResult? = null,
        var errors: List<CacophonyDiagnostics.ReportedError>? = null,
    ) {
        fun checkSnapshots(path: String) {
            ast.matchWithSnapshot("$path.ast")
            (resolvedVariables?.map { (k, v) -> k.range to v.range }?.toMap()).matchWithSnapshot("$path.resolved")
            (types?.expressionTypes?.mapKeys { (k, _) -> k.range }).matchWithSnapshot("$path.types")
            (analysisResult?.mapKeys { (k, _) -> k.range }).matchWithSnapshot("$path.analysis")
            errors.matchWithSnapshot("$path.errors")
        }
    }

    private fun runTest(input: Input): TestResult {
        val diagnostics = CacophonyDiagnostics(input)
        val pipeline = CacophonyPipeline(diagnostics)

        val result = TestResult()

        try {
            result.ast = pipeline.generateAst(input)
            val nameResolutionResult = pipeline.resolveNames(result.ast!!)
            result.resolvedVariables = pipeline.resolveOverloads(result.ast!!, nameResolutionResult)
            result.types = pipeline.checkTypes(result.ast!!, result.resolvedVariables!!)
            val callGraph = pipeline.generateCallGraph(result.ast!!, result.resolvedVariables!!)
            val variablesMap = pipeline.createVariables(result.ast!!, result.resolvedVariables!!, result.types!!)
            result.analysisResult = pipeline.analyzeFunctions(result.ast!!, variablesMap, result.resolvedVariables!!, callGraph)
        } catch (_: CompileException) {
        } finally {
            result.errors = diagnostics.extractErrors()
        }

        return result
    }

    @ParameterizedTest
    @MethodSource("examples")
    fun `examples compilation did not change`(path: Path) {
        val prefix: String = if (path.startsWith("examples/correct")) "correct" else "incorrect"
        val input = FileInput(path.toString())

        val result = runTest(input)
        result.checkSnapshots("$prefix/${path.fileName}")
    }

    @ParameterizedTest
    @MethodSource("correctExamples")
    fun `correct examples compile without errors`(path: Path) {
        val input = FileInput(path.toString())
        val diagnostics = CacophonyDiagnostics(input)
        CacophonyPipeline(diagnostics).process(input)
        diagnostics.getErrors().forEach {
            println(it)
        }
        assertThat(diagnostics.getErrors()).isEmpty()
    }

    @ParameterizedTest
    @MethodSource("incorrectExamples")
    fun `incorrect examples give a compile error`(path: Path) {
        val input = FileInput(path.toString())
        val diagnostics = CacophonyDiagnostics(input)
        try {
            CacophonyPipeline(diagnostics).process(input)
        } catch (_: CompileException) {
        }
        assertThat(diagnostics.getErrors()).isNotEmpty()
    }

    companion object {
        @JvmStatic
        fun examples() = loadExamples()

        @JvmStatic
        fun correctExamples() = loadCorrectExamples()

        @JvmStatic
        fun incorrectExamples() = loadIncorrectExamples()
    }
}
