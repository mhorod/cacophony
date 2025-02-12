package cacophony.semantic.rtti

import cacophony.controlflow.functions.CallableHandler
import cacophony.semantic.syntaxtree.LambdaExpression

fun generateStackFrameOutlines(functionHandlers: Collection<CallableHandler>): List<String> =
    functionHandlers.map { generateStackFrameOutline(it) }

fun generateStackFrameOutline(handler: CallableHandler): String {
    val frameSize =
        handler.getStackSpace().value.let {
            require(it % 8 == 0)
            it / 8
        }
    val label = getStackFrameLocation(handler.getBodyReference())

    return Array(frameSize) { false }.let { stackOutline ->
        handler.getReferenceAccesses().forEach {
            require(it >= 0)
            require(it % 8 == 0)
            stackOutline[it / 8] = true
        }
        toAsm(label, stackOutline.toList())
    }
}

fun getStackFrameLocation(fn: LambdaExpression) = "frame_${fn.getLabel()}"
