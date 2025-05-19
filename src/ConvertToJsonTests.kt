import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ConvertToJsonTests {

    data class Course(
        val name: String,
        val credits: Int,
        val evaluation: List<EvalItem>
    )

    data class EvalItem(
        val name: String,
        val percentage: Double,
        val mandatory: Boolean,
        val type: EvalType?
    )

    enum class EvalType {
        TEST, PROJECT, EXAM
    }

    @Test
    fun testNumberConvert() {
        val integer = convertToJson(18)
        val double = convertToJson(9.18)
        assertEquals("18", integer.stringify())
        assertEquals("9.18", double.stringify())
    }

    @Test
    fun testNumberEqualValue() {
        val integer1 = convertToJson(18)
        val integer2 = convertToJson(18)
        val double1 = convertToJson(9.18)
        val double2 = convertToJson(9.18)
        assertEquals("Numbers don't match", integer2, integer1)
        assertEquals("Numbers don't match", double2, double1)
    }

    @Test
    fun testBooleanConvert() {
        val t = convertToJson(true)
        val f = convertToJson(false)
        assertEquals("true", t.stringify())
        assertEquals("false", f.stringify())
    }

    @Test
    fun testBooleanEqualValue() {
        val t1 = convertToJson(true)
        val t2 = convertToJson(true)
        val f1 = convertToJson(false)
        val f2 = convertToJson(false)
        assertEquals("Booleans don't match", t2, t1)
        assertEquals("Booleans don't match", f2, f1)
    }

    @Test
    fun testStringConvert() {
        val string = convertToJson("Hello world!")
        assertEquals("\"Hello world!\"", string.stringify())
    }

    @Test
    fun testStringEqualValue() {
        val string1 = convertToJson("Hello world!")
        val string2 = convertToJson("Hello world!")
        assertEquals("Strings don't match", string2, string1)
    }

    @Test
    fun testEmptyStringConvert() {
        val emptyString = convertToJson("")
        assertEquals("\"\"", emptyString.stringify())
    }

    @Test
    fun testEmptyStringEqualValue() {
        val emptyString1 = convertToJson("")
        val emptyString2 = convertToJson("")
        assertEquals("Empty strings don't match", emptyString2, emptyString1)
    }

    @Test
    fun testListConvert() {
        val list = convertToJson(listOf(1, 2, 3))
        assertEquals("""[1,2,3]""", list.stringify())
    }

    @Test
    fun testListEqualValue() {
        val list1 = convertToJson(listOf(1, 2, 3))
        val list2 = convertToJson(listOf(1, 2, 3))
        assertEquals("Strings don't match", list2, list1)
    }

    @Test
    fun testEmptyListConvert() {
        val emptyList = convertToJson(emptyList<Any>())
        assertEquals("[]", emptyList.stringify())
    }

    @Test
    fun testEmptyListEqualValue() {
        val emptyList1 = convertToJson(emptyList<Any>())
        val emptyList2 = convertToJson(emptyList<Any>())
        assertEquals("Empty lists don't match", emptyList2, emptyList1)
    }

    @Test
    fun testEnumConvert() {
        val enum = convertToJson(EvalType.EXAM)
        assertEquals("\"EXAM\"", enum.stringify())
    }

    @Test
    fun testEnumEqualValue() {
        val enum1 = convertToJson(EvalType.EXAM)
        val enum2 = convertToJson(EvalType.EXAM)
        assertEquals("Enums don't match", enum2, enum1)
    }

    @Test
    fun testNullConvert() {
        val nullType = convertToJson(null)
        assertEquals("null", nullType.stringify())
    }

    @Test
    fun testNullEqualValue() {
        val null1 = convertToJson(null)
        val null2 = convertToJson(null)
        assertEquals("Nulls don't match", null2, null1)
    }

    @Test
    fun testEvalItemConvert() {
        val item = convertToJson(EvalItem("project", 0.8, true, EvalType.PROJECT))
        assertEquals(
            """{"name":"project","percentage":0.8,"mandatory":true,"type":"PROJECT"}""",
            item.stringify()
        )
    }

    @Test
    fun testEvalItemEqualValue() {
        val item1 = convertToJson(EvalItem("project", 0.8, true, EvalType.PROJECT))
        val item2 = convertToJson(EvalItem("project", 0.8, true, EvalType.PROJECT))
        assertEquals("Eval items don't match", item2, item1)
    }

    @Test
    fun testCourseConvert() {

        val course = convertToJson(Course(
            "PA", 6, listOf(
                EvalItem("quizzes", 0.2, false, null),
                EvalItem("project", 0.8, true, EvalType.PROJECT)
            )
        ))

        assertEquals(
            """{"name":"PA","credits":6,"evaluation":[{"name":"quizzes","percentage":0.2,"mandatory":false,"type":null},{"name":"project","percentage":0.8,"mandatory":true,"type":"PROJECT"}]}""",
            course.stringify()
        )
    }

    @Test
    fun testCourseEqualValue() {
        val course1 = Course(
            "PA", 6, listOf(
                EvalItem("quizzes", 0.2, false, null),
                EvalItem("project", 0.8, true, EvalType.PROJECT)
            )
        )
        val course2 = Course(
            "PA", 6, listOf(
                EvalItem("quizzes", 0.2, false, null),
                EvalItem("project", 0.8, true, EvalType.PROJECT)
            )
        )
        assertEquals("Course classes don't match", course2, course1)
    }

    @Test
    fun testMapWithIntValuesConvert() {
        val map = convertToJson(mapOf("x" to 1, "y" to 2))
        assertEquals("""{"x":1,"y":2}""", map.stringify())
    }

    @Test
    fun testMapWithIntValuesEqual() {
        val map1 = convertToJson(mapOf("x" to 1, "y" to 2))
        val map2 = convertToJson(mapOf("x" to 1, "y" to 2))
        assertEquals("Maps with Int values don't match", map2, map1)
    }

    @Test
    fun testMapWithDoubleValuesConvert() {
        val map = convertToJson(mapOf("x" to 9.18, "y" to 18.9))
        assertEquals("""{"x":9.18,"y":18.9}""", map.stringify())
    }

    @Test
    fun testMapWithDoubleValuesEqual() {
        val map1 = convertToJson(mapOf("x" to 9.18, "y" to 18.9))
        val map2 = convertToJson(mapOf("x" to 9.18, "y" to 18.9))
        assertEquals("Maps with Double values don't match", map2, map1)
    }

    @Test
    fun testMapWithBooleanValuesConvert() {
        val map = convertToJson(mapOf("a" to true, "b" to false))
        assertEquals("""{"a":true,"b":false}""", map.stringify())
    }

    @Test
    fun testMapWithBooleanValuesEqual() {
        val map1 = convertToJson(mapOf("a" to true, "b" to false))
        val map2 = convertToJson(mapOf("a" to true, "b" to false))
        assertEquals("Maps with Boolean values don't match", map2, map1)
    }

    @Test
    fun testMapWithStringValuesConvert() {
        val map = convertToJson(mapOf("a" to "aa", "b" to "bb"))
        assertEquals("""{"a":"aa","b":"bb"}""", map.stringify())
    }

    @Test
    fun testMapWithStringValuesEqual() {
        val map1 = convertToJson(mapOf("a" to "aa", "b" to "bb"))
        val map2 = convertToJson(mapOf("a" to "aa", "b" to "bb"))
        assertEquals("Maps with String values don't match", map2, map1)
    }

    @Test
    fun testMapWithListValuesConvert() {
        val map = convertToJson(mapOf("numbers" to listOf(1, 2, 3), "booleans" to listOf(true, false)))
        assertEquals("""{"numbers":[1,2,3],"booleans":[true,false]}""", map.stringify())
    }

    @Test
    fun testMapWithListValuesEqual() {
        val map1 = convertToJson(mapOf("numbers" to listOf(1, 2, 3), "booleans" to listOf(true, false)))
        val map2 = convertToJson(mapOf("numbers" to listOf(1, 2, 3), "booleans" to listOf(true, false)))
        assertEquals("Maps with List values don't match", map2, map1)
    }

    @Test
    fun testMapWithEnumValuesConvert() {
        val map = convertToJson(mapOf("eval1" to EvalType.TEST, "eval2" to EvalType.EXAM))
        assertEquals("""{"eval1":"TEST","eval2":"EXAM"}""", map.stringify())
    }

    @Test
    fun testMapWithEnumValuesEqual() {
        val map1 = convertToJson(mapOf("eval1" to EvalType.TEST, "eval2" to EvalType.EXAM))
        val map2 = convertToJson(mapOf("eval1" to EvalType.TEST, "eval2" to EvalType.EXAM))
        assertEquals("Maps with Enum values don't match", map2, map1)
    }

    @Test
    fun testMapWithNullValuesConvert() {
        val map = convertToJson(mapOf("first" to null, "second" to null))
        assertEquals("""{"first":null,"second":null}""", map.stringify())
    }

    @Test
    fun testMapWithNullValuesEqual() {
        val map1 = convertToJson(mapOf("first" to null, "second" to null))
        val map2 = convertToJson(mapOf("first" to null, "second" to null))
        assertEquals("Maps with null values don't match", map2, map1)
    }

    @Test
    fun testMapWithCourseValuesConvert() {
        val course1 = Course(
            "PA", 6, listOf(
                EvalItem("quizzes", 0.2, false, null),
                EvalItem("project", 0.8, true, EvalType.PROJECT)
            )
        )
        val course2 = Course(
            "ICO", 6, listOf(
                EvalItem("project", 1.0, true, EvalType.PROJECT)
            )
        )
        val map = convertToJson(
            mapOf(
                "course1" to course1,
                "course2" to course2
            )
        )
        assertEquals(
            """{"course1":{"name":"PA","credits":6,"evaluation":[{"name":"quizzes","percentage":0.2,"mandatory":false,"type":null},{"name":"project","percentage":0.8,"mandatory":true,"type":"PROJECT"}]},"course2":{"name":"ICO","credits":6,"evaluation":[{"name":"project","percentage":1.0,"mandatory":true,"type":"PROJECT"}]}}""",
            map.stringify()
        )
    }

    @Test
    fun testMapWithCourseValuesEqual() {
        val course1 = Course(
            "PA", 6, listOf(
                EvalItem("quizzes", 0.2, false, null),
                EvalItem("project", 0.8, true, EvalType.PROJECT)
            )
        )
        val course2 = Course(
            "ICO", 6, listOf(
                EvalItem("project", 1.0, true, EvalType.PROJECT)
            )
        )
        val map1 = convertToJson(
            mapOf(
                "course1" to course1,
                "course2" to course2
            )
        )
        val map2 = convertToJson(
            mapOf(
                "course1" to course1,
                "course2" to course2
            )
        )
        assertEquals("Maps with Course data class values don't match", map2, map1)
    }

    @Test
    fun testEmptyMapConvert() {
        val emptyMap = convertToJson(emptyMap<String, Any>())
        assertEquals("{}", emptyMap.stringify())
    }

    @Test
    fun testEmptyMapEqualValue() {
        val emptyMap1 = convertToJson(emptyMap<String, Any>())
        val emptyMap2 = convertToJson(emptyMap<String, Any>())
        assertEquals("Empty maps don't match", emptyMap2, emptyMap1)
    }

    @Test
    fun testMapWithNonStringKeyException() {
        val invalidMap = mapOf(0 to "zero")
        try {
            convertToJson(invalidMap)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Map keys must be Strings", e.message)
        }
    }
}