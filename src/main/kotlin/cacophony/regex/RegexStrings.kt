package cacophony.regex

object RegexStrings {
    const val PUNCTUATION_REGEX = """\(|\)|[|]|->|=>|;|:|,"""
    const val KEYWORD_REGEX = """let|if|then|else|while|do|break|return"""
    const val LITERAL_REGEX ="""-\d(\d)*|\d(\d)*|true|false"""
    const val OPERATOR_REGEX = """==|=|<|>|<=|>=|+=|-=|+|-|\*|/|%|&&|\|\||!"""
    const val TYPE_IDENTIFIER_REGEX = """\u\w*"""
    const val VARIABLE_IDENTIFIER_REGEX = """(\l|_)\w*"""
    const val WHITESPACE_REGEX = """(\n|\r|\t| )*"""

    const val COMMENT_REGEX = """#\N*""" //dont know iw we will implement it

    //some auxiliary regexes:
    const val AUX_WORD_REGEX = """\d|\l|\u|_""" // \w
    const val AUX_DIGIT_REGEX = """0|1|2|3|4|5|6|7|8|9""" // \d
    const val AUX_LOWERCASE_REGEX = """a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z""" // \l
    const val AUX_UPPERCASE_REGEX = """A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z""" // \u
}