package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.controlflow.CFGNode
import cacophony.controlflow.RegisterLabel
import cacophony.controlflow.ValueLabel

abstract class RegisterAssignmentTemplate {
    protected val lhsRegisterLabel = RegisterLabel()
    protected val rhsLabel = ValueLabel()
    protected val lhsSlot = CFGNode.RegisterSlot(lhsRegisterLabel)
    protected val rhsSlot = CFGNode.ValueSlot(rhsLabel)
}

abstract class MemoryAssignmentTemplate {
    protected val lhsLabel = ValueLabel()
    protected val rhsLabel = ValueLabel()
    protected val lhsSlot = CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel))
    protected val rhsSlot = CFGNode.ValueSlot(rhsLabel)
}

abstract class BinaryOpPattern {
    protected val lhsLabel = ValueLabel()
    protected val rhsLabel = ValueLabel()
    protected val lhsSlot = CFGNode.ValueSlot(lhsLabel)
    protected val rhsSlot = CFGNode.ValueSlot(rhsLabel)
}

abstract class UnaryOpPattern {
    protected val childLabel = ValueLabel()
    protected val childSlot = CFGNode.ValueSlot(childLabel)
}
