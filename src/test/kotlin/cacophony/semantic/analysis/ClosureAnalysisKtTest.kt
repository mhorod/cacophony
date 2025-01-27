package cacophony.semantic.analysis

import cacophony.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClosureAnalysisKtTest {
    @Test
    fun `simple functional expressions are correctly separated into static functions and closures`() {
        /*
         * let f = [x: Int] -> Int => x; # f is a static function
         * let g = ([x: Int] -> Int => x); # g is a closure
         */
        val fArg = intArg("x")
        val fArgVar = primVar()
        val fArgUse = variableUse("x")
        val staticLambda = lambda(listOf(fArg), intType(), fArgUse)
        val fDef = functionDefinition("f", staticLambda)
        val fVar = funVar()

        val gArg = intArg("x")
        val gArgVar = primVar()
        val gArgUse = variableUse("x")
        val dynamicLambda = lambda(listOf(gArg), intType(), gArgUse)
        val gDef = variableDeclaration("g", block(dynamicLambda))
        val gVar = funVar()

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

    @Test
    fun `escaping static functions are promoted to closures`() {
        /*
         * let f = [] -> [] -> Int => (
         *     let x = 42;
         *     let g = [] -> Int => x;
         *     g
         * );
         * # g has escaped
         */
        val xDef = variableDeclaration("x", lit(42))
        val xVar = primVar()
        val xUse = variableUse("x")

        val gLambda = lambda(emptyList(), intType(), xUse)
        val gDef = functionDefinition("g", gLambda)
        val gVar = funVar()
        val gUse = variableUse("g")

        val fBody = block(xDef, gDef, gUse)
        val fLambda = lambda(emptyList(), functionalType(emptyList(), intType()), fBody)
        val fDef = functionDefinition("f", fLambda)
        val fVar = funVar()

        val ast = block(fDef)

        val variablesMap =
            VariablesMap(
                mapOf(xUse to xVar, gUse to gVar),
                mapOf(xDef to xVar, gDef to gVar, fDef to fVar),
            )
        val escaping = setOf(xVar, gVar)

        val result = analyzeClosures(ast, variablesMap, escaping)

        assertThat(result.closures).containsExactlyInAnyOrder(gLambda)
        assertThat(result.staticFunctions).containsExactlyInAnyOrder(fLambda)
    }

    @Test
    fun `standalone lambda expressions are identified as closures`() {
        // ([x: Int] -> Int => x + 1)[1];
        val xDef = intArg("x")
        val xUse = variableUse("x")
        val xVar = primVar()

        val lambda = lambda(listOf(xDef), intType(), xUse add lit(1))
        val ast = block(call(lambda, lit(1)))
        val variablesMap = VariablesMap(mapOf(xUse to xVar), mapOf(xDef to xVar))

        val result = analyzeClosures(ast, variablesMap, emptySet())

        assertThat(result.closures).containsExactlyInAnyOrder(lambda)
        assertThat(result.staticFunctions).isEmpty()
    }
}
