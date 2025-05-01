import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ConvertToJsonTests {

    @Test
    fun testNumberConvert() {
        val integer = convertToJson(18)
        val double = convertToJson(9.18)
        assertEquals("18", integer.stringify())
        assertEquals("9.18", double.stringify())
    }

    @Test
    fun testBooleanConvert() {
        val t = convertToJson(true)
        val f = convertToJson(false)
        assertEquals("true", t.stringify())
        assertEquals("false", f.stringify())
    }

    @Test
    fun testStringConvert() {
        val json = convertToJson("Hello world!")
        assertEquals("\"Hello world!\"", json.stringify())
    }

    @Test
    fun testListConvert() {
        val list = listOf(18, true, "Hello world!")
        val json = convertToJson(list)
        assertEquals("""[18,true,"Hello world!"]""", json.stringify())
    }

    @Test
    fun testEnumConvert() {
        val enum = EvalType.EXAM
        val json = convertToJson(enum)
        assertEquals("\"EXAM\"", json.stringify())
    }

    @Test
    fun testNullConvert() {
        val json = convertToJson(null)
        assertEquals("null", json.stringify())
    }

    @Test
    fun testEvalItemConvert() {
        val item = EvalItem("project", 0.8, true, EvalType.PROJECT)
        val json = convertToJson(item)
        assertEquals(
            """{"name":"project","percentage":0.8,"mandatory":true,"type":"PROJECT"}""",
            json.stringify()
        )
    }

    @Test
    fun testCourseConvert() {
        val course = Course(
            "PA", 6, listOf(
                EvalItem("quizzes", 0.2, false, null),
                EvalItem("project", 0.8, true, EvalType.PROJECT)
            )
        )

        val json = convertToJson(course)
        assertEquals(
            """{"name":"PA","credits":6,"evaluation":[{"name":"quizzes","percentage":0.2,"mandatory":false,"type":null},{"name":"project","percentage":0.8,"mandatory":true,"type":"PROJECT"}]}""",
            json.stringify()
        )
    }

    @Test
    fun testMapConvert() {
        val map = mapOf("x" to 1, "y" to 2)
        val json = convertToJson(map)
        assertEquals("""{"x":1,"y":2}""", json.stringify())
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