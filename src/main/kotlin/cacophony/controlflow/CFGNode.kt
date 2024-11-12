package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

class CFGLabel

/**
 * Single computation tree that has no control-flow or data-flow dependencies
 */
sealed interface CFGNode {
    sealed interface Unconditional : CFGNode

    sealed interface Leaf : CFGNode

    sealed interface LValue : Unconditional

    data object NoOp : Unconditional, Leaf {
        override fun toString(): String = "nop"
    }

    data object Return :
        Unconditional,
        Leaf

    data class Call(
        val declaration: Definition.FunctionDeclaration,
    ) : Unconditional,
        Leaf {
        override fun toString(): String = "call ${declaration.identifier}"
    }

    // NOTE: Push may be unnecessary since it can be done via Assignment + MemoryAccess
    data class Push(
        val value: CFGNode,
    ) : Unconditional

    // NOTE: Pop may be unnecessary since it can be done via Assignment
    data class Pop(
        val regvar: Register,
    ) : Unconditional,
        Leaf

    data class Assignment(
        val destination: LValue,
        val value: CFGNode,
    ) : Unconditional {
        override fun toString(): String = "($destination = $value)"
    }

    data class VariableUse(
        val regvar: Register,
    ) : LValue,
        Leaf {
        @OptIn(ExperimentalStdlibApi::class)
        override fun toString(): String =
            when (regvar) {
                is Register.FixedRegister -> regvar.hardwareRegister.toString()
                is Register.VirtualRegister -> "VReg(${regvar.hashCode().toHexString()})"
            }
    }

    data class MemoryAccess(
        val destination: CFGNode,
    ) : LValue

    data class Constant(
        val value: Int,
    ) : Unconditional,
        Leaf {
        override fun toString(): String = value.toString()
    }

    data class Sequence(
        val nodes: List<CFGNode>,
    ) : Unconditional {
        override fun toString(): String = nodes.joinToString("; ") { it.toString() }
    }

    sealed interface ArithmeticOperator : Unconditional

    sealed interface ArithmeticAssignmentOperator : ArithmeticOperator

    data class Addition(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs + $rhs)"
    }

    data class AdditionAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs += $rhs)"
    }

    data class Subtraction(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs - $rhs)"
    }

    data class SubtractionAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs -= $rhs)"
    }

    data class Multiplication(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs * $rhs)"
    }

    data class MultiplicationAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs *= $rhs)"
    }

    data class Division(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs / $rhs)"
    }

    data class DivisionAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs /= $rhs)"
    }

    data class Modulo(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs % $rhs)"
    }

    data class ModuloAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs %= $rhs)"
    }

    data class Minus(val value: CFGNode) : ArithmeticOperator {
        override fun toString(): String = "(-$value)"
    }

    sealed interface LogicalOperator : CFGNode, Unconditional

    data class LogicalNot(
        val value: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "(~$value)"
    }

    data class Equals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs == $rhs)"
    }

    data class NotEquals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs != $rhs)"
    }

    data class Less(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs < $rhs)"
    }

    data class Greater(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs > $rhs)"
    }

    data class LessEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs <= $rhs)"
    }

    data class GreaterEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs >= $rhs)"
    }

    companion object {
        val UNIT = Constant(42)
        val FALSE = Constant(0)
        val TRUE = Constant(1)
    }
}
