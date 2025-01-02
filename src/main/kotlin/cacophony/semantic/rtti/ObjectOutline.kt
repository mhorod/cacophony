package cacophony.semantic.rtti

import cacophony.semantic.syntaxtree.Type

typealias ObjectOutlineLocation = Map<Type, String>

fun sizeOf(type: Type): Int = throw NotImplementedError()
