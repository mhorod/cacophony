package cacophony.grammar.syntaxtree

import cacophony.utils.Location
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ExpressionTest {
    @Test
    fun `Expressions have range`() {
        val locBegin = Location(1)
        val locEnd = Location(3)
        val expression = Expression.Literal(Pair(locBegin, locEnd))
        assertEquals(locBegin, expression.range.first)
        assertEquals(locEnd, expression.range.second)
    }

    @Test
    fun `Nested Expressions`() {
        val expression1 = Expression.Literal(Pair(Location(1), Location(2)))
        val expression2 = Expression.Literal(Pair(Location(3), Location(4)))
        val additionExpression =
            Expression.Operator.BinaryOperator.AdditionOperator(
                Pair(Location(1), Location(4)),
                expression1,
                expression2,
            )
        assertEquals(expression1, additionExpression.lhs)
        assertEquals(expression2, additionExpression.rhs)
    }
}
