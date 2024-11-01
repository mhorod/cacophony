@file:Suppress("ktlint:standard:no-wildcard-imports")

package cacophony.semantic.syntaxtree

import cacophony.utils.Location
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ExpressionTest {
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
            OperatorBinary.Addition(
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
        val subExpr = Literal.IntLiteral(Pair(locEnd, locEnd), 7)
        var expression = Definition.VariableDeclaration(Pair(locBegin, locEnd), "index", null, subExpr)
        assertNull(expression.type)
        val typeExpression = Type.Basic(Pair(locBegin, locEnd), "Int")
        expression = Definition.VariableDeclaration(Pair(locBegin, locEnd), "index", typeExpression, subExpr)
        assertNotNull(expression.type)
    }
}
