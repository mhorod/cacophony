package cacophony.util

interface AlgebraicRegex

class AtomicRegex(val symbol: Char) : AlgebraicRegex

class UnionRegex(vararg val internalRegexes: Regex) : AlgebraicRegex

class ConcatenationRegex(vararg val internalRegexes: Regex) : AlgebraicRegex

class StarRegex(val internalRegex: Regex) : AlgebraicRegex
