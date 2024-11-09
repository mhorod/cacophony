package cacophony.grammars

import cacophony.utils.AlgebraicRegex

data class Production<SymbolT>(
    val lhs: SymbolT,
    val rhs: AlgebraicRegex<SymbolT>,
)

infix fun <SymbolT> SymbolT.produces(regex: AlgebraicRegex<SymbolT>) = Production(this, regex)

data class Grammar<SymbolT>(
    val start: SymbolT,
    val productions: Collection<Production<SymbolT>>,
)
