package cacophony.controlflow

sealed class Variable(
    @Transient private val name: String,
) {
    // Name only for debugging purposes
    companion object {
        var index = 0
    }

    class PrimitiveVariable(name: String) : Variable(name) {
        constructor() : this("pv${index++}")
    }

    class StructVariable(val fields: Map<String, Variable>, name: String) : Variable(name) {
        constructor(fields: Map<String, Variable>) : this(fields, "sv${index++}")
    }

    fun getPrimitives(): List<PrimitiveVariable> =
        when (this) {
            is PrimitiveVariable -> listOf(this)
            is StructVariable -> fields.map { (_, field) -> field.getPrimitives() }.flatten()
        }

    override fun toString(): String = name
}
