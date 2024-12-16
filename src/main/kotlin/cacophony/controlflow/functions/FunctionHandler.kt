package cacophony.controlflow.functions

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.VariableAllocation
import cacophony.semantic.syntaxtree.Definition

interface FunctionHandler {
    fun getFunctionDeclaration(): Definition.FunctionDefinition

    fun generateVariableAccess(variable: Variable): CFGNode.LValue

    fun getVariableAllocation(variable: Variable): VariableAllocation

    fun registerVariableAllocation(variable: Variable, allocation: VariableAllocation)

    // Returns static link to parent
    fun getStaticLink(): Variable.PrimitiveVariable

    fun getStackSpace(): CFGNode.ConstantLazy

    fun getVariableFromDefinition(varDef: Definition): Variable

    fun generateAccessToFramePointer(other: Definition.FunctionDefinition): CFGNode

    fun generateStaticLinkVariable(callerFunction: FunctionHandler): CFGNode

    fun allocateFrameVariable(variable: Variable): CFGNode.LValue

    fun generatePrologue(): List<CFGNode>

    fun generateEpilogue(): List<CFGNode>

    fun getResultRegister(): Register.VirtualRegister
}
