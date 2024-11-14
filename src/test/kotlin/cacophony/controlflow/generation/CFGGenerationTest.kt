package cacophony.controlflow.generation

import cacophony.block
import cacophony.functionDeclaration
import cacophony.semantic.ResolvedVariables
import cacophony.semantic.UseTypeAnalysisResult
import cacophony.semantic.VariableUseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression
import org.junit.jupiter.api.Test

class CFGGenerationTest {
    @Test
    fun `CFG of empty function`() {
        // given
        val emptyBlock = block()
        val fDef = functionDeclaration("f", emptyBlock)

        val resolvedVariables: ResolvedVariables = emptyMap()
        val useTypeMap: UseTypeAnalysisResult = emptyMap<Expression, Map<Definition, VariableUseType>>().withDefault { emptyMap() }
        val handlers = mapOf(fDef to TestFunctionHandler())

        // when
        val cfg = generateCFG(resolvedVariables, useTypeMap, handlers)

        // then
    }
}
