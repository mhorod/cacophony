package cacophony.controlflow.generation

import cacophony.controlflow.CFGFragment
import cacophony.controlflow.CFGLabel
import cacophony.controlflow.CFGNode
import cacophony.controlflow.CFGVertex
import cacophony.controlflow.Register
import org.assertj.core.api.Assertions.assertThat

/**
 * Assert that two CFGs are equivalent
 * Two fragments are considered equivalent when:
 *   - they have the exact same graph structure up to label names
 *   - the CFG nodes are equivalent up to virtual register names
 */
internal fun assertEquivalent(actual: ProgramCFG, expected: ProgramCFG) {
    assertThat(actual.keys).isEqualTo(expected.keys)
    actual.entries.forEach { (function, cfg) -> assertFragmentIsEquivalent(cfg, expected[function]!!) }
}

fun assertFragmentIsEquivalent(actual: CFGFragment, expected: CFGFragment) {
    FragmentEquivalenceVisitor().visit(actual, expected)
}

private class FragmentEquivalenceVisitor {
    private val labelMapping = mutableMapOf<CFGLabel, CFGLabel>()
    private val registerMapping = mutableMapOf<Register, Register>()

    private val reverseLabelMapping = mutableMapOf<CFGLabel, CFGLabel>()
    private val reverseRegisterMapping = mutableMapOf<Register, Register>()

    private val visited = mutableSetOf<CFGVertex>()

    private var actualCFG = mapOf<CFGLabel, CFGVertex>()
    private var expectedCFG = mapOf<CFGLabel, CFGVertex>()

    fun visit(actual: CFGFragment, expected: CFGFragment) {
        mapLabels(actual.initialLabel, expected.initialLabel)
        actualCFG = actual.vertices
        expectedCFG = expected.vertices
        visit(actual.vertices[actual.initialLabel]!!, expected.vertices[expected.initialLabel]!!)
    }

    fun visit(actual: CFGVertex, expected: CFGVertex) {
        if (visited.contains(expected)) return
        visited.add(expected)

        when (expected) {
            is CFGVertex.Final -> {
                assertThat(actual).isInstanceOf(CFGVertex.Final::class.java)

                visit(actual.tree, expected.tree)
            }

            is CFGVertex.Jump -> {
                assertThat(actual).isInstanceOf(CFGVertex.Jump::class.java)
                check(actual is CFGVertex.Jump)
                mapLabels(actual.destination, expected.destination)

                visit(actual.tree, expected.tree)
                visit(actualCFG[actual.destination]!!, expectedCFG[expected.destination]!!)
            }

            is CFGVertex.Conditional -> {
                assertThat(actual).isInstanceOf(CFGVertex.Conditional::class.java)
                check(actual is CFGVertex.Conditional)
                mapLabels(actual.trueDestination, expected.trueDestination)
                mapLabels(actual.falseDestination, expected.falseDestination)

                visit(actual.tree, expected.tree)
                visit(actualCFG[actual.trueDestination]!!, expectedCFG[expected.trueDestination]!!)
                visit(actualCFG[actual.falseDestination]!!, expectedCFG[expected.falseDestination]!!)
            }
        }
    }

