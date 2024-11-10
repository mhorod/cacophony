package cacophony.controlflow

import cacophony.semantic.syntaxtree.Definition

class CFGLabel

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
        tree: CFGNode,
    ) : CFGVertex(tree) {
        override fun dependents() = emptyList<CFGLabel>()
    }
}

sealed interface CFGNode {
    sealed interface Unconditional : CFGNode

    sealed interface Leaf : CFGNode

    sealed interface LValue : Unconditional

    data object Return :
        Unconditional,
        Leaf

    data object NoOp : Unconditional, Leaf

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
    ) : LValue,
        Leaf

    data class MemoryAccess(
        val destination: CFGNode,
    ) : LValue

    data class Constant(
        val value: Int,
    ) : Unconditional,
        Leaf

    data class Sequence(
        val nodes: List<CFGNode>,
    ) : Unconditional

    sealed interface ArithmeticOperator : Unconditional

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

    class Minus(val value: CFGNode): ArithmeticOperator

    sealed interface LogicalOperator : CFGNode

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

    class Negation(val value: CFGNode): ArithmeticOperator

    companion object {
        val UNIT = Constant(0)
        val FALSE = Constant(0)
        val TRUE = Constant(1)
    }
}
