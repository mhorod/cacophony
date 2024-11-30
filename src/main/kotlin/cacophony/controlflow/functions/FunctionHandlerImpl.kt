package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.CFGNode.Addition
import cacophony.controlflow.CFGNode.Constant
import cacophony.controlflow.CFGNode.MemoryAccess
import cacophony.controlflow.CFGNode.RegisterUse
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.Definition.FunctionDeclaration

class FunctionHandlerImpl(
    val function: FunctionDeclaration,
    val analyzedFunction: AnalyzedFunction,
    // List of parents' handlers ordered from immediate parent.
    private val ancestorFunctionHandlers: List<FunctionHandler>,
) : FunctionHandler {
    private val staticLink: Variable.AuxVariable.StaticLinkVariable = Variable.AuxVariable.StaticLinkVariable()
    private val definitionToVariable =
        (analyzedFunction.variables.map { it.declaration } union function.arguments).associateWith { Variable.SourceVariable(it) }

    private val variableAllocation: MutableMap<Variable, VariableAllocation> =
        run {
            val res = mutableMapOf<Variable, VariableAllocation>()
            val usedVars = analyzedFunction.variablesUsedInNestedFunctions
            val regVar =
                (
                    analyzedFunction
                        .declaredVariables()
                        .map { it.declaration } union function.arguments
                ).toSet()
                    .minus(usedVars)
            regVar.forEach { varDef ->
                res[definitionToVariable[varDef]!!] = VariableAllocation.InRegister(Register.VirtualRegister())
            }
            // offset starts at 8, since static link is placed at offset 0
            var offset = REGISTER_SIZE
            usedVars.forEach { varDef ->
                res[definitionToVariable[varDef]!!] = VariableAllocation.OnStack(offset)
                offset += REGISTER_SIZE
            }
            res
        }

    init {
        introduceStaticLinksParams()
    }

    override fun registerVariableAllocation(variable: Variable, allocation: VariableAllocation) {
        variableAllocation[variable] = allocation
    }

    override fun getFunctionDeclaration(): FunctionDeclaration = function

    override fun getFunctionAnalysis(): AnalyzedFunction = analyzedFunction

    // Wrapper for generateCall that additionally fills staticLink to parent function.
    // Since staticLink is not property of node itself, but rather of its children,
    // if caller is immediate parent, we have to fetch RBP instead.
    override fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean,
    ): List<CFGNode> {
        val staticLinkVar =
            if (ancestorFunctionHandlers.isEmpty() || callerFunction === ancestorFunctionHandlers[0]) {
                RegisterUse(Register.FixedRegister(HardwareRegister.RBP))
            } else {
                callerFunction.generateAccessToFramePointer(ancestorFunctionHandlers[0].getFunctionDeclaration())
            }

        return generateCall(function, mutableListOf(staticLinkVar) + arguments, result, respectStackAlignment)
    }

    private fun traverseStaticLink(depth: Int): CFGNode =
        if (depth == 0) {
            RegisterUse(Register.FixedRegister(HardwareRegister.RBP))
        } else {
            MemoryAccess(traverseStaticLink(depth - 1))
        }

    override fun generateAccessToFramePointer(other: FunctionDeclaration): CFGNode =
        if (other == function) {
            traverseStaticLink(0)
        } else {
            traverseStaticLink(
                ancestorFunctionHandlers.indexOfFirst { it.getFunctionDeclaration() == other } + 1,
            )
        }

    override fun allocateFrameVariable(): CFGNode.LValue {
        TODO("Not yet implemented")
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
                // For whoever finds this place in the future because of compilation error after
                // adding a new subtype of AuxVariable:
                //   If generateVariableAccess should support the new type, implement new logic here.
                //   If not, uncomment this else statement and add an appropriate unit test.
                // else -> throw GenerateVariableAccessException(
                //   "Cannot generate access to variables other than static links and source variables.")
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
                    Addition(
                        generateAccessToFramePointer(definedInDeclaration),
                        Constant(variableAllocation.offset),
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

    private val prologueEpilogueHandler =
        PrologueEpilogueHandler(
            this,
            ::defaultCallConvention,
            PRESERVED_REGISTERS,
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
