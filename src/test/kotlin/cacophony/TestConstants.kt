package cacophony

import cacophony.automata.SimpleDFA
import cacophony.automata.minimalization.via

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
            1 via 'a' to 2,
            1 via 'b' to 2,
            1 via 'A' to 2,
            1 via 'B' to 2,
            2 via 'a' to 2,
            2 via 'b' to 2,
            2 via 'A' to 2,
            2 via 'B' to 2,
        ),
        mapOf(
            2 to Unit,
        ),
    )

// (A|B)(a|b)*
val DFA_FIRST_UPPER_CASE =
    SimpleDFA(
        1,
        mapOf(
            1 via 'A' to 2,
            1 via 'B' to 2,
            2 via 'a' to 2,
            2 via 'b' to 2,
        ),
        mapOf(
            2 to Unit,
        ),
    )

// (A|B)+
val DFA_UPPER_CASE =
    SimpleDFA(
        1,
        mapOf(
            1 via 'A' to 2,
            1 via 'B' to 2,
            2 via 'A' to 2,
            2 via 'B' to 2,
        ),
        mapOf(
            2 to Unit,
        ),
    )

// [(a|A|b|B)*]
val DFA_SQUARE =
    SimpleDFA(
        1,
        mapOf(
            1 via '[' to 2,
            2 via 'a' to 2,
            2 via 'b' to 2,
            2 via 'A' to 2,
            2 via 'B' to 2,
            2 via ']' to 3,
        ),
        mapOf(
            3 to Unit,
        ),
    )
