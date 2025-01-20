package cacophony.controlflow

sealed class Variable(
    @Transient private val name: String,
) {
    // Name only for debugging purposes
    companion object {
        var index = 0
    }

    class PrimitiveVariable(name: String, val holdsReference: Boolean = false) : Variable(name) {
        constructor(holdsReference: Boolean = false) : this("pv${index++}", holdsReference)
    }

    class StructVariable(val fields: Map<String, Variable>, name: String) : Variable(name) {
        constructor(fields: Map<String, Variable>) : this(fields, "sv${index++}")
    }

    object Heap : Variable("<heap>")

    fun getPrimitives(): List<PrimitiveVariable> =
        when (this) {
            is PrimitiveVariable -> listOf(this)
            is StructVariable -> fields.map { (_, field) -> field.getPrimitives() }.flatten()
            is Heap -> emptyList()
        }

    fun size(): Int =
        when (this) {
            is PrimitiveVariable -> 1
            is StructVariable -> fields.map { (_, field) -> field.size() }.sum()
            is Heap -> 0 // This is an internal entity
        }

    override fun toString(): String = name
}
