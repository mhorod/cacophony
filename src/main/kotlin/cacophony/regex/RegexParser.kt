package cacophony.regex

import cacophony.utils.AlgebraicRegex

fun parseRegex(str: String): AlgebraicRegex<Char> {
    val parser = RegexParser()
    val regex = "($str)"
    var specialCharacter = false
    var lastEscaped = -1
    regex.indices.forEach {
        val c = regex[it]
        if (specialCharacter) {
            val specialRegex =
                getSpecialCharacterRegex(c)
                    ?: throw RegexSyntaxError("Invalid escaped character '$c' at position ${it - 1}")
            if (it > 1 && (regex[it - 2] !in "(|" || lastEscaped == it - 2)) parser.pushOperation(ConcatOperator)
            parser.pushRegex(specialRegex)
            specialCharacter = false
            lastEscaped = it
        } else {
            when (c) {
                '(' -> {
                    if (it != 0 && (regex[it - 1] !in "(|" || lastEscaped == it - 1)) parser.pushOperation(ConcatOperator)
                    parser.pushOperation(LeftParenthesis)
                }

                ')' -> parser.closeParenthesis()
                '*' -> parser.pushOperation(StarOperator)
                '|' -> parser.pushOperation(UnionOperator)
                '\\' -> specialCharacter = true
                else -> {
                    if (regex[it - 1] !in "(|") parser.pushOperation(ConcatOperator)
                    parser.pushSymbol(c)
                }
            }
        }
    }
    return parser.finalize().toAlgebraicRegex()
}

private class RegexParser {
    val resultStack = ArrayDeque<RegexT>()
    val operatorStack = ArrayDeque<Operator>()

    fun pushOperation(operator: Operator) {
        try {
            operator.addToStack(this)
        } catch (e: NoSuchElementException) {
            throw RegexSyntaxError("Mismatched operators or parenthesis")
        }
    }

    fun pushSymbol(c: Char) {
        resultStack.add(Atom(c))
    }

    fun pushRegex(c: RegexT) {
        resultStack.add(c)
    }

    fun finalize(): RegexT {
        if (resultStack.size != 1 || operatorStack.isNotEmpty()) throw RegexSyntaxError("Mismatched operators or parenthesis")
        return resultStack.removeLast()
    }

    fun closeParenthesis() {
        try {
            while (true) {
                when (val top = operatorStack.removeLast()) {
                    is LeftParenthesis -> break
                    else -> top.applyToResult(this)
                }
            }
        } catch (e: NoSuchElementException) {
            throw RegexSyntaxError("Mismatched operators or parenthesis")
        }
    }
}

private sealed class Operator(
    val priority: Int,
) {
    open fun addToStack(parser: RegexParser) {
        val stack = parser.operatorStack
        while (true) {
            val topOperator = stack.last()
            if (priority <= topOperator.priority) {
                stack.removeLast()
                topOperator.applyToResult(parser)
                if (priority < topOperator.priority) continue
            }
            stack.add(this)
            return
        }
    }

    abstract fun applyToResult(parser: RegexParser)
}

private data object LeftParenthesis : Operator(0) {
    override fun addToStack(parser: RegexParser) {
        parser.operatorStack.add(this)
    }

    // Should never be reached, as LeftParenthesis has the lowest priority
    override fun applyToResult(parser: RegexParser): Unit = throw IllegalStateException("Internal RegexParser invariant violated")
}

private data object StarOperator : Operator(3) {
    override fun applyToResult(parser: RegexParser) {
        parser.resultStack.add(Star(parser.resultStack.removeLast()))
    }
}

private sealed class InfixOperator(
    priority: Int,
) : Operator(priority) {
    override fun applyToResult(parser: RegexParser) {
        val stack = parser.resultStack
        val x = stack.removeLast()
        val y = stack.removeLast()
        stack.add(merge(y, x))
    }

    abstract fun merge(x: RegexT, y: RegexT): RegexT
}

private data object UnionOperator : InfixOperator(1) {
    override fun merge(x: RegexT, y: RegexT) =
        if (x is Union) {
            x.summands.add(y)
            x
        } else if (y is Union) {
            // Does not need to be at 0, but this is more consistent with string representation.
            y.summands.add(0, x)
            y
        } else {
            Union(mutableListOf(x, y))
        }
}

private data object ConcatOperator : InfixOperator(2) {
    override fun merge(x: RegexT, y: RegexT) =
        if (x is Concat) {
            x.factors.add(y)
            x
        } else if (y is Concat) {
            y.factors.add(0, x)
            y
        } else {
            Concat(mutableListOf(x, y))
        }
}
