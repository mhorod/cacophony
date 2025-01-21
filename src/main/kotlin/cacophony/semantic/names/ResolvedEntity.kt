package cacophony.semantic.names

import cacophony.semantic.syntaxtree.Definition

typealias OverloadSet = Map<Int, Definition>

sealed interface ResolvedEntity {
    data class Unambiguous(val definition: Definition) : ResolvedEntity

    interface WithOverloads : ResolvedEntity {
        val overloads: OverloadSet
    }
}
