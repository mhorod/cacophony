package cacophony

import cacophony.automata.SimpleDFA

enum class MockCategory {
    NON_EMPTY,
    SQUARE,
    FIRST_UPPER_CASE,
    UPPER_CASE,
}

// (a|A|b|B)+
val DFA_NON_EMPTY =
    SimpleDFA(
        1,
        mapOf(
            Pair(1, 'a') to 2,
            Pair(1, 'b') to 2,
            Pair(1, 'A') to 2,
            Pair(1, 'B') to 2,
            Pair(2, 'a') to 2,
            Pair(2, 'b') to 2,
            Pair(2, 'A') to 2,
            Pair(2, 'B') to 2,
        ),
        setOf(2),
    )

// (A|B)(a|b)*
val DFA_FIRST_UPPER_CASE =
    SimpleDFA(
        1,
        mapOf(
            Pair(1, 'A') to 2,
            Pair(1, 'B') to 2,
            Pair(2, 'a') to 2,
            Pair(2, 'b') to 2,
        ),
        setOf(2),
    )

// (A|B)+
val DFA_UPPER_CASE =
    SimpleDFA(
        1,
        mapOf(
            Pair(1, 'A') to 2,
            Pair(1, 'B') to 2,
            Pair(2, 'A') to 2,
            Pair(2, 'B') to 2,
        ),
        setOf(2),
    )

// [(a|A|b|B)*]
val DFA_SQUARE =
    SimpleDFA(
        1,
        mapOf(
            Pair(1, '[') to 2,
            Pair(2, 'a') to 2,
            Pair(2, 'b') to 2,
            Pair(2, 'A') to 2,
            Pair(2, 'B') to 2,
            Pair(2, ']') to 3,
        ),
        setOf(3),
    )
