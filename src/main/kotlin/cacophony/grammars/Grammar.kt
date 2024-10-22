package cacophony.grammars

import cacophony.utils.AlgebraicRegex

data class Production<SymbolType>(
    val lhs: SymbolType,
    // TODO: Uncomment after #46
    // val rhs: AlgebraicRegex<SymbolType>,
    val rhs: AlgebraicRegex,
)

data class Grammar<SymbolType>(
    val start: SymbolType,
    val productions: Collection<Production<SymbolType>>,
)
