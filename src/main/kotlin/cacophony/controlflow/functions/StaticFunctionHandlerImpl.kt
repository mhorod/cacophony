package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.*
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.LambdaExpression

class StaticFunctionHandlerImpl(
    private val function: LambdaExpression,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<CallableHandler>,
    callConvention: CallConvention,
    private val variablesMap: VariablesMap,
    escapeAnalysisResult: EscapeAnalysisResult,
) : StaticFunctionHandler, CallableHandlerImpl(
        analyzedFunction,
        function,
        variablesMap,
        escapeAnalysisResult,
    ) {
    private val staticLink = Variable.PrimitiveVariable("sl")

    // Initialized with 0, because rbp points to caller rbp, which we treat as a reference, except for main function.
    override val referenceOffsets = if (ancestorFunctionHandlers.isNotEmpty()) mutableListOf(0) else mutableListOf()

    // Creates staticLink auxVariable in analyzedFunction, therefore shouldn't be called multiple times.
    // Static link is created even if parent doesn't exist.
    private fun introduceStaticLinksParams() {
        registerVariableAllocation(
            staticLink,
            VariableAllocation.OnStack(REGISTER_SIZE),
        )
        analyzedFunction.auxVariables.add(staticLink)
    }

    private fun traverseStaticLink(depth: Int): CFGNode =
        if (depth == 0) {
            registerUse(rbp, false)
        } else {
            memoryAccess(traverseStaticLink(depth - 1) sub integer(REGISTER_SIZE), false)
        }

    override fun generateAccessToFramePointer(other: CallableHandler): CFGNode =
        if (other == this) {
            traverseStaticLink(0)
        } else {
            traverseStaticLink(
                ancestorFunctionHandlers.indexOfFirst { it == other } + 1,
            )
        }

    private val prologueEpilogueHandler: PrologueEpilogueHandler

    init {
        introduceStaticLinksParams()
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

    override fun getStaticLink(): Variable.PrimitiveVariable = staticLink

    override fun generateStaticLinkVariable(callerFunction: CallableHandler): CFGNode =
        // Since staticLink is not property of node itself, but rather of its children,
        // if caller is an immediate parent, we have to fetch RBP instead.
        if (ancestorFunctionHandlers.isEmpty() || callerFunction === ancestorFunctionHandlers.first()) {
            registerUse(rbp, false)
        } else {
            callerFunction.generateAccessToFramePointer(ancestorFunctionHandlers.first())
        }

    override fun getFlattenedArguments(): List<CFGNode> =
        function.arguments
            .map { variablesMap.definitions[it]!! }
            .map {
                getVariableLayout(
                    this,
                    it,
                )
            }.map { flattenLayout(it) }
            .flatten() + generateVariableAccess(getStaticLink())

    override fun generateOutsideVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        val definedInFunctionHandler =
            ancestorFunctionHandlers.find {
                it.hasVariableAllocation(variable)
            }

        if (definedInFunctionHandler == null) {
            throw GenerateVariableAccessException("Function $function has no access to $variable.")
        }

        return generateVariableAccessFrom(variable, definedInFunctionHandler)
    }

    private fun generateVariableAccessFrom(variable: Variable.PrimitiveVariable, handler: CallableHandler): CFGNode.LValue =
        when (val variableAllocation = handler.getVariableAllocation(variable)) {
            is VariableAllocation.InRegister -> {
                CFGNode.RegisterUse(variableAllocation.register, variable.holdsReference)
            }

            is VariableAllocation.OnStack -> {
                CFGNode.MemoryAccess(
                    CFGNode.Subtraction(
                        generateAccessToFramePointer(handler),
                        CFGNode.ConstantKnown(variableAllocation.offset),
                    ),
                    variable.holdsReference,
                )
            }

            is VariableAllocation.ViaPointer -> {
                CFGNode.MemoryAccess(
                    CFGNode.Addition(
                        generateVariableAccessFrom(
                            handler.getPointerToHeapVariable(variable),
                            handler,
                        ),
                        CFGNode.ConstantKnown(variableAllocation.offset),
                    ),
                )
            }
        }

    override fun getFunctionLabel(): String = function.getLabel()
}
