package cacophony.grammar.syntaxtree

import cacophony.utils.Location
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ExpressionTest {
    @Test
    fun `Access to expression fields`() {
        val locBegin = Location(1)
        val locEnd = Location(3)
        val name = "index"
        val expression = Expression.VariableUse(Pair(locBegin, locEnd), "index")
        assertEquals(name, expression.identifier)
    }

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
            Expression.Operator.Binary.Addition(
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
        var expression = Expression.Definition.VariableDeclaration(Pair(locBegin, locEnd), "index", null)
        assertNull(expression.type)
        val typeExpression = Expression.Type.Basic(Pair(locBegin, locEnd), "Int")
        expression = Expression.Definition.VariableDeclaration(Pair(locBegin, locEnd), "index", typeExpression)
        assertNotNull(expression.type)
    }
}
