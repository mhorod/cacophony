package cacophony.controlflow.functions

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.VariableAllocation
import cacophony.semantic.syntaxtree.Definition

interface FunctionCallHandler {
    fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean = true,
    ): List<CFGNode>
}

interface FunctionHandler : FunctionCallHandler {
    fun getFunctionDeclaration(): Definition.FunctionDefinition

    fun generateVariableAccess(variable: Variable): CFGNode.LValue

    fun getVariableAllocation(variable: Variable): VariableAllocation

    fun registerVariableAllocation(variable: Variable, allocation: VariableAllocation)

    // Returns static link to parent
    fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable

    fun getStackSpace(): CFGNode.ConstantLazy

    fun getVariableFromDefinition(varDef: Definition): Variable

    fun generateAccessToFramePointer(other: Definition.FunctionDefinition): CFGNode

    fun allocateFrameVariable(variable: Variable): CFGNode.LValue

    fun generatePrologue(): List<CFGNode>

    fun generateEpilogue(): List<CFGNode>

    fun getResultRegister(): Register.VirtualRegister
}
