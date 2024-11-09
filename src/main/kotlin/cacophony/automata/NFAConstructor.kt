package cacophony.automata

import cacophony.utils.AlgebraicRegex
import kotlin.collections.emptyMap

fun <AtomT> buildNFAFromRegex(regex: AlgebraicRegex<AtomT>): NFA<Int, AtomT> = buildSimpleNFAFromRegex(regex)

// Implementation relies on the fact that the initial state will always have number 0.
private fun <AtomT> buildSimpleNFAFromRegex(regex: AlgebraicRegex<AtomT>): SimpleNFA<AtomT> =
    when (regex) {
        is AlgebraicRegex.AtomicRegex ->
            SimpleNFA(
                0,
                mapOf(0 via regex.symbol to listOf(1)),
                emptyMap(),
                1,
            )

        is AlgebraicRegex.UnionRegex -> {
            var totalSize = 1
            val prods: MutableMap<Pair<Int, AtomT>, List<Int>> = mutableMapOf()
            val epsProds: MutableMap<Int, MutableList<Int>> = mutableMapOf()
            val initState = 0
            var finalState = 1
            val subAutomata: MutableList<SimpleNFA<AtomT>> = mutableListOf()
            for (sub in regex.internalRegexes) {
                val subAutomaton = buildSimpleNFAFromRegex(sub)
                subAutomata.add(subAutomaton)
                finalState += subAutomaton.size()
            }
            for (subAutomaton in subAutomata) {
                val sub = subAutomaton.offsetStateNumbers(totalSize)
                prods.putAll(sub.getProductions())
                sub.getEpsilonProductions().forEach { (key, value) -> epsProds[key] = value.toMutableList() }
                epsProds.getOrPut(initState) { mutableListOf() }.add(sub.getStartingState())
                totalSize += sub.size()
                epsProds.getOrPut(sub.getAcceptingState()) { mutableListOf() }.add(finalState)
            }
            SimpleNFA(
                initState,
                prods,
                epsProds,
                finalState,
            )
        }

        is AlgebraicRegex.ConcatenationRegex -> {
            val prods: MutableMap<Pair<Int, AtomT>, List<Int>> = mutableMapOf()
            val epsProds: MutableMap<Int, MutableList<Int>> = mutableMapOf()
            val initState = 0
            var finalState = 1
            val subAutomata: MutableList<SimpleNFA<AtomT>> = mutableListOf()
            for (sub in regex.internalRegexes) {
                val subAutomaton = buildSimpleNFAFromRegex(sub)
                subAutomata.add(subAutomaton)
                finalState += subAutomaton.size()
            }
            var totalSize = 1
            epsProds.getOrPut(initState) { mutableListOf() }.add(1)
            for (subAutomaton in subAutomata) {
                val sub = subAutomaton.offsetStateNumbers(totalSize)
                prods.putAll(sub.getProductions())
                sub.getEpsilonProductions().forEach { (key, value) -> epsProds[key] = value.toMutableList() }
                totalSize += sub.size()
                epsProds.getOrPut(sub.getAcceptingState()) { mutableListOf() }.add(totalSize)
            }
            SimpleNFA(
                initState,
                prods,
                epsProds,
                finalState,
            )
        }

        is AlgebraicRegex.StarRegex -> {
            val initState = 0
            val sub = buildSimpleNFAFromRegex(regex.internalRegex).offsetStateNumbers(1)
            val prods = sub.getProductions().toMutableMap()
            val epsProds: MutableMap<Int, MutableList<Int>> = mutableMapOf()
            sub.getEpsilonProductions().forEach { (key, value) -> epsProds[key] = value.toMutableList() }
            epsProds[initState] = mutableListOf(initState + 1)
            epsProds.getOrPut(sub.getAcceptingState()) { mutableListOf() }.add(initState)
            SimpleNFA(
                initState,
                prods,
                epsProds,
                initState,
            )
        }
    }

fun <AtomT> SimpleNFA<AtomT>.size(): Int = this.getAllStates().size

fun <AtomT> SimpleNFA<AtomT>.offsetStateNumbers(offset: Int): SimpleNFA<AtomT> =
    SimpleNFA(
        this.getStartingState() + offset,
        this
            .getProductions()
            .mapKeys { (key, _) ->
                Pair(key.first + offset, key.second)
            }.mapValues { (_, value) ->
                value.map { it + offset }
            },
        this
            .getEpsilonProductions()
            .mapKeys { (key, _) ->
                key + offset
            }.mapValues { (_, value) ->
                value.map { it + offset }
            },
        this.getAcceptingState() + offset,
    )
