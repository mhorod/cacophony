package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.generation.*
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.ClosureAnalysisResult
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition

class FunctionHandlerImpl(
    private val function: FunctionDefinition,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<CallableHandler>, // first (or last?) can be Lambda, not Function
    callConvention: CallConvention,
    private val variablesMap: VariablesMap,
    private val escapeAnalysisResult: EscapeAnalysisResult,
    private val closureAnalysisResult: ClosureAnalysisResult,
    escapeAnalysis: EscapeAnalysisResult,
) :  FunctionHandler, CallableHandlerImpl(
        callConvention,
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

    override fun getFunctionDeclaration(): FunctionDefinition = function

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

    override fun generateStaticLinkVariable(callerFunction: FunctionHandler): CFGNode =
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
        TODO("Not yet implemented")

        /*
        val analyzedVariable = analyzedFunction.variables.find { it.origin == variable }
        val definedInFunctionHandler =
            if (analyzedVariable != null) {
                (ancestorFunctionHandlers + this).find { it.getFunctionDeclaration() == analyzedVariable.definedIn }
            } else {
                (ancestorFunctionHandlers + this).find { it.getStaticLink() == variable }
            }

        if (definedInFunctionHandler == null) {
            throw GenerateVariableAccessException("Function $function has no access to $variable.")
        }

        val definedInDeclaration = definedInFunctionHandler.getFunctionDeclaration()

        return when (val variableAllocation = definedInFunctionHandler.getVariableAllocation(variable)) {
            is VariableAllocation.InRegister -> {
                RegisterUse(variableAllocation.register, variable.holdsReference)
            }

            is VariableAllocation.OnStack -> {
                MemoryAccess(
                    CFGNode.Subtraction(
                        generateAccessToFramePointer(definedInDeclaration),
                        CFGNode.ConstantKnown(variableAllocation.offset),
                    ),
                    variable.holdsReference,
                )
            }
            is VariableAllocation.ViaPointer -> TODO()
        }

         */
    }
}
