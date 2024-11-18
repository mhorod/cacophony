package cacophony.controlflow.generation

import cacophony.controlflow.CFGNode
import cacophony.controlflow.FunctionHandler
import cacophony.controlflow.Register
import cacophony.controlflow.Variable
import cacophony.controlflow.VariableAllocation
import cacophony.semantic.syntaxtree.Definition

class TestFunctionHandler : FunctionHandler {
    val varRegisters = mutableMapOf<Definition.VariableDeclaration, Register>()

    override fun getFunctionDeclaration(): Definition.FunctionDeclaration {
        TODO()
    }

    override fun generateCallFrom(
        callerFunction: FunctionHandler,
        arguments: List<CFGNode>,
        result: Register?,
        respectStackAlignment: Boolean,
    ): List<CFGNode> {
        TODO()
    }

    override fun generateVariableAccess(variable: Variable): CFGNode.LValue =
        CFGNode.RegisterUse(
            when (variable) {
                is Variable.AuxVariable -> Register.VirtualRegister()
                is Variable.SourceVariable -> varRegisters[variable.definition]!!
            },
        )

    override fun getVariableAllocation(variable: Variable): VariableAllocation {
        TODO()
    }

    override fun registerVariableAllocation(
        variable: Variable,
        allocation: VariableAllocation,
    ) {
        TODO()
    }

    override fun getStaticLink(): Variable.AuxVariable.StaticLinkVariable {
        TODO()
    }

    override fun getVariableFromDefinition(varDef: Definition): Variable {
        TODO()
    }

    override fun generateAccessToFramePointer(other: Definition.FunctionDeclaration): CFGNode {
        TODO()
    }
}
