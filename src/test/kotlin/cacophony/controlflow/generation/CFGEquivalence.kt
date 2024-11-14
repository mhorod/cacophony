package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGVertex
import cacophony.controlflow.Register
import org.assertj.core.api.Assertions.assertThat

/**
 * Assert that two CFGs are equivalent
 * Two fragments are considered equivalent when:
 *   - they have the exact same graph structure up to label names
 *   - the CFG nodes are equivalent up to virtual register names
 */
internal fun assertEquivalent(
    actual: ProgramCFG,
    expected: ProgramCFG,
) {
    assertThat(actual.keys).isEqualTo(expected.keys)
    actual.entries.forEach { (function, cfg) -> assertFragmentIsEquivalent(cfg, expected[function]!!) }
}

private fun assertFragmentIsEquivalent(
    actual: CFGFragment,
    expected: CFGFragment,
) {
    FragmentEquivalenceVisitor().visit(actual, expected)
}

private class FragmentEquivalenceVisitor {
    private val labelMapping = mutableMapOf<CFGLabel, CFGLabel>()
    private val registerMapping = mutableMapOf<Register.VirtualRegister, Register.VirtualRegister>()
    private val visited = mutableSetOf<CFGVertex>()

    private var actualCFG = mapOf<CFGLabel, CFGVertex>()
    private var expectedCFG = mapOf<CFGLabel, CFGVertex>()

    fun visit(
        actual: CFGFragment,
        expected: CFGFragment,
    ) {
        mapLabels(actual.initialLabel, expected.initialLabel)
        actualCFG = actual.vertices
        expectedCFG = expected.vertices
        visit(actual.vertices[actual.initialLabel]!!, expected.vertices[expected.initialLabel]!!)
    }

    fun visit(
        actual: CFGVertex,
        expected: CFGVertex,
    ) {
        if (visited.contains(actual)) return
        visited.add(actual)

        when (actual) {
            is CFGVertex.Final -> {
                assertThat(expected).isInstanceOf(CFGVertex.Final::class.java)
                assertThat(actual.tree).isEqualTo(expected.tree)
            }
            is CFGVertex.Jump -> {
                assertThat(expected).isInstanceOf(CFGVertex.Jump::class.java)
                assertThat(actual.tree).isEqualTo(expected.tree)
            }
            else -> TODO()
        }
    }

    fun mapLabels(
        actual: CFGLabel,
        expected: CFGLabel,
    ) {
        if (labelMapping[actual] != null && labelMapping[actual] != expected) {
            throw AssertionError("Label mismatch, expected $actual to be $expected, but was ${labelMapping[actual]}")
        } else {
            labelMapping[actual] = expected
        }
    }

    fun mapRegisters(
        actual: Register.VirtualRegister,
        expected: Register.VirtualRegister,
    ) {
        if (registerMapping[actual] != null && registerMapping[actual] != expected) {
            throw AssertionError("Virtual register mismatch, expected $actual to be $expected, but was ${registerMapping[actual]}")
        } else {
            registerMapping[actual] = expected
        }
    }
}
