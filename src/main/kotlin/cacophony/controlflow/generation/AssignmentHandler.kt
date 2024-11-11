package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Variable
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression

internal class AssignmentHandler(private val cfg: CFG, private val cfgGenerator: CFGGenerator) {
    internal fun generateAssignment(
        variable: Definition,
        value: Expression,
        mode: EvalMode,
        context: Context,
        propagate: Boolean,
    ): SubCFG {
        // TODO: consider generalizing assigning so it works not only for variables
        val variableAccess = cfgGenerator.getCurrentFunctionHandler().generateVariableAccess(Variable.SourceVariable(variable))
        val valueCFG = cfgGenerator.visit(value, EvalMode.Value, context)
        val variableWrite = CFGNode.Assignment(variableAccess, valueCFG.access)
        return when (valueCFG) {
            is SubCFG.Immediate -> SubCFG.Immediate(variableWrite)
            is SubCFG.Extracted -> {
                val writeVertex = cfg.addUnconditionalVertex(variableWrite)
                valueCFG.exit.connect(writeVertex.label)
                SubCFG.Extracted(
                    valueCFG.entry,
                    writeVertex,
                    noOpOr(if (propagate) variableAccess else CFGNode.UNIT, mode),
                )
            }
        }
    }
}
