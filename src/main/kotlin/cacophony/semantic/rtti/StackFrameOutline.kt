package cacophony.semantic.rtti

import cacophony.controlflow.functions.StaticFunctionHandler
import cacophony.semantic.syntaxtree.LambdaExpression

typealias StackFrameOutlineLocation = Map<LambdaExpression, String>

fun generateStackFrameOutlines(staticFunctionHandlers: Collection<StaticFunctionHandler>): List<String> =
    staticFunctionHandlers.map { generateStackFrameOutline(it) }

fun generateStackFrameOutline(handler: StaticFunctionHandler): String {
    val frameSize =
        handler.getStackSpace().value.let {
            require(it % 8 == 0)
            it / 8
        }
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

fun getStackFrameLocation(fn: LambdaExpression) = "frame_${fn.getLabel()}"
