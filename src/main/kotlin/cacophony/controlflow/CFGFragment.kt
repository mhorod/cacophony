package cacophony.controlflow

typealias CFGFragment = Map<CFGLabel, CFGVertex>

interface CFGFragmentBuilder {
    fun add(
        label: CFGLabel,
        tree: CFGVertex,
    )

    fun getCFGFragment(): CFGFragment
}
