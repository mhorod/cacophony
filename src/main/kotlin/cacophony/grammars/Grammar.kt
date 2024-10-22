package cacophony.grammars

import cacophony.utils.AlgebraicRegex

data class Production<Symbol>(
    val lhs: Symbol,
    // TODO: Uncomment after #46
    // val rhs: AlgebraicRegex<Symbol>,
    val rhs: AlgebraicRegex,
)

data class Grammar<Symbol>(
    val start: Symbol,
    val productions: Collection<Production<Symbol>>,
)
