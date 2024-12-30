package cacophony.controlflow.generation

import cacophony.controlflow.Register
import cacophony.controlflow.registerUse
import cacophony.semantic.syntaxtree.BaseType
import cacophony.utils.Location
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class LayoutTest {
    private fun simpleLayout(): SimpleLayout = SimpleLayout(registerUse(Register.VirtualRegister()))

    private fun anyLocation(): Pair<Location, Location> {
        val mock = mockk<Pair<Location, Location>>()
        every { mock.toString() } returns "(any)"
        every { mock == any() } returns true
        every { mock.first } returns Location(-1)
        every { mock.second } returns Location(-1)
        return mock
    }

    @Test
    fun `basic layout matches Int`() {
        assert(simpleLayout().matchesType(BaseType.Basic(anyLocation(), "Int")))
    }

    @Test
    fun `struct layout matches simple struct`() {
        val layout =
            StructLayout(
                mapOf(
                    "field0" to simpleLayout(),
                    "field1" to simpleLayout(),
                ),
            )

        val type =
            BaseType.Structural(
                anyLocation(),
                mapOf(
                    "field0" to BaseType.Basic(anyLocation(), "Int"),
                    "field1" to BaseType.Basic(anyLocation(), "Int"),
                ),
            )
        assert(layout.matchesType(type))
    }

    @Test
    fun `struct layout does not match struct with different fields`() {
        val layout =
            StructLayout(
                mapOf(
                    "field0" to simpleLayout(),
                ),
            )

        val type =
            BaseType.Structural(
                anyLocation(),
                mapOf(
                    "field1" to BaseType.Basic(anyLocation(), "Int"),
                ),
            )
        assert(!layout.matchesType(type))
    }

    @Test
    fun `struct layout matches nested struct`() {
        val layout =
            StructLayout(
                mapOf(
                    "field0" to simpleLayout(),
                    "field1" to
                        StructLayout(
                            mapOf(
                                "field0" to simpleLayout(),
                            ),
                        ),
                ),
            )

        val type =
            BaseType.Structural(
                anyLocation(),
                mapOf(
                    "field0" to BaseType.Basic(anyLocation(), "Int"),
                    "field1" to
                        BaseType.Structural(
                            anyLocation(),
                            mapOf(
                                "field0" to BaseType.Basic(anyLocation(), "Int"),
                            ),
                        ),
                ),
            )
        assert(layout.matchesType(type))
    }

    @Test
    fun `struct layout does not match nested struct with different fields`() {
        val layout =
            StructLayout(
                mapOf(
                    "field0" to simpleLayout(),
                    "field1" to
                        StructLayout(
                            mapOf(
                                "field0" to simpleLayout(),
                            ),
                        ),
                ),
            )

        val type =
            BaseType.Structural(
                anyLocation(),
                mapOf(
                    "field0" to BaseType.Basic(anyLocation(), "Int"),
                    "field1" to
                        BaseType.Structural(
                            anyLocation(),
                            mapOf(
                                "field1" to BaseType.Basic(anyLocation(), "Int"),
                            ),
                        ),
                ),
            )
        assert(!layout.matchesType(type))
    }
}
