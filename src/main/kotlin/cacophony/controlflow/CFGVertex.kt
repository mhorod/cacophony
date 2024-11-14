package cacophony.controlflow

/**
 * Represents the root of a single computation tree and the nodes dependent on it
 */
sealed class CFGVertex(
    val tree: CFGNode,
) {
    abstract fun dependents(): List<CFGLabel>

    class Conditional(
        tree: CFGNode,
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
