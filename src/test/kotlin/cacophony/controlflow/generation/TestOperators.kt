package cacophony.controlflow.generation

import cacophony.*
import cacophony.controlflow.CFGNode
import cacophony.controlflow.add
import cacophony.controlflow.div
import cacophony.controlflow.eq
import cacophony.controlflow.geq
import cacophony.controlflow.gt
import cacophony.controlflow.leq
import cacophony.controlflow.lt
import cacophony.controlflow.minus
import cacophony.controlflow.mod
import cacophony.controlflow.mul
import cacophony.controlflow.neq
import cacophony.controlflow.not
import cacophony.controlflow.sub
import cacophony.semantic.syntaxtree.Assignable
import cacophony.semantic.syntaxtree.Expression
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.argumentSet

typealias MakeUnaryExpression = (Expression) -> Expression
typealias MakeUnaryNode = (CFGNode) -> CFGNode
typealias MakeBinaryExpression = (Expression, Expression) -> Expression
typealias MakeBinaryNode = (CFGNode, CFGNode) -> CFGNode
typealias MakeAssignmentExpression = (Assignable, Expression) -> Expression

class TestOperators {
    companion object {
        val minus: MakeUnaryExpression = { child -> cacophony.minus(child) }
        private val minusNode: MakeUnaryNode = { child -> minus(child) }

        val not: MakeUnaryExpression = { child -> lnot(child) }
        private val notNode: MakeUnaryNode = { child -> not(child) }

        val add: MakeBinaryExpression = { lhs, rhs -> lhs add rhs }
        private val addeq: MakeAssignmentExpression = { lhs, rhs -> lhs addeq rhs }
        private val addNode: MakeBinaryNode = { lhs, rhs -> lhs add rhs }

        private val sub: MakeBinaryExpression = { lhs, rhs -> lhs sub rhs }
        private val subeq: MakeAssignmentExpression = { lhs, rhs -> lhs subeq rhs }
        private val subNode: MakeBinaryNode = { lhs, rhs -> lhs sub rhs }

        private val mul: MakeBinaryExpression = { lhs, rhs -> lhs mul rhs }
        private val muleq: MakeAssignmentExpression = { lhs, rhs -> lhs muleq rhs }
        private val mulNode: MakeBinaryNode = { lhs, rhs -> lhs mul rhs }

        val div: MakeBinaryExpression = { lhs, rhs -> lhs div rhs }
        private val diveq: MakeAssignmentExpression = { lhs, rhs -> lhs diveq rhs }
        private val divNode: MakeBinaryNode = { lhs, rhs -> lhs div rhs }

        private val mod: MakeBinaryExpression = { lhs, rhs -> lhs mod rhs }
        private val modeq: MakeAssignmentExpression = { lhs, rhs -> lhs modeq rhs }
        private val modNode: MakeBinaryNode = { lhs, rhs -> lhs mod rhs }

        private val eq: MakeBinaryExpression = { lhs, rhs -> lhs eq rhs }
        private val eqNode: MakeBinaryNode = { lhs, rhs -> lhs eq rhs }

        private val neq: MakeBinaryExpression = { lhs, rhs -> lhs neq rhs }
        private val neqNode: MakeBinaryNode = { lhs, rhs -> lhs neq rhs }

        private val lt: MakeBinaryExpression = { lhs, rhs -> lhs lt rhs }
        private val ltNode: MakeBinaryNode = { lhs, rhs -> lhs lt rhs }

        private val leq: MakeBinaryExpression = { lhs, rhs -> lhs leq rhs }
        private val leqNode: MakeBinaryNode = { lhs, rhs -> lhs leq rhs }

        private val gt: MakeBinaryExpression = { lhs, rhs -> lhs gt rhs }
        private val gtNode: MakeBinaryNode = { lhs, rhs -> lhs gt rhs }

        private val geq: MakeBinaryExpression = { lhs, rhs -> lhs geq rhs }
        private val geqNode: MakeBinaryNode = { lhs, rhs -> lhs geq rhs }

        fun unaryExpressions(): List<Arguments> =
            listOf(
                argumentSet("minus", minus, minusNode),
                argumentSet("not", not, notNode),
            )

        fun binaryExpressions(): List<Arguments> = arithmeticOperators() + logicalOperators()

        fun arithmeticOperators(): List<Arguments> =
            listOf(
                argumentSet("add", add, addNode),
                argumentSet("sub", sub, subNode),
                argumentSet("mul", mul, mulNode),
                argumentSet("div", div, divNode),
                argumentSet("mod", mod, modNode),
            )

        fun arithmeticAssignmentOperators(): List<Arguments> =
            listOf(
                argumentSet("addeq", addeq),
                argumentSet("subeq", subeq),
                argumentSet("muleq", muleq),
                argumentSet("diveq", diveq),
                argumentSet("modeq", modeq),
            )

        fun logicalOperators(): List<Arguments> =
            listOf(
                argumentSet("eq", eq, eqNode),
                argumentSet("neq", neq, neqNode),
                argumentSet("lt", lt, ltNode),
                argumentSet("leq", leq, leqNode),
                argumentSet("gt", gt, gtNode),
                argumentSet("geq", geq, geqNode),
            )
    }
}
