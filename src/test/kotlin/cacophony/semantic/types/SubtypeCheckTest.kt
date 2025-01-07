package cacophony.semantic.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SubtypeCheckTest {
    @Test
    fun `basic type is subtype of itself`() {
        val intType1 = BuiltinType.IntegerType
        val intType2 = BuiltinType.IntegerType
        assertThat(isSubtype(intType1, intType2)).isTrue
    }

    @Test
    fun `different basic types are not subtypes`() {
        val intType = BuiltinType.IntegerType
        val boolType = BuiltinType.BooleanType
        assertThat(isSubtype(intType, boolType)).isFalse
        assertThat(isSubtype(boolType, intType)).isFalse
    }

    @Test
    fun `void is subtype of everything`() {
        val intType = BuiltinType.IntegerType
        val voidType = TypeExpr.VoidType
        assertThat(isSubtype(voidType, intType)).isTrue
    }

    @Test
    fun `nothing is subtype of void`() {
        val intType = BuiltinType.IntegerType
        val voidType = TypeExpr.VoidType
        assertThat(isSubtype(intType, voidType)).isFalse
    }

    @Test
    fun `functions with the same signature are subtypes`() {
        val fType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.IntegerType,
            )
        val gType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.IntegerType,
            )
        assertThat(isSubtype(fType, gType)).isTrue
    }

    @Test
    fun `functions with different arity are not subtypes`() {
        val fType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.IntegerType,
            )
        val gType =
            FunctionType(
                listOf(
                    BuiltinType.IntegerType,
                    BuiltinType.IntegerType,
                ),
                BuiltinType.IntegerType,
            )
        assertThat(isSubtype(fType, gType)).isFalse
        assertThat(isSubtype(gType, fType)).isFalse
    }

    @Test
    fun `functions with different argument types are not subtypes`() {
        val fType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.IntegerType,
            )
        val gType =
            FunctionType(
                listOf(BuiltinType.BooleanType),
                BuiltinType.IntegerType,
            )
        assertThat(isSubtype(fType, gType)).isFalse
        assertThat(isSubtype(gType, fType)).isFalse
    }

    @Test
    fun `functions with different return types are not subtypes`() {
        val fType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.IntegerType,
            )
        val gType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.BooleanType,
            )
        assertThat(isSubtype(fType, gType)).isFalse
        assertThat(isSubtype(gType, fType)).isFalse
    }

    @Test
    fun `covariance on function return types`() {
        val fType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.IntegerType,
            )
        val gType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                TypeExpr.VoidType,
            )
        assertThat(isSubtype(fType, gType)).isFalse
        assertThat(isSubtype(gType, fType)).isTrue
    }

    @Test
    fun `contravariance on function argument types`() {
        val fType =
            FunctionType(
                listOf(BuiltinType.IntegerType),
                BuiltinType.IntegerType,
            )
        val gType =
            FunctionType(
                listOf(TypeExpr.VoidType),
                BuiltinType.IntegerType,
            )
        assertThat(isSubtype(fType, gType)).isTrue
        assertThat(isSubtype(gType, fType)).isFalse
    }

    @Test
    fun `structures with identical fields are subtypes`() {
        val structType1 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                ),
            )
        val structType2 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                ),
            )
        assertThat(isSubtype(structType1, structType2)).isTrue
    }

    @Test
    fun `structures with different field types are not subtypes`() {
        val structType1 =
            StructType(
                mapOf(
                    "x" to BuiltinType.BooleanType,
                ),
            )
        val structType2 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                ),
            )
        assertThat(isSubtype(structType1, structType2)).isFalse
    }

    @Test
    fun `structures with different field names are not subtypes`() {
        val structType1 =
            StructType(
                mapOf(
                    "y" to BuiltinType.IntegerType,
                ),
            )
        val structType2 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                ),
            )
        assertThat(isSubtype(structType1, structType2)).isFalse
    }

    @Test
    fun `structure with subset of fields is subtype`() {
        val structType1 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                ),
            )
        val structType2 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                    "y" to BuiltinType.IntegerType,
                ),
            )
        assertThat(isSubtype(structType1, structType2)).isFalse
        assertThat(isSubtype(structType2, structType1)).isTrue
    }

    @Test
    fun `structure with fields being subtypes is subtype`() {
        val structType1 =
            StructType(
                mapOf(
                    "x" to BuiltinType.IntegerType,
                ),
            )
        val structType2 =
            StructType(
                mapOf(
                    "x" to TypeExpr.VoidType,
                ),
            )
        assertThat(isSubtype(structType1, structType2)).isFalse
        assertThat(isSubtype(structType2, structType1)).isTrue
    }

    @Test
    fun `complex structure`() {
        val structType1 =
            StructType(
                mapOf(
                    "x" to
                        StructType(
                            mapOf("a" to BuiltinType.IntegerType),
                        ),
                    "y" to BuiltinType.IntegerType,
                    "z" to BuiltinType.BooleanType,
                ),
            )
        val structType2 =
            StructType(
                mapOf(
                    "x" to
                        StructType(
                            mapOf("a" to BuiltinType.IntegerType, "b" to BuiltinType.IntegerType),
                        ),
                    "y" to BuiltinType.IntegerType,
                    "z" to TypeExpr.VoidType,
                    "a" to BuiltinType.UnitType,
                ),
            )
        assertThat(isSubtype(structType1, structType2)).isFalse
        assertThat(isSubtype(structType2, structType1)).isTrue
    }

    @Test
    fun `subtyping for references is not supported`() {
        val referenceType1 =
            ReferentialType(
                StructType(
                    mapOf(
                        "x" to BuiltinType.IntegerType,
                    ),
                ),
            )
        val referenceType2 =
            ReferentialType(
                StructType(
                    mapOf(
                        "x" to BuiltinType.IntegerType,
                        "y" to BuiltinType.IntegerType,
                    ),
                ),
            )
        assertThat(isSubtype(referenceType1, referenceType2)).isFalse
        assertThat(isSubtype(referenceType2, referenceType1)).isFalse
    }
}
