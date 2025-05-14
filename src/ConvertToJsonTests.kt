import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Represents a course, including name, number of credits
 * and a list of evaluation items.
 */

data class Course(
    val name: String,
    val credits: Int,
    val evaluation: List<EvalItem>
)

/**
 * Represents an evaluation item of a course.
 *
 * @property name Name of the evaluation component.
 * @property percentage Weight of the component.
 * @property mandatory If the component is mandatory.
 * @property type Type of evaluation (TEST, PROJECT, EXAM) or null.
 */
data class EvalItem(
    val name: String,
    val percentage: Double,
    val mandatory: Boolean,
    val type: EvalType?
)

/**
 * Represents evaluation types.
 */
enum class EvalType {
    TEST, PROJECT, EXAM
}
class ConvertToJsonTests {

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
    fun testListConvert() {
        val list = convertToJson(listOf(18, true, "Hello world!"))
        assertEquals("""[18,true,"Hello world!"]""", list.stringify())
    }

    @Test
    fun testListEqualValue() {
        val list1 = convertToJson(listOf(18, true, "Hello world!"))
        val list2 = convertToJson(listOf(18, true, "Hello world!"))
        assertEquals("Strings don't match", list2, list1)
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
    fun testMapConvert() {
        val map = convertToJson(mapOf("x" to 1, "y" to 2))
        assertEquals("""{"x":1,"y":2}""", map.stringify())
    }

    @Test
    fun testMapEqualValue() {
        val map1 = convertToJson(mapOf("x" to 1, "y" to 2))
        val map2 = convertToJson(mapOf("x" to 1, "y" to 2))
        assertEquals("Maps don't match", map2, map1)
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