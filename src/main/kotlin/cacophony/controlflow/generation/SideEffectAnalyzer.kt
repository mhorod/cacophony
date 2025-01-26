package cacophony.controlflow.generation

import cacophony.semantic.syntaxtree.Expression

class SideEffectAnalyzer {
    /**
     * Return true when given expression have side effect conflict
     *  and computation of one might impact the computation of the other.
     */
    fun hasClashingSideEffects(e1: Expression, e2: Expression): Boolean = true // For simplicity assume that there's always a clash
}
