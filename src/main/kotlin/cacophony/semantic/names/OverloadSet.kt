package cacophony.semantic.names

// TODO: Replace with actual way to represent overloads
sealed interface FunctionDeclaration

interface OverloadSet {
    operator fun get(arity: Int): FunctionDeclaration?

    fun toMap(): Map<Int, FunctionDeclaration>

    fun withDeclaration(arity: Int, declaration: FunctionDeclaration): OverloadSet
}
