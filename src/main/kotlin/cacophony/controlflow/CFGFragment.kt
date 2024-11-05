package cacophony.controlflow

typealias CFGFragment = Map<CFGLabel, CFGRootNode>

interface CFGFragmentBuilder {
    fun add(
        label: CFGLabel,
        tree: CFGRootNode,
    )

    fun getCFGFragment(): CFGFragment
}
