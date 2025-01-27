package cacophony.semantic.analysis

import cacophony.*
import cacophony.controlflow.Variable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClosureAnalysisKtTest {
    @Test
    fun `lambda expressions are correctly separated into static functions and closures`() {
        /*
         * let f = [x: Int] -> Int => x; # f is a static function
         * let g = ([x: Int] -> Int => x); # g is a closure
         */
        val fArg = intArg("x")
        val fArgVar = primVar()
        val fArgUse = variableUse("x")
        val staticLambda = lambda(listOf(fArg), intType(), fArgUse)
        val fDef = functionDefinition("f", staticLambda)
        val fVar = Variable.FunctionVariable(primVar(), primVar())

        val gArg = intArg("x")
        val gArgVar = primVar()
        val gArgUse = variableUse("x")
        val dynamicLambda = lambda(listOf(gArg), intType(), gArgUse)
        val gDef = variableDeclaration("g", block(dynamicLambda))
        val gVar = Variable.FunctionVariable(primVar(), primVar())

        val ast = block(fDef, gDef)
        val variablesMap =
            VariablesMap(
                mapOf(fArgUse to fArgVar, gArgUse to gArgVar),
                mapOf(fArg to fArgVar, gArg to gArgVar, fDef to fVar, gDef to gVar),
            )

        val result = analyzeClosures(ast, variablesMap, emptySet())

        assertThat(result.closures).containsExactlyInAnyOrder(dynamicLambda)
        assertThat(result.staticFunctions).containsExactlyInAnyOrder(staticLambda)
    }
}
