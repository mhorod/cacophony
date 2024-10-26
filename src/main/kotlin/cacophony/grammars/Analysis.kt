package cacophony.grammars

import cacophony.automata.DFA
import kotlin.math.min

typealias DFAStateReference<StateType, SymbolType, ResultType> =
    Pair<StateType, DFA<StateType, SymbolType, ResultType>>

sealed class Entity<out Q, out A, out R> {
    data class Ref<Q, A, R>(val ref: DFAStateReference<Q, A, R>) : Entity<Q, A, R>()

    data class Sym<A>(val sym: A) : Entity<Nothing, A, Nothing>()
}

typealias StateToSymbolsMap<StateType, SymbolType, ResultType> =
    Map<DFAStateReference<StateType, SymbolType, ResultType>, Collection<SymbolType>>

fun <StateType, SymbolType, ResultType> findNullable(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
): Collection<DFAStateReference<StateType, SymbolType, ResultType>> {
    TODO("Not implemented")
}

class FindFirstImpl<StateType, SymbolType, ResultType>(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
) {
    private val stateReferenceToSymbol = automata.map { (symbol, dfa) -> Pair(Pair(dfa.getStartingState(), dfa), symbol) }.toMap()
    private val symbolToStateReference = stateReferenceToSymbol.map { (u, v) -> Pair(v, u) }.toMap()
    val firstMap: Map<DFAStateReference<StateType, SymbolType, ResultType>, Set<SymbolType>>

    private fun fromState(ref: DFAStateReference<StateType, SymbolType, ResultType>): Entity<StateType, SymbolType, ResultType> {
        val sym: SymbolType? = stateReferenceToSymbol[ref]
        return if (sym == null) {
            Entity.Ref(ref)
        } else {
            Entity.Sym(sym)
        }
    }

    private fun fromSymbol(sym: SymbolType): Entity<StateType, SymbolType, ResultType> {
        return Entity.Sym(sym)
    }

    private fun toState(entity: Entity<StateType, SymbolType, ResultType>): DFAStateReference<StateType, SymbolType, ResultType>? {
        if (entity is Entity.Ref) {
            return entity.ref
        }
        if (entity is Entity.Sym) {
            return symbolToStateReference[entity.sym]
        }
        return null
    }

    private fun toSymbol(entity: Entity<StateType, SymbolType, ResultType>): SymbolType? {
        if (entity is Entity.Ref) {
            return stateReferenceToSymbol[entity.ref]
        }
        if (entity is Entity.Sym) {
            return entity.sym
        }
        return null
    }

    init {
        val nullableSymbols = nullable.mapNotNull { stateReferenceToSymbol[it] }.toSet()
        val firstGraph = mutableMapOf<Entity<StateType, SymbolType, ResultType>, MutableSet<Entity<StateType, SymbolType, ResultType>>>()
        for (dfa in automata.values) {
            for (state in dfa.getAllStates())
                firstGraph.computeIfAbsent(fromState(Pair(state, dfa))) { mutableSetOf() }
            for (production in dfa.getProductions()) {
                val from = fromState(Pair(production.key.first, dfa))
                val to = fromState(Pair(production.value, dfa))
                val firstSet = firstGraph.getOrPut(from) { mutableSetOf() }
                firstSet.add(fromSymbol(production.key.second))
                if (nullableSymbols.contains(production.key.second)) {
                    firstSet.add(to)
                }
            }
        }
        println("BEG")
        for (key in firstGraph.keys)
            println(key.toString() + " " + firstGraph[key].toString())
        println("END")
        val closure = transitiveClosure(firstGraph)
        println("BEG")
        for (key in closure.keys)
            println(key.toString() + " " + closure[key].toString())
        println("END")
        firstMap =
            closure.mapNotNull { (key, values) ->
                val newKey = toState(key) ?: return@mapNotNull null
                val newValues = values.mapNotNull { toSymbol(it) }.toSet()
                return@mapNotNull Pair(newKey, newValues)
            }.toMap()
    }
}

fun <VType> transitiveClosure(graph: Map<VType, Collection<VType>>): Map<VType, Set<VType>> {
    val closure: MutableMap<VType, Set<VType>> = mutableMapOf()
    for (component in findStronglyConnectedComponents(graph)) {
        val reachable = component.toSet() union component.flatMap { graph[it] ?: emptyList() }.flatMap { closure[it] ?: emptyList() }
        component.forEach { closure[it] = reachable }
    }
    return closure
}

fun <VType> findStronglyConnectedComponents(graph: Map<VType, Collection<VType>>): List<List<VType>> {
    val components: MutableList<List<VType>> = mutableListOf()
    val componentIndex: MutableMap<VType, Int> = mutableMapOf()
    val sizeMap: MutableMap<VType, Int> = mutableMapOf()
    val stack: MutableList<VType> = mutableListOf()

    val dfs =
        DeepRecursiveFunction { v ->
            var low = stack.size
            sizeMap[v] = stack.size
            stack.add(v)
            for (e in graph[v] ?: emptyList())
                if (componentIndex[e] == null) {
                    low = min(low, sizeMap.getOrElse(e) { callRecursive(e) })
                }
            if (low == sizeMap[v]) {
                val component: MutableList<VType> = mutableListOf()
                while (stack.size > sizeMap[v]!!) {
                    val u = stack.removeLast()
                    componentIndex[u] = components.size
                    component.add(u)
                }
                components.add(component)
            }
            low
        }

    graph.keys.forEach { v ->
        if (sizeMap[v] == null) dfs(v)
    }

    return components
}

fun <StateType, SymbolType, ResultType> findFirst(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
): Map<DFAStateReference<StateType, SymbolType, ResultType>, Collection<SymbolType>> = FindFirstImpl(automata, nullable).firstMap

fun <StateType, SymbolType, ResultType> findFollow(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
    first: StateToSymbolsMap<StateType, SymbolType, ResultType>,
): StateToSymbolsMap<StateType, SymbolType, ResultType> {
    TODO("Not implemented")
}

data class AnalyzedGrammar<StateType, SymbolType>(
    val syncSymbolTypes: Collection<SymbolType>,
    val automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
    val nullable: Collection<DFAStateReference<StateType, SymbolType, Production<SymbolType>>>,
    val first: StateToSymbolsMap<StateType, SymbolType, Production<SymbolType>>,
    val follow: StateToSymbolsMap<StateType, SymbolType, Production<SymbolType>>,
) {
    companion object {
        fun <StateType, SymbolType, ResultType> fromAutomata(
            syncSymbolTypes: Collection<SymbolType>,
            automata: Map<SymbolType, DFA<StateType, SymbolType, Production<SymbolType>>>,
        ): AnalyzedGrammar<StateType, SymbolType> {
            val nullable = findNullable(automata)
            val first = findFirst(automata, nullable)
            val follow = findFollow(automata, nullable, first)
            return AnalyzedGrammar(syncSymbolTypes, automata, nullable, first, follow)
        }
    }
}
