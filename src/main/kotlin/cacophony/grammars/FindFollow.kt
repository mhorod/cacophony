package cacophony.grammars

import cacophony.automata.DFA
import java.util.ArrayDeque

fun <Q, A, R> findFollow(
    automata: Map<A, DFA<Q, A, R>>,
    nullable: Collection<DFAStateReference<Q, A, R>>,
    first: StateToSymbolsMap<Q, A, R>,
): Map<A, Set<A>> = getDefaultFollowSetsForRelevantSymbols(automata) + FactContext(automata, nullable, first).followForSymbols

private infix fun <Q, A, R> Q.at(automaton: DFA<Q, A, R>): DFAStateReference<Q, A, R> = DFAStateReference(this, automaton)

private sealed class Fact<out Q, out A, out R> {
    data class Follows<A>(val follower: A, val followed: A) : Fact<Nothing, A, Nothing>()

    data class FirstPlusOf<Q, A, R>(val symbol: A, val state: DFAStateReference<Q, A, R>) : Fact<Q, A, R>()
}

private infix fun <A> A.follows(followed: A): Fact.Follows<A> = Fact.Follows(this, followed)

private infix fun <Q, A, R> A.firstPlusOf(state: DFAStateReference<Q, A, R>): Fact.FirstPlusOf<Q, A, R> = Fact.FirstPlusOf(this, state)

private class FactContext<Q, A, R>(
    private val automata: Map<A, DFA<Q, A, R>>,
    private val nullable: Collection<DFAStateReference<Q, A, R>>,
    private val first: StateToSymbolsMap<Q, A, R>,
) {
    val followForSymbols: Map<A, Set<A>>

    private val facts: MutableSet<Fact<Q, A, R>> = mutableSetOf()
    private val toProcess: ArrayDeque<Fact<Q, A, R>> = ArrayDeque()

    private val acceptingStates: Map<A, Set<DFAStateReference<Q, A, R>>> =
        automata.mapValues { (_, automaton) ->
            automaton.getAllStates().filter { automaton.isAccepting(it) }.map { it at automaton }.toSet()
        }

    private val enteringTransitions: Map<DFAStateReference<Q, A, R>, Collection<Pair<DFAStateReference<Q, A, R>, A>>> =
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
                .filterIsInstance<Fact.Follows<A>>()
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

    private fun process(fact: Fact<Q, A, R>) =
        discoverAll(
            when (fact) {
                is Fact.FirstPlusOf -> implicationsOfFirstPlusOf(fact)
                is Fact.Follows -> implicationsOfFollows(fact)
            },
        )

    private fun implicationsOfFirstPlusOf(fact: Fact.FirstPlusOf<Q, A, R>): Collection<Fact<Q, A, R>> =
        enteringTransitions
            .getOrDefault(fact.state, emptyList())
            .flatMap { (state, symbol) ->
                listOf(fact.symbol follows symbol) + (if (isNullable(symbol)) listOf(fact.symbol firstPlusOf state) else emptyList())
            }

    private fun implicationsOfFollows(fact: Fact.Follows<A>): Collection<Fact<Q, A, R>> =
        acceptingStates
            .getOrDefault(fact.followed, emptySet())
            .map { fact.follower firstPlusOf it }

    private fun discoverAll(facts: Collection<Fact<Q, A, R>>) = facts.forEach { discover(it) }

    private fun discover(fact: Fact<Q, A, R>) {
        if (facts.add(fact)) {
            toProcess.add(fact)
        }
    }

    private fun isNullable(symbol: A): Boolean = automata[symbol]?.let { nullable.contains(it.getStartingState() at it) } ?: false
}

private fun <A> getDefaultFollowSetsForRelevantSymbols(automata: Map<A, DFA<*, A, *>>): Map<A, Set<A>> =
    getRelevantSymbols(automata).associateWith { emptySet() }

private fun <A> getRelevantSymbols(automata: Map<A, DFA<*, A, *>>): Set<A> =
    automata.keys union automata.values.flatMap { it.getProductions().keys.map { (_, symbol) -> symbol } }.toSet()
