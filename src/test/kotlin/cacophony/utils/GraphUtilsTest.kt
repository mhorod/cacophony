package cacophony.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GraphUtilsTest {
    @Test
    fun transitiveClosureTest() {
        val actual =
            getTransitiveClosure(
                mapOf(
                    1 to setOf(2, 3),
                    2 to setOf(3, 4),
                    3 to setOf(),
                    4 to setOf(5),
                    5 to setOf(4, 6),
                    50 to setOf(60),
                    60 to setOf(50),
                    100 to setOf(100),
                    200 to setOf(),
                ),
            )
        val expected =
            mapOf(
                1 to setOf(1, 2, 3, 4, 5, 6),
                2 to setOf(2, 3, 4, 5, 6),
                3 to setOf(3),
                4 to setOf(4, 5, 6),
                5 to setOf(4, 5, 6),
                6 to setOf(6),
                50 to setOf(50, 60),
                60 to setOf(50, 60),
                100 to setOf(100),
                200 to setOf(200),
            )
        assertEquals(expected, actual)
    }

    @Test
    fun properTransitiveClosureTest() {
        val actual =
            getProperTransitiveClosure(
                mapOf(
                    1 to setOf(2, 3),
                    2 to setOf(3, 4),
                    3 to setOf(),
                    4 to setOf(5),
                    5 to setOf(4, 6),
                    50 to setOf(60),
                    60 to setOf(50),
                    100 to setOf(100),
                    200 to setOf(),
                ),
            )
        val expected =
            mapOf(
                1 to setOf(2, 3, 4, 5, 6),
                2 to setOf(3, 4, 5, 6),
                3 to setOf(),
                4 to setOf(4, 5, 6),
                5 to setOf(4, 5, 6),
                6 to setOf(),
                50 to setOf(50, 60),
                60 to setOf(50, 60),
                100 to setOf(100),
                200 to setOf(),
            )
        assertEquals(expected, actual)
    }
}
