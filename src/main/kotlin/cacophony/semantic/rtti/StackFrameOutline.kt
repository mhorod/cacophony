package cacophony.semantic.rtti

import cacophony.controlflow.functions.FunctionHandler
import cacophony.semantic.syntaxtree.Definition.FunctionDefinition

typealias StackFrameOutlineLocation = Map<FunctionDefinition, String>

fun generateStackFrameOutline(handler: FunctionHandler): String {
    val frameSize = handler.getStackSpace().value
    val label = getStackFrameLocation(handler.getFunctionDeclaration())

    return Array(frameSize) { false }.let { stackOutline ->
        handler.getReferenceAccesses().forEach {
            require(it >= 0)
            require(it % 8 == 0)
            stackOutline[it / 8] = true
        }
        toAsm(label, stackOutline.toList())
    }
}

fun getStackFrameLocation(fn: FunctionDefinition) = "frame_${fn.getLabel()}"
