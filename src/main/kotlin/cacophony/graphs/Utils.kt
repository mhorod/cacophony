package cacophony.graphs

import kotlin.math.min

// Returns a set of all elements reachable from a collection of nodes in a directed graph.
fun <VertexT> getReachableFrom(from: Collection<VertexT>, graph: Map<VertexT, Collection<VertexT>>): Set<VertexT> {
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

fun <VertexT> reverseGraph(graph: Map<VertexT, Collection<VertexT>>): Map<VertexT, Set<VertexT>> {
    val reversed: MutableMap<VertexT, MutableSet<VertexT>> = mutableMapOf()
    for ((u, vs) in graph) {
        for (v in vs) {
            reversed.getOrPut(v) { mutableSetOf() }.add(u)
        }
    }
    return reversed
}

// Returns a transitive closure of a directed graph.
fun <VertexT> getTransitiveClosure(graph: Map<VertexT, Collection<VertexT>>): Map<VertexT, Set<VertexT>> {
    val closure: MutableMap<VertexT, Set<VertexT>> = mutableMapOf()
    for (component in getStronglyConnectedComponents(graph)) {
        val reachable =
            component union component.flatMap { graph[it] ?: emptyList() }.flatMap { closure[it] ?: emptyList() }
        component.forEach { closure[it] = reachable }
    }
    return closure
}

// Returns a "proper" transitive closure of a directed graph (i.e. ignoring paths of length zero).
fun <VertexT> getProperTransitiveClosure(graph: Map<VertexT, Collection<VertexT>>): Map<VertexT, Set<VertexT>> {
    val closure: MutableMap<VertexT, Set<VertexT>> = mutableMapOf()
    for (component in getStronglyConnectedComponents(graph)) {
        val step = component.flatMap { graph[it] ?: emptyList() }
        val reachable = step union step.flatMap { closure[it] ?: emptyList() }
        component.forEach { closure[it] = reachable }
    }
    return closure
}

fun <VertexT> getSymmetricClosure(graph: Map<VertexT, Collection<VertexT>>): Map<VertexT, Set<VertexT>> {
    val result = graph.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap()
    graph.forEach { (k, v) ->
        v.forEach { result.getOrPut(it) { mutableSetOf() }.add(k) }
    }
    return result
}

fun <VertexT> getProperTransitiveSymmetricClosure(graph: Map<VertexT, Collection<VertexT>>): Map<VertexT, Set<VertexT>> {
    val symmetric = getSymmetricClosure(graph)
    return getTransitiveClosure(symmetric).mapValues { (k, v) -> v - setOf(k) }
}

// Returns a list of strongly connected components of a graph in a reverse topological order.
fun <VertexT> getStronglyConnectedComponents(graph: Map<VertexT, Collection<VertexT>>): List<List<VertexT>> {
    val components: MutableList<List<VertexT>> = mutableListOf()
    val componentIndex: MutableMap<VertexT, Int> = mutableMapOf()
    val sizeMap: MutableMap<VertexT, Int> = mutableMapOf()
    val stack: MutableList<VertexT> = mutableListOf()

    val dfs =
        DeepRecursiveFunction { v ->
            var low = stack.size
            sizeMap[v] = stack.size
            stack.add(v)
            for (e in graph[v] ?: emptyList()) {
                if (componentIndex[e] == null) {
                    low = min(low, sizeMap.getOrElse(e) { callRecursive(e) })
                }
            }
            if (low == sizeMap[v]) {
                val component: MutableList<VertexT> = mutableListOf()
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
