package cacophony.regex

import cacophony.utils.AlgebraicRegex
import java.util.EmptyStackException
import java.util.Stack

fun parseRegex(str: String): AlgebraicRegex {
    val parser = RegexParser()
    val regex = "($str)"
    var specialCharacter = false
    var lastEscaped = -1
    regex.indices.forEach {
        val c = regex[it]
        if (specialCharacter) {
            if (c !in """()|\*ntr""") throw RegexSyntaxErrorException()
            if (it > 1 && (regex[it - 2] !in "(|" || lastEscaped == it - 2)) parser.pushOperation(ConcatOperator)
            when (c) {
                in """"()|\*""" -> parser.pushSymbol(c)
                'n' -> parser.pushSymbol('\n')
                't' -> parser.pushSymbol('\t')
                'r' -> parser.pushSymbol('\r')
            }
            specialCharacter = false
            lastEscaped = it
        } else {
            when (c) {
                '(' -> {
                    if (it != 0 && (regex[it - 1] !in "(|" || lastEscaped == it - 1)) parser.pushOperation(ConcatOperator)
                    parser.pushOperation(LeftParenthesis)
                }
                ')' -> parser.pushOperation(RightParenthesis)
                '*' -> parser.pushOperation(Star)
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
    val result = Stack<RegexType>()
    val stack = Stack<StackOperator>()

    fun pushOperation(operator: Operator) {
        try {
            operator.addToStack(this)
        } catch (e: EmptyStackException) {
            throw RegexSyntaxErrorException()
        }
    }

    fun pushSymbol(c: Char) {
        result.push(RegexType.Atom(c))
    }

    fun finalize(): RegexType {
        if (result.size != 1 || stack.isNotEmpty()) throw RegexSyntaxErrorException()
        return result.pop()
    }
}

private sealed class Operator {
    abstract fun addToStack(parser: RegexParser)
}

private data object RightParenthesis : Operator() {
    override fun addToStack(parser: RegexParser) {
        while (true) {
            when (val top = parser.stack.pop()) {
                is InfixOperator -> top.applyToResult(parser)
                is LeftParenthesis -> break
            }
        }
    }
}

private data object Star : Operator() {
    override fun addToStack(parser: RegexParser) {
        parser.result.push(RegexType.Star(parser.result.pop()))
    }
}

private sealed class StackOperator(val priority: Int) : Operator()

private data object LeftParenthesis : StackOperator(0) {
    override fun addToStack(parser: RegexParser) {
        parser.stack.push(LeftParenthesis)
    }
}

private sealed class InfixOperator(priority: Int) : StackOperator(priority) {
    fun applyToResult(parser: RegexParser) {
        val stack = parser.result
        val x = stack.pop()
        val y = stack.pop()
        stack.push(merge(y, x))
    }

    abstract fun merge(
        x: RegexType,
        y: RegexType,
    ): RegexType

    override fun addToStack(parser: RegexParser) {
        val stack = parser.stack
        while (true) {
            val topOperator = stack.peek()
            if (priority > topOperator.priority) {
                stack.push(this)
            } else {
                stack.pop()
                require(topOperator is InfixOperator) // invariant: infix operators have larger priority
                topOperator.applyToResult(parser)
                if (priority < topOperator.priority) continue
                stack.push(this)
            }
            return
        }
    }
}

private data object UnionOperator : InfixOperator(1) {
    override fun merge(
        x: RegexType,
        y: RegexType,
    ) = if (x is RegexType.Union) {
        x.summands.add(y)
        x
    } else {
        RegexType.Union(arrayListOf(x, y))
    }
}

private data object ConcatOperator : InfixOperator(2) {
    override fun merge(
        x: RegexType,
        y: RegexType,
    ) = if (x is RegexType.Concat) {
        x.factors.add(y)
        x
    } else {
        RegexType.Concat(arrayListOf(x, y))
    }
}
