package cacophony.controlflow.functions

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Variable
import cacophony.controlflow.VariableAllocation
import cacophony.controlflow.generation.Layout
import cacophony.semantic.syntaxtree.Definition

interface FunctionHandler {
    fun getFunctionDeclaration(): Definition.FunctionDefinition

    fun generateVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue

    fun getVariableAllocation(variable: Variable.PrimitiveVariable): VariableAllocation

    fun registerVariableAllocation(variable: Variable.PrimitiveVariable, allocation: VariableAllocation)

    // Returns static link to parent
    fun getStaticLink(): Variable.PrimitiveVariable

    fun getStackSpace(): CFGNode.ConstantLazy

    fun getVariableFromDefinition(varDef: Definition): Variable

    fun generateAccessToFramePointer(other: Definition.FunctionDefinition): CFGNode

    fun generateStaticLinkVariable(callerFunction: FunctionHandler): CFGNode

    fun allocateFrameVariable(variable: Variable.PrimitiveVariable): CFGNode.LValue

    fun generatePrologue(): List<CFGNode>

    fun generateEpilogue(): List<CFGNode>

    fun getResultLayout(): Layout

    // Returns offsets from RBP to all references on stack
    fun getReferenceAccesses(): List<Int>
}
