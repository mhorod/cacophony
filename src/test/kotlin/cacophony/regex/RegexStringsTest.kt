package cacophony.regex

import cacophony.token.TokenCategoryGeneral
import cacophony.token.TokenCategorySpecific
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RegexStringsTest {
    @Test
    fun `correct regex for general category`() {
        val expected = """let|if|then|else|while|do|break|return"""
        assertEquals(expected, RegexStrings.getCategoryRegex(TokenCategoryGeneral.KEYWORD))
    }

    @Test
    fun `correct regex for specific category`() {
        val expected = """\("""
        assertEquals(expected, RegexStrings.getCategoryRegex(TokenCategorySpecific.LEFT_PARENTHESIS))
    }
}
