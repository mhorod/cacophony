package cacophony.grammars

import cacophony.utils.AlgebraicRegex

data class Production<SymbolType>(
    val lhs: SymbolType,
    val rhs: AlgebraicRegex<SymbolType>,
)

infix fun <SymbolType> SymbolType.produces(regex: AlgebraicRegex<SymbolType>) = Production(this, regex)

data class Grammar<SymbolType>(
    val start: SymbolType,
    val productions: Collection<Production<SymbolType>>,
)
