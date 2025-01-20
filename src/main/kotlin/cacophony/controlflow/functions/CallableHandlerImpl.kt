package cacophony.controlflow.functions

import cacophony.controlflow.*
import cacophony.controlflow.CFGNode.MemoryAccess
import cacophony.controlflow.CFGNode.RegisterUse
import cacophony.controlflow.generation.Layout
import cacophony.controlflow.generation.generateLayoutOfVirtualRegisters
import cacophony.semantic.analysis.AnalyzedFunction
import cacophony.semantic.analysis.EscapeAnalysisResult
import cacophony.semantic.analysis.VariablesMap
import cacophony.semantic.syntaxtree.FunctionalExpression
import kotlin.math.max

abstract class CallableHandlerImpl(
    private val analyzedFunction: AnalyzedFunction, // TODO: we need to change it so it also covers lambda expressions
    private val function: FunctionalExpression, // TODO: and probably get rid of that, analyzedFunction should have all necessary info
    private val variablesMap: VariablesMap,
    private val escapeAnalysisResult: EscapeAnalysisResult,
) : CallableHandler {
    private var stackSpace = REGISTER_SIZE
    private val variableAllocation: MutableMap<Variable.PrimitiveVariable, VariableAllocation> = mutableMapOf()

    // we want to create pointers for all variables
    // that need to be allocated on heap
    private val heapVariablePointers: Map<Variable.PrimitiveVariable, Variable.PrimitiveVariable> =
        analyzedFunction.variables
            .map {
                it.origin
            }.filter {
                escapeAnalysisResult.contains(it)
            }.filterIsInstance<Variable.PrimitiveVariable>()
            .associateWith { Variable.PrimitiveVariable() }

    // Initially variables may be allocated in virtualRegisters, only after spill handling we know
    // if they're truly on stack or in registers.
    // Every virtualRegister knows if it holds reference, therefore we care only about references on stack here.
    protected abstract val referenceOffsets: MutableList<Int>

    // This does not perform any checks and may override previous allocation.
    // Holes in the stack may be also created if stack variables are directly allocated with this method.
    override fun registerVariableAllocation(variable: Variable.PrimitiveVariable, allocation: VariableAllocation) {
        if (allocation is VariableAllocation.OnStack) {
            if (variable.holdsReference) {
                referenceOffsets.add(allocation.offset)
            }
            stackSpace = max(stackSpace, allocation.offset + REGISTER_SIZE)
            println("stack space increases due to $variable.")
        }
        variableAllocation[variable] = allocation
    }

    override fun allocateFrameVariable(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        val currentStackSpace = stackSpace
        registerVariableAllocation(variable, VariableAllocation.OnStack(stackSpace))
        return MemoryAccess(
            CFGNode.Subtraction(
                RegisterUse(Register.FixedRegister(HardwareRegister.RBP), false),
                CFGNode.ConstantKnown(currentStackSpace),
            ),
            variable.holdsReference,
        )
    }

    override fun getAnalyzedFunction() = analyzedFunction

    protected fun allocateVariables() {
        val usedVars =
            analyzedFunction.variablesUsedInNestedFunctions
                .filterIsInstance<Variable.PrimitiveVariable>()
        val declVars =
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
        val regVar =
            declVars
                .minus(usedVars.toSet())

        regVar
            .filterNot {
                heapVariablePointers.containsKey(it)
            }.forEach {
                registerVariableAllocation(
                    it,
                    VariableAllocation.InRegister(Register.VirtualRegister(it.holdsReference)),
                )
            }

        val stackVars = declVars.intersect(usedVars.toSet())

        stackVars
            .filterNot {
                heapVariablePointers.containsKey(it)
            }.forEach {
                allocateFrameVariable(it)
            }

        heapVariablePointers.forEach {
            allocateFrameVariable(it.value) // allocate pointer on stack
            registerVariableAllocation(
                it.key,
                VariableAllocation.ViaPointer(variableAllocation[it.value]!!, 0),
            )
        }
    }

    override fun generateVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue {
        // if variable is our argument or is defined inside our body, it should be in variableAllocation
        if (variable in variableAllocation) {
            return wrapAllocation(
                getVariableAllocation(variable),
                variable.holdsReference,
            )
        }

        return generateOutsideVariableAccess(variable)
    }

    protected abstract fun generateOutsideVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue

    override fun getVariableAllocation(variable: Variable.PrimitiveVariable): VariableAllocation =
        variableAllocation.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable have not been allocated inside $this FunctionHandler")
        }

    override fun hasVariableAllocation(variable: Variable.PrimitiveVariable): Boolean {
        println((this as FunctionHandler).getFunctionDeclaration())
        println(variableAllocation[variable])
        return variableAllocation.containsKey(variable)
    }

    override fun getStackSpace(): CFGNode.ConstantLazy = CFGNode.ConstantLazy { stackSpace }

    private val resultLayout = generateLayoutOfVirtualRegisters(function.returnType)

    override fun getResultLayout(): Layout = resultLayout

    protected abstract fun getFlattenedArguments(): List<CFGNode>

    protected abstract fun getPrologueEpilogueHandler(): PrologueEpilogueHandler

    override fun generatePrologue(): List<CFGNode> = getPrologueEpilogueHandler().generatePrologue()

    override fun generateEpilogue(): List<CFGNode> = getPrologueEpilogueHandler().generateEpilogue()

    override fun getReferenceAccesses(): List<Int> = referenceOffsets

    protected fun getHeapVariablePointers() = heapVariablePointers

    override fun getPointerToHeapVariable(variable: Variable.PrimitiveVariable) =
        heapVariablePointers.getOrElse(variable) {
            throw IllegalArgumentException("Variable $variable is not allocated on heap inside $this FunctionHandler")
        }
}
