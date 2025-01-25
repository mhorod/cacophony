package cacophony.controlflow

sealed class Variable(
    @Transient private val name: String,
) {
    abstract fun getNested(): List<Variable>

    // Name only for debugging purposes
    companion object {
        var index = 0
    }

    class PrimitiveVariable(name: String, val holdsReference: Boolean = false) : Variable(name) {
        constructor(holdsReference: Boolean = false) : this("pv${index++}", holdsReference)

        override fun getNested() = emptyList<Variable>()
    }

    class StructVariable(val fields: Map<String, Variable>, name: String) : Variable(name) {
        constructor(fields: Map<String, Variable>) : this(fields, "sv${index++}")

        override fun getNested() = fields.values.toList()
    }

    class FunctionVariable(name: String, val code: PrimitiveVariable, val link: PrimitiveVariable) : Variable(name) {
        constructor(code: PrimitiveVariable, link: PrimitiveVariable) : this("fv${index++}", code, link)

        override fun getNested() = listOf(code, link)
    }

    object Heap : Variable("<heap>") {
        override fun getNested() = emptyList<Variable>()
    }

    fun getPrimitives(): List<PrimitiveVariable> =
        when (this) {
            is PrimitiveVariable -> listOf(this)
            else -> this.getNested().flatMap(Variable::getPrimitives)
        }

    fun size(): Int =
        when (this) {
            is PrimitiveVariable -> 1
            is Heap -> 0 // This is an internal entity
            else -> this.getNested().map(Variable::size).sum()
        }

    override fun toString(): String = name
}

fun getAllNestedVariables(v: Variable): List<Variable> = listOf(v) + v.getNested().flatMap(::getAllNestedVariables)
