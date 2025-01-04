package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.semantic.syntaxtree.Expression

/**
 * Converts assignment into CFG
 */
internal class AssignmentHandler(private val cfgGenerator: CFGGenerator) {
    internal fun generateAssignment(
        layout: Layout,
        value: Expression,
        mode: EvalMode,
        context: Context,
        propagate: Boolean,
    ): SubCFG =
        when (val valueCFG = cfgGenerator.visit(value, EvalMode.Value, context)) {
            // dark magic - this way it is compatible with previous tests
            is SubCFG.Immediate -> {
                if (layout is SimpleLayout) {
                    require(layout.access is CFGNode.LValue)
                    val source = valueCFG.access
                    require(source is SimpleLayout)
                    SubCFG.Immediate(SimpleLayout(CFGNode.Assignment(layout.access, source.access)))
                } else {
                    val assignment =
                        cfgGenerator.assignLayoutWithValue(
                            valueCFG.access,
                            layout,
                            noOpOr(if (propagate) layout else SimpleLayout(CFGNode.UNIT), mode),
                        )
                    cfgGenerator.ensureExtracted(valueCFG, EvalMode.SideEffect) merge assignment
                }
            }
            is SubCFG.Extracted -> {
                val assignment =
                    cfgGenerator.assignLayoutWithValue(
                        valueCFG.access,
                        layout,
                        noOpOr(if (propagate) layout else SimpleLayout(CFGNode.UNIT), mode),
                    )
                valueCFG merge assignment
            }
        }
}
