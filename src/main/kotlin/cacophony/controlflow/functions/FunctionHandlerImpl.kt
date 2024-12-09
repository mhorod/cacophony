package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.CFGNode.MemoryAccess
import cacophony.controlflow.CFGNode.RegisterUse
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition
import kotlin.math.max

class FunctionHandlerImpl(
    private val function: FunctionDefinition,
    private val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>,
    callConvention: CallConvention,
) : FunctionHandler {
    private val staticLink: Variable.AuxVariable.StaticLinkVariable = Variable.AuxVariable.StaticLinkVariable()
    private val definitionToVariable =
        (analyzedFunction.variables.map { it.declaration } union function.arguments).associateWith { Variable.SourceVariable(it) }
    private var stackSpace = 0
    private val variableAllocation: MutableMap<Variable, VariableAllocation> = mutableMapOf()

    // This does not perform any checks and may override previous allocation.
    // Holes in the stack may be also created if stack variables are directly allocated with this method.
    override fun registerVariableAllocation(variable: Variable, allocation: VariableAllocation) {
        if (allocation is VariableAllocation.OnStack) {
            stackSpace = max(stackSpace, allocation.offset + REGISTER_SIZE)
        }
        variableAllocation[variable] = allocation
    }

    override fun allocateFrameVariable(variable: Variable): CFGNode.LValue {
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
            val usedVars = analyzedFunction.variablesUsedInNestedFunctions
            val regVar =
                (
                    analyzedFunction
                        .declaredVariables()
                        .map { it.declaration } union function.arguments
                ).toSet()
                    .minus(usedVars)
            regVar.forEach { varDef ->
                registerVariableAllocation(
                    definitionToVariable[varDef]!!,
                    VariableAllocation.InRegister(Register.VirtualRegister(varDef.identifier)),
                )
            }

            usedVars.forEach { varDef ->
                allocateFrameVariable(definitionToVariable[varDef]!!)
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

    override fun generateVariableAccess(variable: Variable): CFGNode.LValue {
        val definedInDeclaration =
            when (variable) {
                is Variable.SourceVariable -> {
                    analyzedFunction.variables.find { it.declaration == variable.definition }?.definedIn
                }
                is Variable.AuxVariable.StaticLinkVariable -> {
                    if (getStaticLink() == variable) {
                        function
                    } else {
                        ancestorFunctionHandlers.find { it.getStaticLink() == variable }?.getFunctionDeclaration()
                    }
                }
                else -> throw GenerateVariableAccessException(
                    "Cannot generate access to variables other than static links and source variables.",
                )
            }

        if (definedInDeclaration == null) {
            throw GenerateVariableAccessException("Function $function has no access to $variable.")
        }

        val definedInFunctionHandler =
            ancestorFunctionHandlers.find { it.getFunctionDeclaration() == definedInDeclaration } ?: this

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

    override fun getVariableAllocation(variable: Variable): VariableAllocation =
        variableAllocation.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable have not been allocated inside $this FunctionHandler")
        }

    override fun getVariableFromDefinition(varDef: Definition): Variable =
        definitionToVariable.getOrElse(varDef) {
            throw IllegalArgumentException("Variable $varDef have not been defined inside function $function")
        }

    override fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable = staticLink

    override fun getStackSpace(): CFGNode.ConstantLazy = CFGNode.ConstantLazy { stackSpace }

    private val resultRegister = Register.VirtualRegister()

    override fun getResultRegister(): Register.VirtualRegister = resultRegister

    private val prologueEpilogueHandler =
        PrologueEpilogueHandler(
            this,
            callConvention,
            getStackSpace(),
            resultRegister,
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
