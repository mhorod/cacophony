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

    sealed interface RegisterRef : Leaf, LValue

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
        val register: RegisterRef,
    ) :
        Leaf

    data class Assignment(
        val destination: LValue,
        val value: CFGNode,
    ) : CFGNode {
        override fun toString(): String = "($destination = $value)"
    }

    data class RegisterUse(
        val register: Register,
    ) : Value,
        RegisterRef {
        @OptIn(ExperimentalStdlibApi::class)
        override fun toString(): String =
            when (register) {
                is Register.FixedRegister -> register.hardwareRegister.toString()
                is Register.VirtualRegister -> "VReg(${register.hashCode().toHexString()})"
            }
    }

    data class MemoryAccess(
        val destination: CFGNode,
    ) : LValue

    data class Constant(
        val value: Int,
    ) : Value,
        Leaf {
        override fun toString(): String = value.toString()
    }

    sealed interface ArithmeticOperator : Value

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

    sealed interface LogicalOperator : Value

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

    /* Slots are used by patterns only. Each slot represents some CFGNode specifying some
     * constraints
     */
    sealed interface Slot : CFGNode {
        val label: SlotLabel
    }

    data class RegisterSlot(
        override val label: RegisterLabel,
    ) : Slot, RegisterRef

    data class ValueSlot(
        override val label: ValueLabel,
    ) : Slot, Value

    // will be used later for optimization purposes; see [CacophonyPattern.ConstantPattern]
    data class ConstantSlot(
        override val label: ConstantLabel,
        val predicate: (Int) -> Boolean,
    ) : Slot, Value
}
