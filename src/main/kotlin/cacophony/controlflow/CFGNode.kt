package cacophony.controlflow

sealed class CFGVertex(
    val tree: CFGNode,
) {
    abstract fun dependents(): List<CFGVertex>

    sealed class Conditional(
        tree: CFGNode.LogicalOperator,
        val trueDestination: CFGVertex,
        val falseDestination: CFGVertex,
    ) : CFGVertex(tree) {
        override fun dependents() = listOf(trueDestination, falseDestination)
    }

    sealed class Jump(
        tree: CFGNode.Unconditional,
        val destination: CFGVertex,
    ) : CFGVertex(tree) {
        override fun dependents() = listOf(destination)
    }

    sealed class Final(
        tree: CFGNode.Unconditional,
    ) : CFGVertex(tree) {
        override fun dependents() = emptyList<CFGVertex>()
    }
}

sealed interface CFGNode {
    sealed interface Unconditional : CFGNode

    sealed interface Leaf : CFGNode

    sealed class Return :
        Unconditional,
        Leaf

    sealed class Call :
        Unconditional,
        Leaf

    sealed class Assignment(
        val regvar: String, // TODO: make Register
        val value: CFGNode,
    ) : Unconditional

    sealed class VariableUse(
        val regvar: String, // TODO: make Register
    ) : Unconditional,
        Leaf

    sealed class MemoryAccess(
        val destination: CFGNode,
    ) : Unconditional

    sealed class Constant(
        val value: Int,
    ) : Unconditional,
        Leaf

    sealed class Sequence(
        vararg val nodes: CFGNode,
    ) : Unconditional

    sealed interface ArithmeticOperator : Unconditional

    sealed class Addition(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    sealed class Subtraction(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    sealed class Multiplication(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    sealed class Division(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    sealed class Modulo(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : ArithmeticOperator

    sealed interface BitwiseOperator : Unconditional

    sealed class BitwiseAnd(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : BitwiseOperator

    sealed class BitwiseOr(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : BitwiseOperator

    sealed interface LogicalOperator : CFGNode

    sealed class LogicalNot(
        val value: CFGNode,
    ) : LogicalOperator

    sealed class LogicalAnd(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    sealed class LogicalOr(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    sealed class Equals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    sealed class NotEquals(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    sealed class Lesser(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    sealed class Greater(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    sealed class LesserEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator

    sealed class GreaterEqual(
        val lhs: CFGNode,
        val rhs: CFGNode,
    ) : LogicalOperator
}
