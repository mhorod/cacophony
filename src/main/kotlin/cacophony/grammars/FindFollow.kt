package cacophony.grammars

import cacophony.automata.DFA
import java.util.ArrayDeque

fun <StateT, SymbolT, ResultT> findFollow(
    automata: Map<SymbolT, DFA<StateT, SymbolT, ResultT>>,
    nullable: Collection<DFAStateReference<StateT, SymbolT, ResultT>>,
    first: StateToSymbolsMap<StateT, SymbolT, ResultT>,
): Map<SymbolT, Set<SymbolT>> = getDefaultFollowSetsForRelevantSymbols(automata) + FactContext(automata, nullable, first).followForSymbols

fun <StateT, SymbolT, ResultT> findExtendedFollowForStateReferences(
    automata: Map<SymbolT, DFA<StateT, SymbolT, ResultT>>,
    nullable: Collection<DFAStateReference<StateT, SymbolT, ResultT>>,
    first: StateToSymbolsMap<StateT, SymbolT, ResultT>,
): StateToSymbolsMap<StateT, SymbolT, ResultT> {
    val followForSymbols = findFollow(automata, nullable, first)
    return automata
        .flatMap { (symbol, automaton) ->
            val followSet = followForSymbols[symbol]!!
            automaton.getAllStates().map { Pair(it at automaton, followSet) }
        }.toMap()
}

private infix fun <StateT, SymbolT, ResultT> StateT.at(
    automaton: DFA<StateT, SymbolT, ResultT>,
): DFAStateReference<StateT, SymbolT, ResultT> = DFAStateReference(this, automaton)

private sealed class Fact<out StateT, out SymbolT, out ResultT> {
    data class Follows<SymbolT>(
        val follower: SymbolT,
        val followed: SymbolT,
    ) : Fact<Nothing, SymbolT, Nothing>()

    data class FirstPlusOf<StateT, SymbolT, ResultT>(
        val symbol: SymbolT,
        val state: DFAStateReference<StateT, SymbolT, ResultT>,
    ) : Fact<StateT, SymbolT, ResultT>()
}

private infix fun <SymbolT> SymbolT.follows(followed: SymbolT): Fact.Follows<SymbolT> = Fact.Follows(this, followed)

private infix fun <StateT, SymbolT, ResultT> SymbolT.firstPlusOf(
    state: DFAStateReference<StateT, SymbolT, ResultT>,
): Fact.FirstPlusOf<StateT, SymbolT, ResultT> = Fact.FirstPlusOf(this, state)

private class FactContext<StateT, SymbolT, ResultT>(
    private val automata: Map<SymbolT, DFA<StateT, SymbolT, ResultT>>,
    private val nullable: Collection<DFAStateReference<StateT, SymbolT, ResultT>>,
    private val first: StateToSymbolsMap<StateT, SymbolT, ResultT>,
) {
    val followForSymbols: Map<SymbolT, Set<SymbolT>>

    private val facts: MutableSet<Fact<StateT, SymbolT, ResultT>> = mutableSetOf()
    private val toProcess: ArrayDeque<Fact<StateT, SymbolT, ResultT>> = ArrayDeque()

    private val acceptingStates: Map<SymbolT, Set<DFAStateReference<StateT, SymbolT, ResultT>>> =
        automata.mapValues { (_, automaton) ->
            automaton
                .getAllStates()
                .filter { automaton.isAccepting(it) }
                .map { it at automaton }
                .toSet()
        }

    private val enteringTransitions:
        Map<
            DFAStateReference<StateT, SymbolT, ResultT>,
            Collection<Pair<DFAStateReference<StateT, SymbolT, ResultT>, SymbolT>>,
        > =
        automata.values
            .flatMap { automaton ->
                automaton
                    .getProductions()
                    .map { (args, result) -> Pair(result at automaton, Pair(args.first at automaton, args.second)) }
            }.groupBy({ it.first }, { it.second })

    init {
        determineFacts()
        followForSymbols =
            facts
                .filterIsInstance<Fact.Follows<SymbolT>>()
                .groupBy({ it.followed }, { it.follower })
                .mapValues { it.value.toSet() }
    }

    private fun determineFacts() {
        initializeFacts()
        while (!toProcess.isEmpty()) {
            process(toProcess.removeFirst())
        }
    }

    private fun initializeFacts() =
        discoverAll(
            automata.values.flatMap { automaton ->
                automaton.getAllStates().flatMap { state ->
                    val stateRef = state at automaton
                    first[stateRef]!!.map { it firstPlusOf stateRef }
                }
            },
        )

    private fun process(fact: Fact<StateT, SymbolT, ResultT>) =
        discoverAll(
            when (fact) {
                is Fact.FirstPlusOf -> implicationsOfFirstPlusOf(fact)
                is Fact.Follows -> implicationsOfFollows(fact)
            },
        )

    private fun implicationsOfFirstPlusOf(fact: Fact.FirstPlusOf<StateT, SymbolT, ResultT>): Collection<Fact<StateT, SymbolT, ResultT>> =
        enteringTransitions
            .getOrDefault(fact.state, emptyList())
            .flatMap { (state, symbol) ->
                listOf(fact.symbol follows symbol) + (if (isNullable(symbol)) listOf(fact.symbol firstPlusOf state) else emptyList())
            }

    private fun implicationsOfFollows(fact: Fact.Follows<SymbolT>): Collection<Fact<StateT, SymbolT, ResultT>> =
        acceptingStates
            .getOrDefault(fact.followed, emptySet())
            .map { fact.follower firstPlusOf it }

    private fun discoverAll(facts: Collection<Fact<StateT, SymbolT, ResultT>>) = facts.forEach { discover(it) }

    private fun discover(fact: Fact<StateT, SymbolT, ResultT>) {
        if (facts.add(fact)) {
            toProcess.add(fact)
        }
    }

    private fun isNullable(symbol: SymbolT): Boolean = automata[symbol]?.let { nullable.contains(it.getStartingState() at it) } ?: false
}

private fun <SymbolT> getDefaultFollowSetsForRelevantSymbols(automata: Map<SymbolT, DFA<*, SymbolT, *>>): Map<SymbolT, Set<SymbolT>> =
    getRelevantSymbols(automata).associateWith { emptySet() }

private fun <SymbolT> getRelevantSymbols(automata: Map<SymbolT, DFA<*, SymbolT, *>>): Set<SymbolT> =
    automata.keys union automata.values.flatMap { it.getProductions().keys.map { (_, symbol) -> symbol } }.toSet()
