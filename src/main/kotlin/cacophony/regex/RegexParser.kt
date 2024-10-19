package cacophony.regex

import cacophony.utils.AlgebraicRegex

fun parseRegex(str: String): AlgebraicRegex {
    val parser = RegexParser()
    val regex = "($str)"
    var specialCharacter = false
    var lastEscaped = -1
    regex.indices.forEach {
        val c = regex[it]
        if (specialCharacter) {
            val specialRegex =
                SPECIAL_CHARACTER_MAP[c]
                    ?: throw RegexSyntaxErrorException("Invalid escaped character '$c' at position ${it - 1}")
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
    val resultStack = ArrayDeque<RegexType>()
    val operatorStack = ArrayDeque<Operator>()

    fun pushOperation(operator: Operator) {
        try {
            operator.addToStack(this)
        } catch (e: NoSuchElementException) {
            throw RegexSyntaxErrorException("Mismatched operators or parenthesis")
        }
    }

    fun pushSymbol(c: Char) {
        resultStack.add(Atom(c))
    }

    fun pushRegex(c: RegexType) {
        resultStack.add(c)
    }

    fun finalize(): RegexType {
        if (resultStack.size != 1 || operatorStack.isNotEmpty()) throw RegexSyntaxErrorException("Mismatched operators or parenthesis")
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
            throw RegexSyntaxErrorException("Mismatched operators or parenthesis")
        }
    }
}

private sealed class Operator(val priority: Int) {
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
    override fun applyToResult(parser: RegexParser) {
        throw IllegalStateException("Internal RegexParser invariant violated")
    }
}

private data object StarOperator : Operator(3) {
    override fun applyToResult(parser: RegexParser) {
        parser.resultStack.add(Star(parser.resultStack.removeLast()))
    }
}

private sealed class InfixOperator(priority: Int) : Operator(priority) {
    override fun applyToResult(parser: RegexParser) {
        val stack = parser.resultStack
        val x = stack.removeLast()
        val y = stack.removeLast()
        stack.add(merge(y, x))
    }

    abstract fun merge(
        x: RegexType,
        y: RegexType,
    ): RegexType
}

private data object UnionOperator : InfixOperator(1) {
    override fun merge(
        x: RegexType,
        y: RegexType,
    ) = if (x is Union) {
        x.summands.add(y)
        x
    } else {
        Union(arrayListOf(x, y))
    }
}

private data object ConcatOperator : InfixOperator(2) {
    override fun merge(
        x: RegexType,
        y: RegexType,
    ) = if (x is Concat) {
        x.factors.add(y)
        x
    } else {
        Concat(arrayListOf(x, y))
    }
}
