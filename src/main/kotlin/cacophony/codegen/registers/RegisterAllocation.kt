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

    private val wrappers = liveness.allRegisters.associateWith { RegisterWrapper(it) }
    private val graph =
        liveness.interference
            .map { (k, v) -> wrappers[k]!! to v.map { wrappers[it]!! }.toMutableSet() }
            .toMap()
            .toMutableMap()
    private val registers = liveness.allRegisters.map { wrappers[it]!! }.toMutableSet()
    private val copying = liveness.copying.map { (k, v) -> wrappers[k]!! to v.map { wrappers[it]!! } }.toMap()
    private val stack = mutableListOf<Set<RegisterWrapper>>()
    private val k = allowedRegisters.size
    private val predefinedRegisters = allowedRegisters.map { RegisterWrapper(Register.FixedRegister(it)) }

    init {
        if (liveness.allRegisters.any { it is Register.FixedRegister && it.hardwareRegister !in allowedRegisters }) {
            throw IllegalArgumentException("Unexpected hardware register")
        }
        for ((register, interferences) in liveness.interference)
            if (register in interferences)
                throw IllegalArgumentException("Register cannot interfere with itself")
    }

    private val copyGroups = mutableMapOf<RegisterWrapper, Set<RegisterWrapper>>()

    private fun predefinedNeighbors(r: RegisterWrapper) =
        if (r.register is Register.FixedRegister) {
            predefinedRegisters.filter { (it.register as Register.FixedRegister).hardwareRegister != r.register.hardwareRegister }.toSet()
        } else {
            emptySet()
        }

    private fun originalNeighbors(r: RegisterWrapper) =
        (liveness.interference[r.register]?.map { wrappers[it]!! }?.toSet() ?: emptySet()) + predefinedNeighbors(r)

    private fun neighbors(r: RegisterWrapper) = (graph[r] ?: emptySet()) + predefinedNeighbors(r)

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
        copyGroups[a] = copyGroup(a) + copyGroup(b)
        registers.remove(b)
        neighbors(b).forEach {
            graph[it]?.remove(b)
            graph[it]?.add(a)
        }
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
                deposit(registers.minBy { neighbors(it).size })
        }
    }

    private fun doColoring(): RegisterAllocation {
        val coloring = mutableMapOf<RegisterWrapper, HardwareRegister>()
        val spills = mutableSetOf<RegisterWrapper>()
        predefinedRegisters.forEach {
            coloring[it] = (it.register as Register.FixedRegister).hardwareRegister
        }
        stack.reversed().forEach { group ->
            val forbiddenColors = group.map { r -> originalNeighbors(r).mapNotNull { coloring[it] } }.flatten().toSet()
            val color = (allowedRegisters - forbiddenColors).firstOrNull()
            if (color == null) spills.addAll(group)
            else group.forEach { coloring[it] = color }
        }
        predefinedRegisters.forEach {
            coloring.remove(it)
        }
        return RegisterAllocation(coloring.map { (k, v) -> k.register to v }.toMap(), spills.map { it.register }.toSet())
    }

    private fun registerToCoalesce(r: RegisterWrapper) = copies(r).find { shouldCoalesce(r, it) }

    // Uses George criterion for safety
    private fun isCoalesceSafe(a: RegisterWrapper, b: RegisterWrapper) =
        neighbors(a).all {
            it in neighbors(b) ||
                neighbors(it).size < allowedRegisters.size
        }

    private fun shouldCoalesce(a: RegisterWrapper, b: RegisterWrapper) =
        b !in neighbors(a) &&
            b in copies(a) &&
            isCoalesceSafe(a, b)

    private class RegisterWrapper(val register: Register)
}
