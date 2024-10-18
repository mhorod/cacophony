package cacophony.regex

import cacophony.token.TokenCategoryGeneral
import cacophony.token.TokenCategorySpecific

object RegexStrings {
    private val generalCategoryMap =
        mapOf(
            TokenCategoryGeneral.PUNCTUATION to """\(|\)|[|]|->|=>|;|:|,""",
            TokenCategoryGeneral.KEYWORD to """let|if|then|else|while|do|break|return""",
            TokenCategoryGeneral.LITERAL to """-\d(\d)*|\d(\d)*|true|false""",
            TokenCategoryGeneral.OPERATOR to """==|!=|=|<|>|<=|>=|+=|-=|+|-|\*|\*=|/|/=|%|%=|&&|\|\||!""",
            TokenCategoryGeneral.TYPE_IDENTIFIER to """\u\w*""",
            TokenCategoryGeneral.VARIABLE_IDENTIFIER to """(\l|_)\w*""",
            TokenCategoryGeneral.WHITESPACE to """(\n|\r|\t| )*""",
            TokenCategoryGeneral.COMMENT to """#\N*""", // dont know if it will be implemented
        )

    private val specificCategoryMap =
        mapOf(
            TokenCategorySpecific.LEFT_PARENTHESIS to """\(""",
            TokenCategorySpecific.RIGHT_PARENTHESIS to """\)""",
            TokenCategorySpecific.LEFT_BRACKET to """[""",
            TokenCategorySpecific.RIGHT_BRACKET to """]""",
            TokenCategorySpecific.ARROW to """->""",
            TokenCategorySpecific.DOUBLE_ARROW to """=>""",
            TokenCategorySpecific.COLON to """:""",
            TokenCategorySpecific.SEMICOLON to """;""",
            TokenCategorySpecific.COMMA to """,""",
            // keywords
            TokenCategorySpecific.KEYWORD_LET to """let""",
            TokenCategorySpecific.KEYWORD_IF to """if""",
            TokenCategorySpecific.KEYWORD_THEN to """then""",
            TokenCategorySpecific.KEYWORD_ELSE to """else""",
            TokenCategorySpecific.KEYWORD_WHILE to """while""",
            TokenCategorySpecific.KEYWORD_DO to """do""",
            TokenCategorySpecific.KEYWORD_BREAK to """break""",
            TokenCategorySpecific.KEYWORD_RETURN to """return""",
            // operators
            TokenCategorySpecific.OPERATOR_EQUALS to """==""",
            TokenCategorySpecific.OPERATOR_NOT_EQUALS to """!=""",
            TokenCategorySpecific.OPERATOR_ASSIGNMENT to """=""",
            TokenCategorySpecific.OPERATOR_LESS to """<""",
            TokenCategorySpecific.OPERATOR_GREATER to """>""",
            TokenCategorySpecific.OPERATOR_LESS_EQUAL to """<=""",
            TokenCategorySpecific.OPERATOR_GREATER_EQUAL to """>=""",
            TokenCategorySpecific.OPERATOR_ADDITION to """+""",
            TokenCategorySpecific.OPERATOR_SUBTRACTION to """-""",
            TokenCategorySpecific.OPERATOR_ADDITION_ASSIGNMENT to """+=""",
            TokenCategorySpecific.OPERATOR_SUBTRACTION_ASSIGNMENT to """-=""",
            TokenCategorySpecific.OPERATOR_MULTIPLICATION to """\*""",
            TokenCategorySpecific.OPERATOR_DIVISION to """/""",
            TokenCategorySpecific.OPERATOR_MODULO to """%""",
            TokenCategorySpecific.OPERATOR_MULTIPLICATION_ASSIGNMENT to """\*=""",
            TokenCategorySpecific.OPERATOR_DIVISION_ASSIGNMENT to """/=""",
            TokenCategorySpecific.OPERATOR_MODULO_ASSIGNMENT to """%=""",
            TokenCategorySpecific.OPERATOR_LOGICAL_OR to """\|\|""",
            TokenCategorySpecific.OPERATOR_LOGICAL_AND to """&&""",
            TokenCategorySpecific.OPERATOR_LOGICAL_NOT to """!""",
            // and the others
            TokenCategorySpecific.LITERAL to """-\d(\d)*|\d(\d)*|true|false""",
            TokenCategorySpecific.TYPE_IDENTIFIER to """\u\w*""",
            TokenCategorySpecific.VARIABLE_IDENTIFIER to """(\l|_)\w*""",
            TokenCategorySpecific.WHITESPACE to """(\n|\r|\t| )*""",
            TokenCategorySpecific.COMMENT to """#\N*""",
        )

    private val specialCharacterMap =
        mapOf(
            'w' to """\d|\l|\u|_""",
            'd' to """0|1|2|3|4|5|6|7|8|9""",
            'l' to """a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z""",
            'u' to """A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z""",
        )

    fun getCategoryRegex(category: TokenCategoryGeneral): String? {
        return generalCategoryMap.get(category)
    }

    fun getCategoryRegex(category: TokenCategorySpecific): String? {
        return specificCategoryMap.get(category)
    }

    fun getSpecialCharacterRegex(character: Char): String? {
        return specialCharacterMap.get(character)
    }
}
