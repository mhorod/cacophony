package cacophony.controlflow.functions

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Variable
import cacophony.controlflow.generation.flattenLayout
import cacophony.controlflow.generation.getVariableLayout
import cacophony.controlflow.rbp
import cacophony.controlflow.registerUse
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.FunctionalExpression
import cacophony.semantic.syntaxtree.LambdaExpression

class LambdaHandlerImpl(
    callConvention: CallConvention,
    private val function: FunctionalExpression,
    private val analyzedFunction: AnalyzedFunction,
    private val variablesMap: VariablesMap,
    private val escapeAnalysisResult: EscapeAnalysisResult,
) : LambdaHandler, CallableHandlerImpl(
        callConvention,
        analyzedFunction,
        function,
        variablesMap,
        escapeAnalysisResult,
    ) {
    // I'm not sure
    override val referenceOffsets = mutableListOf(0)

    override fun getFlattenedArguments() =
        function.arguments
            .map { variablesMap.definitions[it]!! }
            .map {
                getVariableLayout(
                    this,
                    it,
                )
            }.map { flattenLayout(it) }
            .flatten() + generateVariableAccess(getClosureLink())

    override fun generateAccessToFramePointer(other: CallableHandler): CFGNode =
        if (other == this) {
            registerUse(rbp, false)
        } else {
            throw GenerateAccessToFramePointerException("Lambda $function cannot access another function frame pointer")
        }

    private val prologueEpilogueHandler: PrologueEpilogueHandler

    init {
        prologueEpilogueHandler =
            PrologueEpilogueHandler(
                this,
                callConvention,
                getStackSpace(),
                getFlattenedArguments(),
                getResultLayout(),
                getHeapVariablePointers(),
            )
    }

    override fun getPrologueEpilogueHandler() = prologueEpilogueHandler

    override fun getBodyReference(): LambdaExpression {
        TODO("Not yet implemented")
    }

    override fun getClosureLink(): Variable.PrimitiveVariable {
        TODO("Not yet implemented")
    }

    override fun getCapturedVariableOffsets(): Map<Variable, Int> {
        TODO("Not yet implemented")
    }

    override fun generateOutsideVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        TODO("Not yet implemented")
    }
}
