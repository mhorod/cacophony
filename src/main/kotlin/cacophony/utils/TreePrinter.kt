package cacophony.utils

interface Tree {
    fun isLeaf(): Boolean

    fun children(): List<Tree>
}

interface TreeLeaf : Tree {
    override fun isLeaf() = true

    override fun children() = emptyList<Tree>()
}

class TreePrinter(
    builder: StringBuilder,
) {
    val builder: StringBuilder = builder

    private fun printBlock(
        block: String,
        indent: String,
    ) {
        builder.append(block.lines().first())
        builder.append('\n')
        for (line in block.lines().drop(1)) {
            builder.append(indent + line)
            builder.append('\n')
        }
    }

    private fun printTreeInternal(
        t: Tree,
        indent: String,
        isLastChild: Boolean,
    ) {
        if (indent.isNotEmpty()) {
            builder.append(indent.dropLast(1))
        }
        if (isLastChild) {
            builder.append('└')
        } else {
            builder.append('├')
        }
        if (t.isLeaf()) {
            builder.append('─')
            printBlock(t.toString(), "$indent  ")
        } else {
            builder.append('┬')
            var newIndent = "$indent│"
            printBlock(t.toString(), newIndent)
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
