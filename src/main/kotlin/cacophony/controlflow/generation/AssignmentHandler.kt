package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Variable
import cacophony.semantic.syntaxtree.Expression

/**
 * Converts assignment into CFG
 */
internal class AssignmentHandler(private val cfgGenerator: CFGGenerator) {
    private val handler = cfgGenerator.getCurrentFunctionHandler()

    internal fun generateAssignment(
        variable: Variable,
        value: Expression,
        mode: EvalMode,
        context: Context,
        propagate: Boolean,
    ): SubCFG {
        val variableLayout = getVariableLayout(handler, variable)

        return when (val valueCFG = cfgGenerator.visit(value, EvalMode.Value, context)) {
            // dark magic - this way it is compatible with previous tests
            is SubCFG.Immediate -> {
                if (variableLayout is SimpleLayout) {
                    require(variableLayout.access is CFGNode.LValue)
                    val source = valueCFG.access
                    require(source is SimpleLayout)
                    SubCFG.Immediate(SimpleLayout(CFGNode.Assignment(variableLayout.access, source.access), variableLayout.holdsReference))
                } else {
                    val assignment =
                        cfgGenerator.assignLayoutWithValue(
                            valueCFG.access,
                            variableLayout,
                            noOpOr(if (propagate) variableLayout else SimpleLayout(CFGNode.UNIT, false), mode),
                        )
                    cfgGenerator.ensureExtracted(valueCFG, EvalMode.SideEffect) merge assignment
                }
            }
            is SubCFG.Extracted -> {
                val assignment =
                    cfgGenerator.assignLayoutWithValue(
                        valueCFG.access,
                        variableLayout,
                        noOpOr(if (propagate) variableLayout else SimpleLayout(CFGNode.UNIT, false), mode),
                    )
                valueCFG merge assignment
            }
        }
    }
}
