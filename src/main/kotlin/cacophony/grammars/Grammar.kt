package cacophony.grammars

import cacophony.automata.joinAutomata
import cacophony.automata.minimalization.buildDFAFromRegex
import cacophony.utils.AlgebraicRegex

data class Production<SymbolType>(
    val lhs: SymbolType,
    val rhs: AlgebraicRegex<SymbolType>,
)

infix fun <SymbolType> SymbolType.produces(regex: AlgebraicRegex<SymbolType>) = Production(this, regex)

data class Grammar<SymbolType>(
    val start: SymbolType,
    val productions: Collection<Production<SymbolType>>,
) {
    fun buildAutomata() = productions.groupBy { it.lhs }.mapValues { buildAutomaton(it.value) }

    private fun buildAutomaton(productions: List<Production<SymbolType>>) =
        joinAutomata(productions.map { buildDFAFromRegex(it.rhs) }.zip(productions))
}

fun <SymbolType> grammarOf(
    start: SymbolType,
    vararg productions: Production<SymbolType>,
) = Grammar(start, productions.toList())
