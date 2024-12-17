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
            is SubCFG.Immediate -> SubCFG.Immediate(getLayoutOfAssignments(valueCFG.access, variableLayout))
            is SubCFG.Extracted -> {
                val assignment =
                    cfgGenerator.assignLayoutWithValue(
                        valueCFG.access,
                        variableLayout,
                        noOpOr(if (propagate) variableLayout else SimpleLayout(CFGNode.UNIT), mode),
                    )
                valueCFG merge assignment
            }
        }
    }

    private fun getLayoutOfAssignments(source: Layout, destination: Layout): Layout =
        when (destination) {
            is SimpleLayout -> {
                // by type checking
                require(source is SimpleLayout)
                require(destination.access is CFGNode.LValue) // TODO: remove after LValueLayout
                SimpleLayout(CFGNode.Assignment(destination.access, source.access))
            }
            is StructLayout -> {
                // by type checking
                require(source is StructLayout)
                StructLayout(destination.fields.mapValues { (field, layout) -> getLayoutOfAssignments(source.fields[field]!!, layout) })
            }
        }
}
