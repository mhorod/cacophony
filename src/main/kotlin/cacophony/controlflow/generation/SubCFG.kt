package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

/**
 * Subgraph of Control Flow Graph created from a certain AST subtree
 * @property access Pure access to the value produced by the graph
 */
internal sealed interface SubCFG {
    val access: CFGNode.Unconditional

    /**
     * Indicates that no control flow vertex was created during expression translation
     */
    data class Immediate(override val access: CFGNode.Unconditional) : SubCFG

    /**
     * Indicates that expression was translated to a graph of CFG vertices.
     *
     * @property entry Entry point to the graph i.e. the first vertex that should be executed before others
     * @property exit Exit point of the graph i.e. the last vertex that should be executed after others
     */
    data class Extracted(
        val entry: GeneralCFGVertex,
        val exit: GeneralCFGVertex.UnconditionalVertex,
        override val access: CFGNode.Unconditional,
    ) : SubCFG {
        infix fun merge(rhs: Extracted): Extracted {
            exit.connect(rhs.entry.label)
            return Extracted(entry, rhs.exit, rhs.access)
        }
    }
}
