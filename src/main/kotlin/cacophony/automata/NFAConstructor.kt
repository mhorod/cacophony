package cacophony.automata

import kotlin.collections.emptyMap
import cacophony.utils.AlgebraicRegex

fun buildNFAFromRegex(regex: AlgebraicRegex): NFA<Int> = buildSimpleNFAFromRegex(regex)

// implementation relies on the fact that the initial state will always have number 0
private fun buildSimpleNFAFromRegex(regex: AlgebraicRegex): SimpleNFA {
    return when (regex) {
        is AlgebraicRegex.AtomicRegex -> SimpleNFA(
            0,
            mapOf(Pair(0, regex.symbol) to listOf(1)),
            emptyMap(),
            1)
        is AlgebraicRegex.UnionRegex -> {
            var totalSize = 1
            var prods: Map<Pair<Int, Char>, List<Int>> = mapOf()
            val epsProds : MutableMap<Int, MutableList<Int>> = mutableMapOf()
            val initState = 0
            var finalState = 1
            val subAutomata: MutableList<SimpleNFA> = mutableListOf()
            for (sub in regex.internalRegexes) {
                val subAutomaton = buildSimpleNFAFromRegex(sub)
                subAutomata.add(subAutomaton)
                finalState += subAutomaton.size()
            }
            for (subAutomaton in subAutomata) {
                val sub = subAutomaton.offsetStateNumbers(totalSize)
                prods = prods + sub.getProductions()
                sub.getEpsilonProductions().forEach{(key, value) -> epsProds[key] = value.toMutableList() }
                epsProds.getOrPut(initState) { mutableListOf() }.add(sub.getStartingState())
                totalSize += sub.size()
                epsProds.getOrPut(sub.getAcceptingState()) { mutableListOf() }.add(finalState)
            }
            SimpleNFA(
                initState,
                prods,
                epsProds,
                finalState)
        }
        is AlgebraicRegex.ConcatenationRegex -> {
            var prods: Map<Pair<Int, Char>, List<Int>> = mapOf()
            val epsProds : MutableMap<Int, MutableList<Int>> = mutableMapOf()
            val initState = 0
            var finalState = 1
            val subAutomata: MutableList<SimpleNFA> = mutableListOf()
            for (sub in regex.internalRegexes) {
                val subAutomaton = buildSimpleNFAFromRegex(sub)
                subAutomata.add(subAutomaton)
                finalState += subAutomaton.size()
            }
            var totalSize = 1
            epsProds.getOrPut(initState) { mutableListOf() }.add(1)
            for (subAutomaton in subAutomata) {
                val sub = subAutomaton.offsetStateNumbers(totalSize)
                prods = prods + sub.getProductions()
                sub.getEpsilonProductions().forEach{(key, value) -> epsProds[key] = value.toMutableList() }
                totalSize += sub.size()
                epsProds.getOrPut(sub.getAcceptingState()) { mutableListOf() }.add(totalSize)
            }
            SimpleNFA(
                initState,
                prods,
                epsProds,
                finalState)
        }
        is AlgebraicRegex.StarRegex -> {
            val initState = 0
            val sub = buildSimpleNFAFromRegex(regex.internalRegex).offsetStateNumbers(1)
            val prods = sub.getProductions().toMutableMap()
            val epsProds : MutableMap<Int, MutableList<Int>> = mutableMapOf()
            sub.getEpsilonProductions().forEach{(key, value) -> epsProds[key] = value.toMutableList() }
            epsProds[initState] = mutableListOf(initState + 1)
            epsProds.getOrPut(sub.getAcceptingState()) { mutableListOf() }.add(initState)
            SimpleNFA(
                initState,
                prods,
                epsProds,
                initState)
        }
    }
}

private class SimpleNFA(
    private val start: Int,
    private val prod: Map<Pair<Int, Char>, List<Int>>,
    private val epsProd: Map<Int, List<Int>>,
    private val accept: Int,
) : NFA<Int> {
    private val all = (
                setOf(start, accept) union
                        prod.keys
                            .unzip()
                            .first
                            .toSet() union prod.values.flatten().toSet() union epsProd.keys union
                        epsProd.values
                            .flatten()
                            .toSet()
                ).toList()
    override fun getStartingState() = start
    override fun getAcceptingState() = accept
    override fun isAccepting(state: Int): Boolean {
        return state == accept
    }
    override fun getAllStates() = all
    override fun getProductions() = prod
    override fun getProductions(
        state: Int,
        symbol: Char,
    ) = prod[state to symbol] ?: emptyList()

    override fun getEpsilonProductions(): Map<Int, List<Int>> {
        return epsProd
    }

    fun size() = all.size

    fun offsetStateNumbers(offset: Int): SimpleNFA {
        return SimpleNFA(
           this.start + offset,
            this.prod.mapKeys { (key, _) ->
                Pair(key.first + offset, key.second)
            }.mapValues { (_, value) ->
                value.map { it + offset }
            },
           this.epsProd.mapKeys { (key, _) ->
                key + offset
            }.mapValues { (_, value) ->
                value.map { it + offset }
            },
           this.accept + offset,
        )
    }
}
