package cacophony.codegen.registers

import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register

/**
 * @param successful Map describing the allocation of registers that made it into hardware registers
 * @param spills Registers that did not make it into hardware registers
 */
data class RegisterAllocation(val successful: HardwareRegisterMapping, val spills: Set<Register>)

/**
 * @throws IllegalArgumentException if liveness object is invalid i.e.
 * - there are hardware registers that are expected to be allocated but are not allowed
 * - there is a register interfering with itself
 * - interference or copying mappings contain register outside of liveness.allRegisters
 */
fun allocateRegisters(liveness: Liveness, allowedRegisters: Set<HardwareRegister>): RegisterAllocation =
    RegisterAllocator(liveness, allowedRegisters).allocate()

class RegisterAllocator(private val liveness: Liveness, private val allowedRegisters: Set<HardwareRegister>) {
    fun allocate(): RegisterAllocation {
        generateFirstFitOrder()
        return doColoring()
    }

    init {
        for (mapping in listOf(liveness.interference, liveness.copying)) {
            if (!liveness.allRegisters.containsAll(mapping.keys union mapping.values.flatten()))
                throw IllegalArgumentException("Unexpected register")
        }
    }

    private val wrappers =
        (
            liveness.allRegisters +
                allowedRegisters.map {
                    Register.FixedRegister(
                        it,
                    )
                }
        ).associateWith { RegisterWrapper(it) }
    private val graph =
        symmetricClosure(
            liveness.interference
                .map { (k, v) -> wrappers[k]!! to v.map { wrappers[it]!! }.toMutableSet() }
                .toMap(),
        )

    init {
        wrappers.values.forEach { if (graph[it] == null) graph[it] = mutableSetOf() }
        allowedRegisters.forEach { r ->
            graph[
                wrappers[
                    Register.FixedRegister(
                        r,
                    ),
                ],
            ]!!.addAll(allowedRegisters.filter { it != r }.map { wrappers[Register.FixedRegister(it)]!! })
        }
    }

    private val registers = liveness.allRegisters.map { wrappers[it]!! }.toMutableSet()
    private val copying =
        transitiveSymmetricClosure(
            liveness.copying
                .map { (k, v) ->
                    wrappers[k]!! to v.map { wrappers[it]!! }.toMutableSet()
                }.toMap()
                .toMutableMap(),
        )
    private val stack = mutableListOf<Set<RegisterWrapper>>()
    private val k = allowedRegisters.size

    init {
        if (liveness.allRegisters.any { it is Register.FixedRegister && it.hardwareRegister !in allowedRegisters }) {
            throw IllegalArgumentException("Unexpected hardware register")
        }
        for ((register, interferences) in liveness.interference)
            if (register in interferences)
                throw IllegalArgumentException("Register cannot interfere with itself")
    }

    private val copyGroups = mutableMapOf<RegisterWrapper, Set<RegisterWrapper>>()

    private fun originalNeighbors(r: RegisterWrapper) = (liveness.interference[r.register]?.map { wrappers[it]!! }?.toSet() ?: emptySet())

    private fun neighbors(r: RegisterWrapper) = (graph[r] ?: emptySet())

    private fun copies(r: RegisterWrapper) = (copying[r] ?: emptySet()).filter { it in registers }

    private fun deposit(r: RegisterWrapper) {
        stack.add(copyGroup(r))
        registers.removeAll(copyGroup(r))
        graph[r]?.forEach {
            graph[it]?.remove(r)
        }
    }

    private fun copyGroup(r: RegisterWrapper) = (copyGroups[r] ?: setOf()) + setOf(r)

    private fun coalesce(a: RegisterWrapper, b: RegisterWrapper) {
        if (a.register
                is Register.FixedRegister &&
            b.register is Register.FixedRegister
        ) throw IllegalArgumentException("Cannot coalesce two fixed registers")
        if (b.register is Register.FixedRegister) {
            coalesce(b, a)
            return
        }
        copyGroups[a] = copyGroup(a) + copyGroup(b)
        registers.remove(b)
        graph[b]!!.forEach {
            graph[it]?.remove(b)
            graph[it]?.add(a)
        }
        graph[a]!!.addAll(graph[b]!!)
    }

