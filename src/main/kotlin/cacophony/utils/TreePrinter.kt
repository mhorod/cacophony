package cacophony.utils

interface Tree {
    fun isLeaf(): Boolean

    fun children(): List<Tree>

    fun stringValue(): String
}

class TreePrinter(
    builder: StringBuilder,
) {
    val builder: StringBuilder = builder

    private fun printTreeInternal(
        t: Tree,
        indent: String,
        isLastChild: Boolean,
    ) {
        println("Indent = x${indent}x")
        if (indent.isNotEmpty()) {
            builder.append(indent.dropLast(1))
        }
        if (isLastChild) {
            builder.append('└')
        } else {
            builder.append('├')
        }
        if (t.isLeaf()) {
            builder.append('—')
            builder.append(t.stringValue())
            builder.append('\n')
        } else {
            builder.append('┬')
            builder.append(t.stringValue())
            builder.append('\n')
            var newIndent = "$indent│"
            for (child in t.children().dropLast(1)) {
                printTreeInternal(child, newIndent, false)
            }
            newIndent = "$indent "
            printTreeInternal(t.children().last(), newIndent, true)
        }
    }

    fun printTree(t: Tree): String {
        printTreeInternal(t, " ", true)
        return builder.toString()
    }
}
