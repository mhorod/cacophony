package cacophony.regex

import cacophony.token.TokenCategoryGeneral
import cacophony.token.TokenCategorySpecific
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RegexStringsTest {
    @Test
    fun testSpecialCharacterRegex() {
        val expected = """0|1|2|3|4|5|6|7|8|9"""
        assertEquals(expected, RegexStrings.getSpecialCharacterRegex('d'))
    }

    @Test
    fun testGeneralCategoryRegex() {
        val expected = """let|if|then|else|while|do|break|return"""
        assertEquals(expected, RegexStrings.getCategoryRegex(TokenCategoryGeneral.KEYWORD))
    }

    @Test
    fun testSpecificCategoryRegex() {
        val expected = """\("""
        assertEquals(expected, RegexStrings.getCategoryRegex(TokenCategorySpecific.LEFT_PARENTHESIS))
    }
}
