package cacophony.controlflow.functions

import cacophony.controlflow.CFGNode
import cacophony.controlflow.Variable
import cacophony.controlflow.VariableAllocation
import cacophony.controlflow.generation.Layout
import cacophony.semantic.syntaxtree.Definition
import cacophony.semantic.syntaxtree.LambdaExpression

interface CallableHandler {
    fun generateVariableAccess(variable: Variable.PrimitiveVariable): CFGNode.LValue

    fun getVariableAllocation(variable: Variable.PrimitiveVariable): VariableAllocation

    fun registerVariableAllocation(variable: Variable.PrimitiveVariable, allocation: VariableAllocation)

    fun getVariableFromDefinition(varDef: Definition): Variable

    fun generatePrologue(): List<CFGNode>

    fun generateEpilogue(): List<CFGNode>

    fun getResultLayout(): Layout

    // Returns offsets from RBP to all references on stack
    fun getReferenceAccesses(): List<Int>

    fun getStackSpace(): CFGNode.ConstantLazy

    fun allocateFrameVariable(variable: Variable.PrimitiveVariable): CFGNode.LValue

    fun generateStaticLinkVariable(callerFunction: FunctionHandler): CFGNode
}

interface LambdaHandler : CallableHandler {
    fun getBodyReference(): LambdaExpression

    // TODO: maybe add getClosureLink?
}

interface FunctionHandler : CallableHandler {
    fun getFunctionDeclaration(): Definition.FunctionDefinition

    // Returns static link to parent
    fun getStaticLink(): Variable.PrimitiveVariable

    fun generateAccessToFramePointer(other: Definition.FunctionDefinition): CFGNode
}
