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
        val variableLayout = getVariableLayout(variable)
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

    private fun getVariableLayout(variable: Variable): Layout =
        when (variable) {
            is Variable.PrimitiveVariable -> SimpleLayout(handler.generateVariableAccess(variable))
            is Variable.StructVariable -> StructLayout(variable.fields.mapValues { (_, subfield) -> getVariableLayout(subfield) })
            // TODO: These two should be removed
            is Variable.SourceVariable -> SimpleLayout(handler.generateVariableAccess(variable))
            is Variable.AuxVariable.StaticLinkVariable -> SimpleLayout(handler.generateVariableAccess(variable))
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