    private fun generateFirstFitOrder() {
        while (registers.isNotEmpty()) {
            while (true) {
                val x = registers.find { neighbors(it).size < k && registerToCoalesce(it) == null }?.also { deposit(it) }
                if (x != null) continue
                val y =
                    registers
                        .map { it to registerToCoalesce(it) }
                        .firstOrNull { it.second != null }
                        ?.also { (a, b) -> coalesce(a, b!!) }
                if (y == null) break
            }
            if (registers.isNotEmpty())
                deposit(registers.minBy { if (it.register is Register.FixedRegister) Int.MAX_VALUE else neighbors(it).size })
        }
    }

    private fun doColoring(): RegisterAllocation {
        val coloring = mutableMapOf<RegisterWrapper, HardwareRegister>()
        val spills = mutableSetOf<RegisterWrapper>()
        stack.forEach { group ->
            val fixedRegister = group.find { it.register is Register.FixedRegister }
            if (fixedRegister != null) {
                group.forEach {
                    coloring[it] = (fixedRegister.register as Register.FixedRegister).hardwareRegister
                }
            }
        }
        stack.reversed().forEach { group ->
            if (group.any { it.register is Register.FixedRegister }) return@forEach
            val forbiddenColors = group.map { r -> originalNeighbors(r).mapNotNull { coloring[it] } }.flatten().toSet()
            val color = (allowedRegisters - forbiddenColors).firstOrNull()
            if (color == null) spills.addAll(group)
            else group.forEach { coloring[it] = color }
        }
        return RegisterAllocation(
            coloring
                .filter { (k, _) -> k.register in liveness.allRegisters }
                .map { (k, v) ->
                    k.register to v
                }.toMap(),
            spills.map { it.register }.toSet(),
        )
    }

    private fun registerToCoalesce(r: RegisterWrapper) = copies(r).find { shouldCoalesce(r, it) }

    // Uses George criterion for safety
    private fun isCoalesceSafe(a: RegisterWrapper, b: RegisterWrapper) =
        neighbors(a).all {
            it in neighbors(b) ||
                neighbors(it).size < allowedRegisters.size
        }

    private fun shouldCoalesce(a: RegisterWrapper, b: RegisterWrapper): Boolean {
        if (a in neighbors(b) && b !in neighbors(a))
            println("co")
        return b !in neighbors(a) &&
            b in copies(a) &&
            isCoalesceSafe(a, b)
    }

    private fun dfs(from: RegisterWrapper, graph: Map<RegisterWrapper, Set<RegisterWrapper>>, visited: MutableSet<RegisterWrapper>) {
        if (from in visited) return
        visited.add(from)
        graph[from]!!.forEach { dfs(it, graph, visited) }
    }

    private fun getReachable(from: RegisterWrapper, graph: Map<RegisterWrapper, Set<RegisterWrapper>>): Set<RegisterWrapper> {
        val visited: MutableSet<RegisterWrapper> = mutableSetOf()
        dfs(from, graph, visited)
        return visited
    }

    private fun transitiveSymmetricClosure(graph: Map<RegisterWrapper, Set<RegisterWrapper>>): Map<RegisterWrapper, Set<RegisterWrapper>> {
        val symmetric = symmetricClosure(graph)
        return symmetric.mapValues { (k, _) -> getReachable(k, symmetric) - setOf(k) }
    }

    private fun symmetricClosure(
        graph: Map<RegisterWrapper, Set<RegisterWrapper>>,
    ): MutableMap<RegisterWrapper, MutableSet<RegisterWrapper>> {
        val result = graph.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap()
        graph.forEach { (k, v) ->
            v.forEach {
                if (result[it] == null) result[it] = mutableSetOf()
                result[it]!!.add(k)
            }
        }
        return result
    }

    private class RegisterWrapper(val register: Register)
}
