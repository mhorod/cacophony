package cacophony.grammars

import cacophony.automata.DFA
import cacophony.utils.getTransitiveClosure

sealed class Entity<out Q, out A, out R> {
    data class Ref<Q, A, R>(val ref: DFAStateReference<Q, A, R>) : Entity<Q, A, R>()

    data class Sym<A>(val sym: A) : Entity<Nothing, A, Nothing>()
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
        for (key in firstGraph.keys)
            println(key.toString() + " " + firstGraph[key].toString())
        val closure = getTransitiveClosure(firstGraph)
        for (key in closure.keys)
            println(key.toString() + " " + closure[key].toString())
        firstMap =
            closure.mapNotNull { (key, values) ->
                val newKey = toState(key) ?: return@mapNotNull null
                val newValues = values.mapNotNull { toSymbol(it) }.toSet()
                return@mapNotNull Pair(newKey, newValues)
            }.toMap()
    }
}

fun <StateType, SymbolType, ResultType> findFirst(
    automata: Map<SymbolType, DFA<StateType, SymbolType, ResultType>>,
    nullable: Collection<DFAStateReference<StateType, SymbolType, ResultType>>,
): Map<DFAStateReference<StateType, SymbolType, ResultType>, Collection<SymbolType>> = FindFirstImpl(automata, nullable).firstMap
