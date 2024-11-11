package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.SourceVariable
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Expression

internal class AssignmentHandler(private val cfgGenerator: CFGGenerator) {
    internal fun generateAssignment(
        variable: Definition,
        value: Expression,
        mode: EvalMode,
        propagate: Boolean,
    ): SubCFG {
        val variableAccess = cfgGenerator.getCurrentFunctionHandler().generateVariableAccess(SourceVariable(variable))
        val valueCFG = cfgGenerator.visit(value, EvalMode.Value)
        val variableWrite = CFGNode.Assignment(variableAccess, valueCFG.access)
        return when (valueCFG) {
            is SubCFG.Immediate -> SubCFG.Immediate(variableWrite)
            is SubCFG.Extracted -> {
                val writeVertex = cfgGenerator.addVertex(variableWrite)
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
