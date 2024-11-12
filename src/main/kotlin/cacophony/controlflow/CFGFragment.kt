package cacophony.controlflow

typealias CFGVertices = Map<CFGLabel, CFGVertex>

/**
 * Control-Flow Graph generated for a single function
 *
 * @property vertices vertices of the graph
 * @property initialLabel entry point to the computation
 */
data class CFGFragment(val vertices: CFGVertices, val initialLabel: CFGLabel)
