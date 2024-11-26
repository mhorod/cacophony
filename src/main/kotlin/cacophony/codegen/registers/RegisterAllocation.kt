package cacophony.codegen.registers

import cacophony.controlflow.HardwareRegister
import cacophony.controlflow.HardwareRegisterMapping
import cacophony.controlflow.Register
import cacophony.utils.getTransitiveClosure

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
fun allocateRegisters(liveness: Liveness, allowedRegisters: Set<HardwareRegister>): RegisterAllocation {
    val allocation = RegisterAllocator(liveness, allowedRegisters).allocate()
    allocation.validate(liveness, allowedRegisters)
}

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

    private val graph =
        symmetricClosure(
            liveness.interference
                .mapValues { (_, v) -> v.toMutableSet() },
        )

    private val originalGraph: Map<Register, Set<Register>> = symmetricClosure(liveness.interference)

    init {
        allowedRegisters.map { Register.FixedRegister(it) }.forEach { r ->
            graph.getOrPut(r) {
                mutableSetOf()
            }.addAll(allowedRegisters.filter { it != r.hardwareRegister }.map { Register.FixedRegister(it) })
        }
    }

    private val registers = liveness.allRegisters.toMutableSet()
    private val copying = transitiveSymmetricClosure(liveness.copying)
    private val stack = mutableListOf<Set<Register>>()
    private val k = allowedRegisters.size

    init {
        if (liveness.allRegisters.any { it is Register.FixedRegister && it.hardwareRegister !in allowedRegisters }) {
            throw IllegalArgumentException("Unexpected hardware register")
        }
        for ((register, interferences) in liveness.interference)
            if (register in interferences)
                throw IllegalArgumentException("Register cannot interfere with itself")
    }

    private val copyGroups = mutableMapOf<Register, Set<Register>>()

    private fun originalNeighbors(r: Register) = originalGraph[r] ?: emptySet()

    private fun neighbors(r: Register) = graph[r] ?: emptySet()

    private fun copies(r: Register) = (copying[r] ?: emptySet()).filter { it in registers }

    private fun deposit(r: Register) {
        stack.add(copyGroup(r))
        registers.removeAll(copyGroup(r))
        graph[r]?.forEach {
            graph[it]?.remove(r)
        }
    }

    private fun copyGroup(r: Register) = (copyGroups[r] ?: setOf()) + setOf(r)

    private fun coalesce(a: Register, b: Register) {
        copyGroups[a] = copyGroup(a) + copyGroup(b)
        registers.remove(b)
        graph[b]?.forEach {
            graph[it]?.remove(b)
            graph[it]?.add(a)
        }
        graph.getOrPut(a) { mutableSetOf() }.addAll(graph[b] ?: emptySet())
    }

    private fun generateFirstFitOrder() {
        while (registers.isNotEmpty()) {
            while (true) {
                val y =
                    registers
                        .map { it to registerToCoalesce(it) }
                        .firstOrNull { it.second != null }
                        ?.also { (a, b) -> coalesce(a, b!!) }
                if (y != null) continue

                val x = registers.find { neighbors(it).size < k && registerToCoalesce(it) == null }?.also { deposit(it) }
                if (x == null) break
            }
            if (registers.isNotEmpty())
                deposit(registers.minBy { if (it is Register.FixedRegister) Int.MAX_VALUE else neighbors(it).size })
        }
    }

    private fun doColoring(): RegisterAllocation {
        val coloring = mutableMapOf<Register, HardwareRegister>()
        val spills = mutableSetOf<Register>()
        stack.forEach { group ->
            val fixedRegister = group.find { it is Register.FixedRegister }
            if (fixedRegister != null) {
                group.forEach {
                    coloring[it] = (fixedRegister as Register.FixedRegister).hardwareRegister
                }
            }
        }
        stack.reversed().forEach { group ->
            if (group.any { it is Register.FixedRegister }) return@forEach
            val forbiddenColors = group.map { r -> originalNeighbors(r).mapNotNull { coloring[it] } }.flatten().toSet()
            val color = (allowedRegisters - forbiddenColors).firstOrNull()
            if (color == null) spills.addAll(group)
            else group.forEach { coloring[it] = color }
        }
        return RegisterAllocation(coloring.filterKeys { it in liveness.allRegisters }, spills)
    }

    private fun registerToCoalesce(r: Register) = copies(r).find { shouldCoalesce(r, it) }

    // Uses George criterion for safety
    private fun isCoalesceSafe(a: Register, b: Register) =
        neighbors(a).all {
            it in neighbors(b) ||
                neighbors(it).size < allowedRegisters.size
        }

    private fun shouldCoalesce(a: Register, b: Register): Boolean =
        b !in neighbors(a) &&
            b in copies(a) &&
            isCoalesceSafe(a, b)

    private fun <A> transitiveSymmetricClosure(graph: Map<A, Set<A>>): Map<A, Set<A>> {
        val symmetric = symmetricClosure(graph)
        return getTransitiveClosure(symmetric).mapValues { (k, v) -> v - setOf(k) }
    }

    private fun <A> symmetricClosure(graph: Map<A, Set<A>>): MutableMap<A, MutableSet<A>> {
        val result = graph.mapValues { (_, v) -> v.toMutableSet() }.toMutableMap()
        graph.forEach { (k, v) ->
            v.forEach { result.getOrPut(it) { mutableSetOf() }.add(k) }
        }
        return result
    }
}

fun RegisterAllocation.validate(liveness: Liveness, allowedRegisters: Set<HardwareRegister>) {
    require(spills union successful.keys == liveness.allRegisters) { "Spills and successful registers do not cover all registers" }
    require((spills intersect successful.keys).isEmpty()) { "Spills and successful registers intersect" }
    require(successful.values.all { it in allowedRegisters }) { "Not allowed register was used" }

    for ((reg1, interferences) in liveness.interference.entries) {
        for (reg2 in interferences) {
            val hw1 = successful[reg1]
            val hw2 = successful[reg2]
            require(hw1 == null || hw2 == null || hw1 != hw2)
        }
    }

    for (fixedRegister in liveness.allRegisters.filterIsInstance<Register.FixedRegister>()) {
        require(successful.containsKey(fixedRegister)) { "Fixed register was not allocated" }
    }
}
