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
import cacophony.controlflow.mod
import cacophony.controlflow.mul
import cacophony.controlflow.neq
import cacophony.controlflow.sub
import cacophony.semantic.syntaxtree.Expression

typealias MakeBinaryExpression = (Expression, Expression) -> Expression
typealias MakeBinaryNode = (CFGNode, CFGNode) -> CFGNode

class TestOperators {
    companion object {
        val add: MakeBinaryExpression = { lhs, rhs -> lhs add rhs }
        val addNode: MakeBinaryNode = { lhs, rhs -> lhs add rhs }

        val sub: MakeBinaryExpression = { lhs, rhs -> lhs sub rhs }
        val subNode: MakeBinaryNode = { lhs, rhs -> lhs sub rhs }

        val mul: MakeBinaryExpression = { lhs, rhs -> lhs mul rhs }
        val mulNode: MakeBinaryNode = { lhs, rhs -> lhs mul rhs }

        val div: MakeBinaryExpression = { lhs, rhs -> lhs div rhs }
        val divNode: MakeBinaryNode = { lhs, rhs -> lhs div rhs }

        val mod: MakeBinaryExpression = { lhs, rhs -> lhs mod rhs }
        val modNode: MakeBinaryNode = { lhs, rhs -> lhs mod rhs }

        val eq: MakeBinaryExpression = { lhs, rhs -> lhs eq rhs }
        val eqNode: MakeBinaryNode = { lhs, rhs -> lhs eq rhs }

        val neq: MakeBinaryExpression = { lhs, rhs -> lhs neq rhs }
        val neqNode: MakeBinaryNode = { lhs, rhs -> lhs neq rhs }

        val lt: MakeBinaryExpression = { lhs, rhs -> lhs lt rhs }
        val ltNode: MakeBinaryNode = { lhs, rhs -> lhs lt rhs }

        val leq: MakeBinaryExpression = { lhs, rhs -> lhs leq rhs }
        val leqNode: MakeBinaryNode = { lhs, rhs -> lhs leq rhs }

        val gt: MakeBinaryExpression = { lhs, rhs -> lhs gt rhs }
        val gtNode: MakeBinaryNode = { lhs, rhs -> lhs gt rhs }

        val geq: MakeBinaryExpression = { lhs, rhs -> lhs geq rhs }
        val geqNode: MakeBinaryNode = { lhs, rhs -> lhs geq rhs }
    }
}
