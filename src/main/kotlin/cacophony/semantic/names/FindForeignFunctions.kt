package cacophony.semantic.names

import cacophony.semantic.syntaxtree.Definition

fun findForeignFunctions(nr: NameResolutionResult): Set<Definition.ForeignFunctionDeclaration> =
    nr.values.filterIsInstance<ResolvedName.Function>().flatMap {
        it.def.toMap().values
    }.filterIsInstance<Definition.ForeignFunctionDeclaration>().toSet()
