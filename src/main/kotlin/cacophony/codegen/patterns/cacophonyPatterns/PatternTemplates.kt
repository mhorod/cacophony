package cacophony.codegen.patterns.cacophonyPatterns

import cacophony.controlflow.CFGNode
import cacophony.controlflow.RegisterLabel
import cacophony.controlflow.ValueLabel

abstract class RegisterAssignmentTemplate {
    val lhsRegisterLabel = RegisterLabel()
    val rhsLabel = ValueLabel()
    val lhsSlot = CFGNode.RegisterSlot(lhsRegisterLabel)
    val rhsSlot = CFGNode.ValueSlot(rhsLabel)
}

abstract class MemoryAssignmentTemplate {
    val lhsLabel = ValueLabel()
    val rhsLabel = ValueLabel()
    val lhsSlot = CFGNode.MemoryAccess(CFGNode.ValueSlot(lhsLabel))
    val rhsSlot = CFGNode.ValueSlot(rhsLabel)
}

abstract class BinaryOpPattern {
    val lhsLabel = ValueLabel()
    val rhsLabel = ValueLabel()
    val lhsSlot = CFGNode.ValueSlot(lhsLabel)
    val rhsSlot = CFGNode.ValueSlot(rhsLabel)
}

abstract class UnaryOpPattern {
    val childLabel = ValueLabel()
    val childSlot = CFGNode.ValueSlot(childLabel)
}
