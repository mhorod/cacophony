package cacophony.semantic.rtti

import cacophony.semantic.types.BuiltinType
import cacophony.semantic.types.ReferentialType
import cacophony.semantic.types.StructType
import cacophony.semantic.types.TypeExpr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Map.entry
import kotlin.math.absoluteValue

class ObjectOutlinesCreatorTest {
    @Test
    fun `builtin types`() {
        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(listOf(BuiltinType.IntegerType, BuiltinType.BooleanType, BuiltinType.UnitType))

        val labelInt = "outline_Int_${BuiltinType.IntegerType.hashCode().absoluteValue}"
        val labelBoolean = "outline_Boolean_${BuiltinType.BooleanType.hashCode().absoluteValue}"
        val labelUnit = "outline_Unit_${BuiltinType.UnitType.hashCode().absoluteValue}"

        assertThat(ooCreator.getLocations()).containsExactlyInAnyOrderEntriesOf(
            mapOf(BuiltinType.IntegerType to labelInt, BuiltinType.BooleanType to labelBoolean, BuiltinType.UnitType to labelUnit),
        )
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder(
            "$labelInt: dq 1, 0",
            "$labelBoolean: dq 1, 0",
            "$labelUnit: dq 1, 0",
        )
    }

    @Test
    fun references() {
        val refInt = ReferentialType(BuiltinType.IntegerType)
        val labelRefInt = "outline_Ref_Int_${BuiltinType.IntegerType.hashCode().absoluteValue}"
        val struct =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                    "z" to ReferentialType(BuiltinType.IntegerType),
                    "y" to BuiltinType.BooleanType,
                ),
            )
        val refStruct = ReferentialType(struct)
        val labelRefStruct = "outline_Ref_Struct_x_y_z_${struct.hashCode().absoluteValue}"

        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(listOf(refInt, refStruct))

        assertThat(ooCreator.getLocations()).containsExactlyInAnyOrderEntriesOf(
            mapOf(refInt to labelRefInt, refStruct to labelRefStruct),
        )
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder("$labelRefInt: dq 1, 1", "$labelRefStruct: dq 1, 1")
    }

    @Test
    fun `double reference`() {
        val refInt = ReferentialType(BuiltinType.IntegerType)
        val refRefInt = ReferentialType(refInt)
        val labelRefRefInt = "outline_Ref_Ref_Int_${BuiltinType.IntegerType.hashCode().absoluteValue}"
        val struct =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                    "z" to ReferentialType(BuiltinType.IntegerType),
                    "y" to BuiltinType.BooleanType,
                ),
            )
        val refStruct = ReferentialType(struct)
        val refRefStruct = ReferentialType(refStruct)
        val labelRefRefStruct = "outline_Ref_Ref_Struct_x_y_z_${struct.hashCode().absoluteValue}"

        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(listOf(refRefInt, refRefStruct))

        assertThat(ooCreator.getLocations()).containsExactlyInAnyOrderEntriesOf(
            mapOf(refRefInt to labelRefRefInt, refRefStruct to labelRefRefStruct),
        )
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder("$labelRefRefInt: dq 1, 1", "$labelRefRefStruct: dq 1, 1")
    }

    @Test
    fun `simple struct`() {
        val struct =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                    "z" to ReferentialType(BuiltinType.IntegerType),
                    "y" to BuiltinType.BooleanType,
                ),
            )
        val label = "outline_Struct_x_y_z_${struct.hashCode().absoluteValue}"

        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(struct)

        assertThat(ooCreator.getLocations()).containsExactly(entry(struct, label))
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder("$label: dq 3, 4")
    }

    @Test
    fun `nested struct`() {
        val refIntT = ReferentialType(BuiltinType.IntegerType)
        val refStructT = ReferentialType(StructType(mapOf("t" to BuiltinType.IntegerType)))
        val struct =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                    "y" to StructType(mapOf("a" to refIntT, "b" to BuiltinType.BooleanType)),
                    "z" to refStructT,
                ),
            )
        val label = "outline_Struct_x_y_z_${struct.hashCode().absoluteValue}"

        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(struct)

        assertThat(ooCreator.getLocations()).containsExactly(entry(struct, label))
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder("$label: dq 4, 10")
    }

    @Test
    fun `large struct`() {
        val refIntT = ReferentialType(BuiltinType.IntegerType)
        val fieldsMap = mutableMapOf<String, TypeExpr>()
        val fieldNames = mutableListOf<String>()
        for (i in 0..69) {
            fieldsMap["x$i"] = refIntT
            fieldNames.add("x$i")
        }
        val struct = StructType(fieldsMap)
        fieldsMap["y0"] = BuiltinType.IntegerType
        fieldsMap["y1"] = refIntT
        fieldNames.add("y0")
        fieldNames.add("y1")
        val label = "outline_Struct_" + fieldNames.sorted().joinToString("_") + "_${struct.hashCode().absoluteValue}"

        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(struct)

        assertThat(ooCreator.getLocations()).containsExactly(entry(struct, label))
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder("$label: dq 72, ${ULong.MAX_VALUE}, ${255 - 64}")
    }

    @Test
    fun `deep struct`() {
        val refIntT = ReferentialType(BuiltinType.IntegerType)
        var struct =
            StructType(
                mapOf("a" to BuiltinType.IntegerType, "b" to refIntT),
            )
        for (i in 0..20) {
            struct = StructType(mapOf("x" to struct))
        }
        val label = "outline_Struct_x_${struct.hashCode().absoluteValue}"

        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(struct)

        assertThat(ooCreator.getLocations()).containsExactly(entry(struct, label))
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder("$label: dq 2, 2")
    }

    @Test
    fun `many structs with repetitions`() {
        val refStructT = ReferentialType(StructType(mapOf("t" to BuiltinType.IntegerType)))
        val struct1 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                    "z" to ReferentialType(BuiltinType.IntegerType),
                    "y" to BuiltinType.BooleanType,
                ),
            )
        val label1 = "outline_Struct_x_y_z_${struct1.hashCode().absoluteValue}"
        val struct2 = StructType(mapOf("a" to BuiltinType.IntegerType, "b" to BuiltinType.IntegerType))
        val label2 = "outline_Struct_a_b_${struct2.hashCode().absoluteValue}"
        val struct3 = StructType(mapOf("r" to refStructT, "x" to BuiltinType.UnitType))
        val label3 = "outline_Struct_r_x_${struct3.hashCode().absoluteValue}"

        val ooCreator = ObjectOutlinesCreator()
        ooCreator.add(listOf(struct1, struct2, struct3))
        ooCreator.add(struct1)
        ooCreator.add(struct2)
        ooCreator.add(struct1)

        assertThat(ooCreator.getLocations()).containsExactlyInAnyOrderEntriesOf(
            mapOf(struct1 to label1, struct2 to label2, struct3 to label3),
        )
        assertThat(ooCreator.getAsm()).containsExactlyInAnyOrder(
            "$label1: dq 3, 4",
            "$label2: dq 2, 0",
            "$label3: dq 2, 1",
        )
    }
}
