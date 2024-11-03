package cacophony.parser

import cacophony.examples.AssertNoErrors
import cacophony.examples.ExampleRunner
import cacophony.examples.TestDiagnostics
import cacophony.examples.loadCorrectExamples
import cacophony.examples.loadIncorrectExamples
import cacophony.examples.runExample
import cacophony.grammars.AnalyzedGrammar
import cacophony.grammars.ParseTree
import cacophony.lexer.CacophonyLexer
import cacophony.utils.Diagnostics
import cacophony.utils.Input
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path

class CacophonyParserExamplesTest {
    class CacophonyParser {
        private val lexer = CacophonyLexer()
        private val analyzedGrammar: AnalyzedGrammar<Int, CacophonyGrammarSymbol> =
            AnalyzedGrammar.fromGrammar(
                emptyList(),
                CacophonyGrammar.grammar,
            )
        private val parser = LLOneParser.fromAnalyzedGrammar(analyzedGrammar)

        fun process(
            input: Input,
            diagnostics: Diagnostics,
        ): ParseTree<CacophonyGrammarSymbol> {
            val tokens = lexer.process(input, diagnostics)
            val terminals = tokens.map { token -> ParseTree.Leaf(CacophonyGrammarSymbol.fromLexerToken(token)) }
            return parser.process(terminals, diagnostics)
        }
    }

    class CacophonyParserExampleRunner : ExampleRunner {
        private val parser = CacophonyParser()

        override fun run(
            input: Input,
            diagnostics: TestDiagnostics,
        ) {
            parser.process(input, diagnostics)
        }
    }

    @ParameterizedTest
    @MethodSource("correctExamples")
    fun `parser parses correct examples without errors`(path: Path) {
        println("Im here in the test")
        runExample(
            path,
            parserRunner,
            AssertNoErrors(),
        )
    }

//    TODO
//    @ParameterizedTest
//    @MethodSource("incorrectExamples")
//    fun `parser parses incorrect examples with described errors`(description: IncorrectExampleDescription) {
//        runExample(
//            Path.of(description.path),
//            parserRunner,
//            assertionFromDescription(description),
//        )
//    }

    companion object {
        @JvmStatic
        fun correctExamples() = loadCorrectExamples()

        @JvmStatic
        fun incorrectExamples() = loadIncorrectExamples()

        private val parserRunner = CacophonyParserExampleRunner()
    }
}
