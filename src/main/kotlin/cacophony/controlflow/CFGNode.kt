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
        tree: CFGNode.Unconditional,
    ) : CFGVertex(tree) {
        override fun dependents() = emptyList<CFGLabel>()
    }
}

sealed interface CFGNode {
    sealed interface Unconditional : CFGNode

    sealed interface Leaf : CFGNode

    sealed interface LValue : CFGNode

    class Return :
        Unconditional,
        Leaf

    class Call(
        val declaration: Definition.FunctionDeclaration,
    ) : Unconditional,
        Leaf

    // NOTE: Push may be unnecessary since it can be done via Assignment + MemoryAccess
    class Push(
        val value: CFGNode,
    ) : Unconditional

    // NOTE: Pop may be unnecessary since it can be done via Assignment
    class Pop(
        val regvar: Register,
    ) : Unconditional,
        Leaf

    class Assignment(
        val destination: Register,
        val value: CFGNode,
    ) : Unconditional

    class VariableUse(
        val regvar: Register,
    ) : Unconditional,
        Leaf,
        LValue

    class MemoryAccess(
        val destination: CFGNode,
    ) : Unconditional,
        LValue

    class MemoryWrite(
        val destination: MemoryAccess,
        val value: CFGNode,
    ) : Unconditional

    class Constant(
        val value: Int,
    ) : Unconditional,
        Leaf

    class Sequence(
        val nodes: List<CFGNode>,
    ) : Unconditional

    sealed interface ArithmeticOperator : Unconditional

    class Addition(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    class Subtraction(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    class Multiplication(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    class Division(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    class Modulo(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    sealed interface LogicalOperator : CFGNode

    class LogicalNot(
        val value: CFGNode,
    ) : LogicalOperator

    class Equals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    class NotEquals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    class Less(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    class Greater(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    class LessEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    class GreaterEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator
}
