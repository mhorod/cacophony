package cacophony.controlflow

typealias CFGVertices = Map<CFGLabel, CFGVertex>

data class CFGFragment(val vertices: CFGVertices, val initialLabel: CFGLabel)
