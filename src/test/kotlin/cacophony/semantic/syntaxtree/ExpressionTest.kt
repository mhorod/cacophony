package cacophony.semantic.syntaxtree

import cacophony.semantic.syntaxtree.OperatorBinary.*
import cacophony.utils.Location
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private typealias BinaryOperatorConstructor = (Pair<Location, Location>, Expression, Expression) -> Expression

internal class ExpressionTest {
    companion object {
        val binaryOperatorConstructors =
            listOf(
                ::Multiplication,
                ::Division,
                ::Modulo,
                ::Addition,
                ::Subtraction,
                ::Less,
                ::Greater,
                ::LessEqual,
                ::GreaterEqual,
                ::Equals,
                ::NotEquals,
                ::LogicalAnd,
                ::LogicalOr,
                ::Assignment,
                ::AdditionAssignment,
                ::SubtractionAssignment,
                ::MultiplicationAssignment,
                ::DivisionAssignment,
                ::ModuloAssignment,
            )
    }

    @Test
    fun `Access to expression fields`() {
        val locBegin = Location(1)
        val locEnd = Location(3)
        val name = "index"
        val expression = VariableUse(Pair(locBegin, locEnd), "index")
        assertEquals(name, expression.identifier)
    }

    @Test
    fun `Expressions have range`() {
        val locBegin = Location(1)
        val locEnd = Location(5)
        val expression = Literal.BoolLiteral(Pair(locBegin, locEnd), false)
        assertEquals(locBegin, expression.range.first)
        assertEquals(locEnd, expression.range.second)
    }

    @Test
    fun `Nested Expressions`() {
        val expression1 = Literal.IntLiteral(Pair(Location(1), Location(2)), 4)
        val expression2 = Literal.IntLiteral(Pair(Location(3), Location(4)), 2)
        val additionExpression =
            Addition(
                Pair(Location(1), Location(4)),
                expression1,
                expression2,
            )
        assertEquals(expression1, additionExpression.lhs)
        assertEquals(expression2, additionExpression.rhs)
    }

    @Test
    fun `Nullable fields`() {
        val locBegin = Location(1)
        val locEnd = Location(3)
        var expression = Definition.VariableDeclaration(Pair(locBegin, locEnd), "index", null, Empty(locBegin to locEnd))
        assertNull(expression.type)
        val typeExpression = Type.Basic(Pair(locBegin, locEnd), "Int")
        expression = Definition.VariableDeclaration(Pair(locBegin, locEnd), "index", typeExpression, Empty(locBegin to locEnd))
        assertNotNull(expression.type)
    }

    @Test
    fun `binary operators are correctly compared for equivalence`() {
        binaryOperatorConstructors.forEach { constructor ->
            `assert that binary operator expressions are not equivalent when they are in different ranges`(constructor)
        }

        binaryOperatorConstructors.forEach { constructor ->
            `assert that binary operator expressions are not equivalent when they have different arguments`(constructor)
        }

        binaryOperatorConstructors.forEach { constructorA ->
            binaryOperatorConstructors.forEach { constructorB ->
                `assert that binary operator expressions must be the same operator to be equivalent`(
                    constructorA,
                    constructorB,
                )
            }
        }
    }

    private fun `assert that binary operator expressions are not equivalent when they are in different ranges`(
        constructor: BinaryOperatorConstructor,
    ) {
        // operation boundaries
        val locOne = Location(1)
        val locThree = Location(3)
        val locFive = Location(5)

        val rangeHere = Pair(locOne, locThree)
        val lhsHere = Literal.IntLiteral(Pair(locOne, locOne), 1)
        val rhsHere = Literal.IntLiteral(Pair(locThree, locThree), 2)

        val rangeThere = Pair(locThree, locFive)
        val lhsThere = Literal.IntLiteral(Pair(locThree, locThree), 1)
        val rhsThere = Literal.IntLiteral(Pair(locFive, locFive), 2)

        val operationHere = constructor(rangeHere, lhsHere, rhsHere)
        val operationThere = constructor(rangeThere, lhsThere, rhsThere)

        assertFalse(areEquivalentExpressions(operationHere, operationThere))
    }

    private fun `assert that binary operator expressions are not equivalent when they have different arguments`(
        constructor: BinaryOperatorConstructor,
    ) {
        val locOne = Location(1)
        val locThree = Location(3)

        val commonLhsRange = Pair(locOne, locOne)
        val commonRhsRange = Pair(locThree, locThree)
        val commonFullRange = Pair(locOne, locThree)

        val lhsA = Literal.IntLiteral(commonLhsRange, 1)
        val rhsA = Literal.IntLiteral(commonRhsRange, 2)

        val lhsB = Literal.IntLiteral(commonLhsRange, 2)
        val rhsB = Literal.IntLiteral(commonRhsRange, 3)

        val operationA = constructor(commonFullRange, lhsA, rhsA)
        val operationB = constructor(commonFullRange, lhsB, rhsB)

        assertFalse(areEquivalentExpressions(operationA, operationB))
    }

    private fun `assert that binary operator expressions must be the same operator to be equivalent`(
        constructorA: BinaryOperatorConstructor,
        constructorB: BinaryOperatorConstructor,
    ) {
        val locOne = Location(1)
        val locThree = Location(3)

        val commonLhsRange = Pair(locOne, locOne)
        val commonRhsRange = Pair(locThree, locThree)
        val commonFullRange = Pair(locOne, locThree)

        val commonLhs = Literal.IntLiteral(commonLhsRange, 1)
        val commonRhs = Literal.IntLiteral(commonRhsRange, 2)

        val operationA = constructorA(commonFullRange, commonLhs, commonRhs)
        val operationB = constructorB(commonFullRange, commonLhs, commonRhs)

        assertEquals(constructorA === constructorB, areEquivalentExpressions(operationA, operationB))
    }
}
