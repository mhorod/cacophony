package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.CFGNode.MemoryAccess
import cacophony.controlflow.CFGNode.RegisterUse
import cacophony.controlflow.generation.Layout
import cacophony.controlflow.generation.SimpleLayout
import cacophony.controlflow.generation.flattenLayout
import cacophony.controlflow.generation.getVariableLayout
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import kotlin.math.max

class FunctionHandlerImpl(
    private val function: FunctionDefinition,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>,
    callConvention: CallConvention,
    private val variablesMap: VariablesMap,
) : FunctionHandler {
    private val staticLink = Variable.PrimitiveVariable("sl")
    private var stackSpace = 0
    private val variableAllocation: MutableMap<Variable.PrimitiveVariable, VariableAllocation> = mutableMapOf()

    // This does not perform any checks and may override previous allocation.
    // Holes in the stack may be also created if stack variables are directly allocated with this method.
    override fun registerVariableAllocation(variable: Variable.PrimitiveVariable, allocation: VariableAllocation) {
        if (allocation is VariableAllocation.OnStack) {
            stackSpace = max(stackSpace, allocation.offset + REGISTER_SIZE)
        }
        variableAllocation[variable] = allocation
    }

    override fun allocateFrameVariable(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        val currentStackSpace = stackSpace
        registerVariableAllocation(variable, VariableAllocation.OnStack(stackSpace))
        return MemoryAccess(
            CFGNode.Subtraction(
                RegisterUse(Register.FixedRegister(HardwareRegister.RBP)),
                CFGNode.ConstantKnown(currentStackSpace),
            ),
        )
    }

    init {
        introduceStaticLinksParams()

        run {
            val usedVars = analyzedFunction.variablesUsedInNestedFunctions.filterIsInstance<Variable.PrimitiveVariable>()
            val regVar =
                (
                    analyzedFunction
                        .declaredVariables()
                        .map { it.origin.getPrimitives() }
                        .flatten() union
                        function.arguments
                            .map { variablesMap.definitions[it]!! }
                            .map { it.getPrimitives() }
                            .flatten()
                ).toSet()
                    .minus(usedVars.toSet())

            regVar.forEach {
                registerVariableAllocation(
                    it,
                    VariableAllocation.InRegister(Register.VirtualRegister()),
                )
            }

            usedVars.forEach {
                allocateFrameVariable(it)
            }
        }
    }

    override fun getFunctionDeclaration(): FunctionDefinition = function

    private fun traverseStaticLink(depth: Int): CFGNode =
        if (depth == 0) {
            RegisterUse(Register.FixedRegister(HardwareRegister.RBP))
        } else {
            MemoryAccess(traverseStaticLink(depth - 1))
        }

    override fun generateAccessToFramePointer(other: FunctionDefinition): CFGNode =
        if (other == function) {
            traverseStaticLink(0)
        } else {
            traverseStaticLink(
                ancestorFunctionHandlers.indexOfFirst { it.getFunctionDeclaration() == other } + 1,
            )
        }

    override fun generateStaticLinkVariable(callerFunction: FunctionHandler): CFGNode =
        // Since staticLink is not property of node itself, but rather of its children,
        // if caller is immediate parent, we have to fetch RBP instead.
        if (ancestorFunctionHandlers.isEmpty() || callerFunction === ancestorFunctionHandlers.first()) {
            RegisterUse(Register.FixedRegister(HardwareRegister.RBP))
        } else {
            callerFunction.generateAccessToFramePointer(ancestorFunctionHandlers.first().getFunctionDeclaration())
        }

    override fun generateVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        if (variable in variableAllocation) { // WARN: hack to handle arguments not used inside function
            return wrapAllocation(variableAllocation[variable]!!)
        }
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
                RegisterUse(variableAllocation.register)
            }

            is VariableAllocation.OnStack -> {
                MemoryAccess(
                    CFGNode.Subtraction(
                        generateAccessToFramePointer(definedInDeclaration),
                        CFGNode.ConstantKnown(variableAllocation.offset),
                    ),
                )
            }
        }
    }

    override fun getVariableAllocation(variable: Variable.PrimitiveVariable): VariableAllocation =
        variableAllocation.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable have not been allocated inside $this FunctionHandler")
        }

    override fun getVariableFromDefinition(varDef: Definition): Variable =
        variablesMap.definitions.getOrElse(varDef) {
            throw IllegalArgumentException("Variable $varDef have not been defined inside function $function")
        }

    override fun getStaticLink(): Variable.PrimitiveVariable = staticLink

    override fun getStackSpace(): CFGNode.ConstantLazy = CFGNode.ConstantLazy { stackSpace }

    private val resultRegister = Register.VirtualRegister()

    override fun getResultRegister(): Register.VirtualRegister = resultRegister

    override fun getResultLayout(): Layout = SimpleLayout(registerUse(getResultRegister()))

    private fun getFlattenedArguments(): List<CFGNode> =
        function.arguments
            .map { variablesMap.definitions[it]!! }
            .map {
                getVariableLayout(
                    this,
                    it,
                )
            }.map { flattenLayout(it) }
            .flatten() + generateVariableAccess(getStaticLink())

    private val prologueEpilogueHandler =
        PrologueEpilogueHandler(
            this,
            callConvention,
            getStackSpace(),
            getFlattenedArguments(),
            getResultLayout(),
        )

    override fun generatePrologue(): List<CFGNode> = prologueEpilogueHandler.generatePrologue()

    override fun generateEpilogue(): List<CFGNode> = prologueEpilogueHandler.generateEpilogue()

    // Creates staticLink auxVariable in analyzedFunction, therefore shouldn't be called multiple times.
    // Static link is created even if parent doesn't exist.
    private fun introduceStaticLinksParams() {
        registerVariableAllocation(
            staticLink,
            VariableAllocation.OnStack(0),
        )
        analyzedFunction.auxVariables.add(staticLink)
    }
}
