package cacophony.grammars

import cacophony.automata.DFA
import java.util.ArrayDeque

fun <StateType, SymbolType, ResultType> findFollow(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
    first: StateToSymbolsMap<StateType, SymbolType, ResultType>,
): Map<SymbolType, Set<SymbolType>> =
    getDefaultFollowSetsForRelevantSymbols(automata) + FactContext(automata, nullable, first).followForSymbols

private infix fun <StateType, SymbolType, ResultType> StateType.at(
    automaton: DFA<StateType, SymbolType, ResultType>,
): DFAStateReference<StateType, SymbolType, ResultType> = DFAStateReference(this, automaton)

private sealed class Fact<out StateType, out SymbolType, out ResultType> {
    data class Follows<SymbolType>(val follower: SymbolType, val followed: SymbolType) :
        Fact<Nothing, SymbolType, Nothing>()

    data class FirstPlusOf<StateType, SymbolType, ResultType>(
        val symbol: SymbolType,
        val state: DFAStateReference<StateType, SymbolType, ResultType>,
    ) : Fact<StateType, SymbolType, ResultType>()
}

private infix fun <SymbolType> SymbolType.follows(followed: SymbolType): Fact.Follows<SymbolType> = Fact.Follows(this, followed)

private infix fun <StateType, SymbolType, ResultType> SymbolType.firstPlusOf(
    state: DFAStateReference<StateType, SymbolType, ResultType>,
): Fact.FirstPlusOf<StateType, SymbolType, ResultType> = Fact.FirstPlusOf(this, state)

private class FactContext<StateType, SymbolType, ResultType>(
    private val automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    private val nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
    private val first: StateToSymbolsMap<StateType, SymbolType, ResultType>,
) {
    val followForSymbols: Map<SymbolType, Set<SymbolType>>

    private val facts: MutableSet<Fact<StateType, SymbolType, ResultType>> = mutableSetOf()
    private val toProcess: ArrayDeque<Fact<StateType, SymbolType, ResultType>> = ArrayDeque()

    private val acceptingStates: Map<SymbolType, Set<DFAStateReference<StateType, SymbolType, ResultType>>> =
        automata.mapValues { (_, automaton) ->
            automaton.getAllStates().filter { automaton.isAccepting(it) }.map { it at automaton }.toSet()
        }

    private val enteringTransitions:
        Map<
            DFAStateReference<StateType, SymbolType, ResultType>,
            Collection<Pair<DFAStateReference<StateType, SymbolType, ResultType>, SymbolType>>,
            > =
        automata.values
            .flatMap { automaton ->
                automaton
                    .getProductions()
                    .map { (args, result) -> Pair(result at automaton, Pair(args.first at automaton, args.second)) }
            }
            .groupBy({ it.first }, { it.second })

    init {
        determineFacts()
        followForSymbols =
            facts
                .filterIsInstance<Fact.Follows<SymbolType>>()
                .groupBy({ it.followed }, { it.follower })
                .mapValues { it.value.toSet() }
    }

    private fun determineFacts() {
        initializeFacts()
        while (!toProcess.isEmpty())
            process(toProcess.removeFirst())
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

    private fun process(fact: Fact<StateType, SymbolType, ResultType>) =
        discoverAll(
            when (fact) {
                is Fact.FirstPlusOf -> implicationsOfFirstPlusOf(fact)
                is Fact.Follows -> implicationsOfFollows(fact)
            },
        )

    private fun implicationsOfFirstPlusOf(
        fact: Fact.FirstPlusOf<StateType, SymbolType, ResultType>,
    ): Collection<Fact<StateType, SymbolType, ResultType>> =
        enteringTransitions
            .getOrDefault(fact.state, emptyList())
            .flatMap { (state, symbol) ->
                listOf(fact.symbol follows symbol) + (if (isNullable(symbol)) listOf(fact.symbol firstPlusOf state) else emptyList())
            }

    private fun implicationsOfFollows(fact: Fact.Follows<SymbolType>): Collection<Fact<StateType, SymbolType, ResultType>> =
        acceptingStates
            .getOrDefault(fact.followed, emptySet())
            .map { fact.follower firstPlusOf it }

    private fun discoverAll(facts: Collection<Fact<StateType, SymbolType, ResultType>>) = facts.forEach { discover(it) }

    private fun discover(fact: Fact<StateType, SymbolType, ResultType>) {
        if (facts.add(fact)) {
            toProcess.add(fact)
        }
    }

    private fun isNullable(symbol: SymbolType): Boolean = automata[symbol]?.let { nullable.contains(it.getStartingState() at it) } ?: false
}

private fun <SymbolType> getDefaultFollowSetsForRelevantSymbols(
    automata: Map<SymbolType, DFA<*, SymbolType, *>>,
): Map<SymbolType, Set<SymbolType>> = getRelevantSymbols(automata).associateWith { emptySet() }

private fun <SymbolType> getRelevantSymbols(automata: Map<SymbolType, DFA<*, SymbolType, *>>): Set<SymbolType> =
    automata.keys union automata.values.flatMap { it.getProductions().keys.map { (_, symbol) -> symbol } }.toSet()
