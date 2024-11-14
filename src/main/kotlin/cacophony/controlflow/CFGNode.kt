package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

class CFGLabel

sealed interface SlotLabel
class RegisterLabel: SlotLabel
class ValueLabel: SlotLabel
class ConstantLabel: SlotLabel

sealed class CFGVertex(
    val tree: CFGNode,
) {
    abstract fun dependents(): List<CFGLabel>

    class Conditional(
        tree: CFGNode.LogicalOperator,
        val trueDestination: CFGLabel,
        val falseDestination: CFGLabel,
    ) : CFGVertex(tree) {
        override fun dependents() = listOf(trueDestination, falseDestination)
    }

    class Jump(
        tree: CFGNode.Unconditional,
        val destination: CFGLabel,
    ) : CFGVertex(tree) {
        override fun dependents() = listOf(destination)
    }

    class Final(
        tree: CFGNode.Unconditional,
    ) : CFGVertex(tree) {
        override fun dependents() = emptyList<CFGLabel>()
    }
}

sealed interface CFGNode {
    sealed interface Unconditional : CFGNode

    sealed interface Value : CFGNode

    sealed interface Leaf : CFGNode

    sealed interface LValue : CFGNode

    data object Return :
        Unconditional,
        Leaf

    data class Call(
        val declaration: Definition.FunctionDeclaration,
    ) : Unconditional,
        Leaf

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
        val destination: Register,
        val value: CFGNode,
    ) : Unconditional

    data class VariableUse(
        val regvar: Register,
    ) : Unconditional,
        Leaf,
        LValue,
        Value

    data class MemoryAccess(
        val destination: CFGNode,
    ) : Unconditional,
        LValue,
        Value

    data class MemoryWrite(
        val destination: MemoryAccess,
        val value: CFGNode,
    ) : Unconditional

    data class Constant(
        val value: Int,
    ) : Unconditional,
        Leaf,
        Value

    data class Sequence(
        val nodes: List<CFGNode>,
    ) : Unconditional

    sealed interface ArithmeticOperator : Unconditional, Value

    data class Addition(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    data class Subtraction(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    data class Multiplication(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    data class Division(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    data class Modulo(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    sealed interface LogicalOperator : CFGNode, Value

    data class LogicalNot(
        val value: CFGNode,
    ) : LogicalOperator

    data class Equals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    data class NotEquals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    data class Less(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    data class Greater(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    data class LessEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    data class GreaterEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    /* TODO: document */
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