    private fun visit(actual: CFGNode, expected: CFGNode) {
        if (actual === expected)
            return
        when (expected) {
            is CFGNode.Assignment -> {
                assertThat(actual).isInstanceOf(CFGNode.Assignment::class.java)
                check(actual is CFGNode.Assignment)
                visit(actual.destination, expected.destination)
                visit(actual.value, expected.value)
            }

            is CFGNode.MemoryAccess -> {
                assertThat(actual).isInstanceOf(CFGNode.MemoryAccess::class.java)
                check(actual is CFGNode.MemoryAccess)
                visit(actual.destination, expected.destination)
            }

            is CFGNode.RegisterSlot -> {
                assertThat(actual).isInstanceOf(CFGNode.RegisterSlot::class.java)
                check(actual is CFGNode.RegisterSlot)
            }

            is CFGNode.RegisterUse -> {
                assertThat(actual).isInstanceOf(CFGNode.RegisterUse::class.java)
                check(actual is CFGNode.RegisterUse)
                mapRegisters(actual.register, expected.register)
            }

//            is CFGNode.Function -> {
//                assertThat(actual).isInstanceOf(CFGNode.Function::class.java)
//                check(actual is CFGNode.Function)
//                assertThat(actual.function).isEqualTo(expected.function)
//            }
//
//            is CFGNode.Call -> {
//                assertThat(actual).isInstanceOf(CFGNode.Call::class.java)
//                check(actual is CFGNode.Call)
//                assertThat(actual.functionRef).isEqualTo(expected.functionRef)
//            }

            is CFGNode.RawCall -> {
                assertThat(actual).isInstanceOf(CFGNode.RawCall::class.java)
                check(actual is CFGNode.RawCall)
                assertThat(actual.label).isEqualTo(expected.label)
            }

            is CFGNode.Comment -> {
                assertThat(actual).isInstanceOf(CFGNode.Comment::class.java)
                check(actual is CFGNode.Comment)
                assertThat(actual.comment).isEqualTo(expected.comment)
            }

            is CFGNode.Constant -> {
                assertThat(actual).isInstanceOf(CFGNode.Constant::class.java)
                check(actual is CFGNode.Constant)
                assertThat(actual.value).isEqualTo(expected.value)
            }

            CFGNode.NoOp -> {
                assertThat(expected).isInstanceOf(CFGNode.NoOp::class.java)
            }

            is CFGNode.Pop -> {
                assertThat(actual).isInstanceOf(CFGNode.Pop::class.java)
                check(actual is CFGNode.Pop)
                assertThat(actual.register).isEqualTo(expected.register)
            }

            is CFGNode.Return -> {
                assertThat(actual).isInstanceOf(CFGNode.Return::class.java)
                check(actual is CFGNode.Return)
                assertThat(actual.resultSize).isEqualTo(expected.resultSize)
            }

            is CFGNode.Push -> {
                assertThat(actual).isInstanceOf(CFGNode.Push::class.java)
                check(actual is CFGNode.Push)
                visit(actual.value, expected.value)
            }

            is CFGNode.ConstantSlot -> {
                assertThat(actual).isInstanceOf(CFGNode.ConstantSlot::class.java)
                check(actual is CFGNode.ConstantSlot)
            }

            is CFGNode.ValueSlot -> {
                assertThat(actual).isInstanceOf(CFGNode.ValueSlot::class.java)
                check(actual is CFGNode.ValueSlot)
            }

//            is CFGNode.FunctionSlot -> {
//                assertThat(actual).isInstanceOf(CFGNode.FunctionSlot::class.java)
//                check(actual is CFGNode.FunctionSlot)
//            }

            is CFGNode.NodeSlot<*> -> {
            }

            is CFGNode.Addition -> {
                assertThat(actual).isInstanceOf(CFGNode.Addition::class.java)
                check(actual is CFGNode.Addition)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.AdditionAssignment -> {
                assertThat(actual).isInstanceOf(CFGNode.AdditionAssignment::class.java)
                check(actual is CFGNode.AdditionAssignment)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.DivisionAssignment -> {
                assertThat(actual).isInstanceOf(CFGNode.DivisionAssignment::class.java)
                check(actual is CFGNode.DivisionAssignment)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.ModuloAssignment -> {
                assertThat(actual).isInstanceOf(CFGNode.ModuloAssignment::class.java)
                check(actual is CFGNode.ModuloAssignment)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.MultiplicationAssignment -> {
                assertThat(actual).isInstanceOf(CFGNode.MultiplicationAssignment::class.java)
                check(actual is CFGNode.MultiplicationAssignment)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.SubtractionAssignment -> {
                assertThat(actual).isInstanceOf(CFGNode.SubtractionAssignment::class.java)
                check(actual is CFGNode.SubtractionAssignment)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.Division -> {
                assertThat(actual).isInstanceOf(CFGNode.Division::class.java)
                check(actual is CFGNode.Division)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.Minus -> {
                assertThat(actual).isInstanceOf(CFGNode.Minus::class.java)
                check(actual is CFGNode.Minus)
                visit(actual.value, expected.value)
            }

            is CFGNode.Modulo -> {
                assertThat(actual).isInstanceOf(CFGNode.Modulo::class.java)
                check(actual is CFGNode.Modulo)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.Multiplication -> {
                assertThat(actual).isInstanceOf(CFGNode.Multiplication::class.java)
                check(actual is CFGNode.Multiplication)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.Subtraction -> {
                assertThat(actual).isInstanceOf(CFGNode.Subtraction::class.java)
                check(actual is CFGNode.Subtraction)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.Equals -> {
                assertThat(actual).isInstanceOf(CFGNode.Equals::class.java)
                check(actual is CFGNode.Equals)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.Greater -> {
                assertThat(actual).isInstanceOf(CFGNode.Greater::class.java)
                check(actual is CFGNode.Greater)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.GreaterEqual -> {
                assertThat(actual).isInstanceOf(CFGNode.GreaterEqual::class.java)
                check(actual is CFGNode.GreaterEqual)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.Less -> {
                assertThat(actual).isInstanceOf(CFGNode.Less::class.java)
                check(actual is CFGNode.Less)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.LessEqual -> {
                assertThat(actual).isInstanceOf(CFGNode.LessEqual::class.java)
                check(actual is CFGNode.LessEqual)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.LogicalNot -> {
                assertThat(actual).isInstanceOf(CFGNode.LogicalNot::class.java)
                check(actual is CFGNode.LogicalNot)
                visit(actual.value, expected.value)
            }

            is CFGNode.NotEquals -> {
                assertThat(actual).isInstanceOf(CFGNode.NotEquals::class.java)
                check(actual is CFGNode.NotEquals)
                visit(actual.lhs, expected.lhs)
                visit(actual.rhs, expected.rhs)
            }

            is CFGNode.DataLabel -> {
                assertThat(actual).isInstanceOf(CFGNode.DataLabel::class.java)
                check(actual is CFGNode.DataLabel)
            }

            is CFGNode.Call -> TODO()
        }
    }

    fun mapLabels(actual: CFGLabel, expected: CFGLabel) {
        if (labelMapping[actual] != null && labelMapping[actual] != expected) {
            throw AssertionError("Label mismatch, expected $actual to be $expected, but was ${labelMapping[actual]}")
        } else if (reverseLabelMapping[expected] != null && reverseLabelMapping[expected] != actual) {
            throw AssertionError("Label mismatch, expected $expected to be $actual, but was ${reverseLabelMapping[expected]}")
        } else {
            labelMapping[actual] = expected
            reverseLabelMapping[expected] = actual
        }
    }

    fun mapRegisters(actual: Register, expected: Register) {
        if (actual !is Register.VirtualRegister || expected !is Register.VirtualRegister) {
            assertThat(actual).isEqualTo(expected)
            return
        }

        if (registerMapping[actual] != null && registerMapping[actual] != expected) {
            throw AssertionError("Virtual register mismatch, expected $actual to be $expected, but was ${registerMapping[actual]}")
        } else if (reverseRegisterMapping[expected] != null && reverseRegisterMapping[expected] != actual) {
            throw AssertionError("Virtual register mismatch, expected $expected to be $actual, but was ${reverseRegisterMapping[expected]}")
        } else {
            registerMapping[actual] = expected
            reverseRegisterMapping[expected] = actual
        }
    }
}
