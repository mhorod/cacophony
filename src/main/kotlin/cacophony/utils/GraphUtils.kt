package cacophony.utils

import kotlin.math.min

// Returns a set of all elements reachable from a collection of nodes in a directed graph.
fun <VType> getReachableFrom(
    from: Collection<VType>,
    graph: Map<VType, Collection<VType>>,
): Set<VType> {
    val reachable = from.toMutableSet()
    val workList = from.toMutableList()
    while (workList.isNotEmpty()) {
        val u = workList.removeLast()
        for (v in graph.getOrDefault(u, listOf())) {
            if (!reachable.contains(v)) {
                reachable.add(v)
                workList.add(v)
            }
        }
    }
    return reachable
}

fun <VType> reverseGraph(graph: Map<VType, Collection<VType>>): Map<VType, Set<VType>> {
    val reversed: MutableMap<VType, MutableSet<VType>> = mutableMapOf()
    for ((u, vs) in graph) {
        for (v in vs) {
            reversed.getOrPut(v) { mutableSetOf() }.add(u)
        }
    }
    return reversed
}

// Returns a transitive closure of a directed graph.
fun <VType> getTransitiveClosure(graph: Map<VType, Collection<VType>>): Map<VType, Set<VType>> {
    val closure: MutableMap<VType, Set<VType>> = mutableMapOf()
    for (component in getStronglyConnectedComponents(graph)) {
        val reachable =
            component union component.flatMap { graph[it] ?: emptyList() }.flatMap { closure[it] ?: emptyList() }
        component.forEach { closure[it] = reachable }
    }
    return closure
}

// Returns a "proper" transitive closure of a directed graph (i.e. ignoring paths of length zero).
fun <VType> getProperTransitiveClosure(graph: Map<VType, Collection<VType>>): Map<VType, Set<VType>> {
    val closure: MutableMap<VType, Set<VType>> = mutableMapOf()
    for (component in getStronglyConnectedComponents(graph)) {
        val step = component.flatMap { graph[it] ?: emptyList() }
        val reachable = step union step.flatMap { closure[it] ?: emptyList() }
        component.forEach { closure[it] = reachable }
    }
    return closure
}

// Returns a list of strongly connected components of a graph in a reverse topological order.
fun <VType> getStronglyConnectedComponents(graph: Map<VType, Collection<VType>>): List<List<VType>> {
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
