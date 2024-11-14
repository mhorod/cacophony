package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

class CFGLabel

sealed interface SlotLabel

class RegisterLabel : SlotLabel

class ValueLabel : SlotLabel

class ConstantLabel : SlotLabel

/**
 * Single computation tree that has no control-flow or data-flow dependencies
 */
sealed interface CFGNode {
    sealed interface Value : CFGNode

    sealed interface Leaf : CFGNode

    sealed interface LValue : CFGNode

    data object NoOp : Leaf {
        override fun toString(): String = "nop"
    }

    data object Return :
        Leaf

    data class Call(
        val declaration: Definition.FunctionDeclaration,
    ) :
        Leaf {
        override fun toString(): String = "call ${declaration.identifier}"
    }

    // NOTE: Push may be unnecessary since it can be done via Assignment + MemoryAccess
    data class Push(
        val value: CFGNode,
    ) : CFGNode

    // NOTE: Pop may be unnecessary since it can be done via Assignment
    data class Pop(
        val regvar: Register,
    ) :
        Leaf

    data class Assignment(
        val destination: LValue,
        val value: CFGNode,
    ) : CFGNode {
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
    ) : CFGNode,
        Leaf {
        override fun toString(): String = value.toString()
    }

    data class Sequence(
        val nodes: List<CFGNode>,
    ) : CFGNode {
        override fun toString(): String = nodes.joinToString("; ") { it.toString() }
    }

    sealed interface ArithmeticOperator : CFGNode

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

    sealed interface LogicalOperator : CFGNode

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

    // TODO: document
    sealed interface Slot : CFGNode {
        val label: SlotLabel
    }

    data class RegisterSlot(
        override val label: RegisterLabel,
//        val register: Register,
    ) : Slot

    data class ValueSlot(
        override val label: ValueLabel,
//        val value: CFGNode,
    ) : Slot

    data class ConstantSlot(
        override val label: ConstantLabel,
        val predicate: (Int) -> Boolean,
    ) : Slot
}
