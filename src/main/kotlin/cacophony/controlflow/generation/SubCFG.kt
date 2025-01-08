package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode

/**
 * Subgraph of Control Flow Graph created from a certain AST subtree
 * @property access Pure access to the value produced by the graph
 */
internal sealed interface SubCFG {
    val access: Layout

    /**
     * Indicates that no control flow vertex was created during expression translation
     */
    data class Immediate(override val access: Layout) : SubCFG {
        constructor(access: CFGNode, holdsReference: Boolean) : this(SimpleLayout(access, holdsReference))
    }

    /**
     * Indicates that expression was translated to a graph of CFG vertices.
     *
     * @property entry Entry point to the graph i.e. the first vertex that should be executed before others
     * @property exit Exit point of the graph i.e. the last vertex that should be executed after others
     */
    data class Extracted(
        val entry: GeneralCFGVertex,
        val exit: GeneralCFGVertex.UnconditionalVertex,
        override val access: Layout,
    ) : SubCFG {
        constructor(
            entry: GeneralCFGVertex,
            exit: GeneralCFGVertex.UnconditionalVertex,
            access: CFGNode,
            holdsReference: Boolean,
        ) : this(entry, exit, SimpleLayout(access, holdsReference))

        infix fun merge(rhs: Extracted): Extracted {
            exit.connect(rhs.entry.label)
            return Extracted(entry, rhs.exit, rhs.access)
        }
    }
}
