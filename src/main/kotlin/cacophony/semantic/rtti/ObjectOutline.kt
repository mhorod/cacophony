package cacophony.semantic.rtti

import cacophony.semantic.syntaxtree.Type
import cacophony.semantic.types.TypeExpr

typealias ObjectOutlineLocation = Map<TypeExpr, String>

fun sizeOf(type: Type): Int = throw NotImplementedError()
