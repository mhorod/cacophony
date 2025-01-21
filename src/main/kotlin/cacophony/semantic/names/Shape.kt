package cacophony.semantic.names

import cacophony.semantic.syntaxtree.BaseType
import cacophony.semantic.syntaxtree.Type

sealed interface Shape {
    data object Atomic : Shape

    data class Structural(val fields: Map<String, Shape>) : Shape

    data class Functional(val arity: Int, val result: Shape) : Shape

    data object Top : Shape

    companion object {
        fun from(type: Type): Shape =
            when (type) {
                is BaseType.Functional -> Functional(type.argumentsType.size, from(type.returnType))
                is BaseType.Structural -> Structural(type.fields.mapValues { from(it.value) })
                else -> Atomic
            }

        infix fun Shape.isSubshapeOf(other: Shape): Boolean {
            if (other is Top)
                return true
            return when (this) {
                is Atomic -> other is Atomic
                is Functional -> other is Functional && other.arity == this.arity && this.result isSubshapeOf other.result
                is Structural -> {
                    other is Structural &&
                        this.fields.all { (name, shape) -> other.fields[name]?.let { shape isSubshapeOf it } ?: false }
                }
                is Top -> false
            }
        }
    }
}
