package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.flattenLayout
import cacophony.controlflow.generation.getVariableLayout
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.ClosureAnalysisResult
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.LambdaExpression

class LambdaHandlerImpl(
    callConvention: CallConvention,
    private val function: LambdaExpression, // TODO: it can be FunctionalExpression
    private val analyzedFunction: AnalyzedFunction,
    private val variablesMap: VariablesMap,
    escapeAnalysisResult: EscapeAnalysisResult,
    closureAnalysisResult: ClosureAnalysisResult,
) : LambdaHandler, CallableHandlerImpl(
        analyzedFunction,
        function,
        variablesMap,
        escapeAnalysisResult,
    ) {
    // I'm not sure
    override val referenceOffsets = mutableListOf(0)

    private val closureLink = Variable.PrimitiveVariable("cl", true)

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

    // copied from FunctionHandler
    private fun introduceClosureLinkParam() {
        registerVariableAllocation(
            closureLink,
            VariableAllocation.OnStack(REGISTER_SIZE),
        )
        analyzedFunction.auxVariables.add(closureLink)
    }

    private val prologueEpilogueHandler: PrologueEpilogueHandler

    private val offsetsMap: Map<Variable, Int> =
        closureAnalysisResult[function]!!
            .filterIsInstance<Variable.PrimitiveVariable>()
            .mapIndexed { index, it ->
                it to index * REGISTER_SIZE
            }.toMap()

    init {
        introduceClosureLinkParam()
        allocateVariables()
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

    override fun getBodyReference() = function

    override fun getClosureLink() = closureLink

    override fun getCapturedVariableOffsets() = offsetsMap // TODO: somebody need to allocate closure using this map

    override fun generateOutsideVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        val offset =
            offsetsMap.getOrElse(variable) {
                throw GenerateVariableAccessException("Variable $variable is not in the closure of $function.")
            }
        return wrapAllocation(
            VariableAllocation.ViaPointer(
                VariableAllocation.ViaPointer(
                    getVariableAllocation(closureLink),
                    offset,
                ),
                0,
            ),
            true,
        )
    }
}
