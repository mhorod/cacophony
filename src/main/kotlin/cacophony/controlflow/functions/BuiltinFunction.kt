package cacophony.controlflow.functions

import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Definition
import cacophony.utils.Location

// TODO: hacked atm
private val loc = Pair(Location(0), Location(0))
val mallocFunction =
    Definition.ForeignFunctionDeclaration(
        loc,
        "malloc",
        BaseType.Functional(loc, listOf(BaseType.Basic(loc, "Int")), BaseType.Basic(loc, "")),
        BaseType.Basic(loc, ""),
    )
