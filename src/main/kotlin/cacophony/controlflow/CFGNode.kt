package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

class CFGLabel {
    override fun toString(): String = super.toString().takeLast(9)
}

sealed interface SlotLabel

class RegisterLabel : SlotLabel

class ValueLabel : SlotLabel

class ConstantLabel : SlotLabel

class FunctionLabel : SlotLabel

/**
 * Single computation tree that has no control-flow or data-flow dependencies
 */
sealed interface CFGNode {
    fun children(): List<CFGNode> = emptyList()

    sealed interface Value : CFGNode

    sealed interface Leaf : CFGNode

    sealed interface LValue : CFGNode

    sealed interface RegisterRef : Leaf, LValue

    sealed interface FunctionRef : Leaf {
        val function: Definition.FunctionDefinition?
    }

    data object NoOp : Leaf {
        override fun toString(): String = "nop"
    }

    // declaration is nullable to create pattern without specific function
    data class Function(override val function: Definition.FunctionDefinition?) : FunctionRef

    data class Call(
        val functionRef: FunctionRef,
    ) : CFGNode {
        constructor(function: Definition.FunctionDefinition?) : this(Function(function))

        override fun children(): List<CFGNode> = listOf(functionRef)

        override fun toString(): String = "call ${functionRef.function?.identifier}"
    }

    data object Return :
        Leaf

    // NOTE: Push may be unnecessary since it can be done via Assignment + MemoryAccess
    data class Push(
        val value: CFGNode,
    ) : CFGNode {
        override fun children(): List<CFGNode> = listOf(value)
    }

    // NOTE: Pop may be unnecessary since it can be done via Assignment
    data class Pop(
        val register: RegisterRef,
    ) : Leaf {
        override fun children(): List<CFGNode> = listOf(register)
    }

    data class Assignment(
        val destination: LValue,
        val value: CFGNode,
    ) : CFGNode, Value {
        override fun toString(): String = "($destination = $value)"

        override fun children(): List<CFGNode> = listOf(destination, value)
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
    ) : Value, LValue {
        override fun children(): List<CFGNode> = listOf(destination)
    }

    sealed class Constant(
        open val value: Int,
    ) : Value,
        Leaf {
        override fun toString(): String = value.toString()
    }

    data class ConstantKnown(override val value: Int) : Constant(value)

    data class ConstantLazy(
        override var value: Int,
    ) : Constant(value)

    sealed interface ArithmeticOperator : Value

    sealed interface ArithmeticAssignmentOperator : ArithmeticOperator

    data class Addition(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs + $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class AdditionAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs += $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class Subtraction(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs - $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class SubtractionAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs -= $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class Multiplication(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs * $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class MultiplicationAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs *= $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class Division(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs / $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class DivisionAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs /= $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class Modulo(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator {
        override fun toString(): String = "($lhs % $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class ModuloAssignment(val lhs: LValue, val rhs: CFGNode) : ArithmeticAssignmentOperator {
        override fun toString(): String = "($lhs %= $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class Minus(val value: CFGNode) : ArithmeticOperator {
        override fun toString(): String = "(-$value)"

        override fun children(): List<CFGNode> = listOf(value)
    }

    sealed interface LogicalOperator : Value

    data class LogicalNot(
        val value: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "(~$value)"

        override fun children(): List<CFGNode> = listOf(value)
    }

    data class Equals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs == $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class NotEquals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs != $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class Less(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs < $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class Greater(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs > $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class LessEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs <= $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    data class GreaterEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator {
        override fun toString(): String = "($lhs >= $rhs)"

        override fun children(): List<CFGNode> = listOf(lhs, rhs)
    }

    companion object {
        val UNIT = ConstantKnown(42)
        val FALSE = ConstantKnown(0)
        val TRUE = ConstantKnown(1)
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

    data class FunctionSlot(
        override val label: FunctionLabel,
    ) : Slot, FunctionRef {
        override val function: Definition.FunctionDefinition?
            get() = null
    }
}
