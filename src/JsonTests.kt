import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTests {
    @Test
    fun testFullJsonStructure() {
        val json = JsonObject(
            mapOf(
                "name" to JsonString("Catarina"),
                "age" to JsonNumber(37),
                "isStudent" to JsonBoolean(true),
                "scores" to JsonArray(
                    listOf(JsonNumber(17), JsonNumber(15), JsonNumber(18))
                )
            )
        )

        assertTrue("Full JSON should validate", json.validate())
        assertEquals(
            "Full JSON stringify failed",
            "{\"name\":\"Catarina\",\"age\":37,\"isStudent\":true,\"scores\":[17,15,18]}",
            json.stringify()
        )
    }

    @Test
    fun testJsonBooleanStringify() {
        val jsonTrue = JsonBoolean(true)
        val jsonFalse = JsonBoolean(false)
        assertEquals("JsonBoolean true stringify failed", "true", jsonTrue.stringify())
        assertEquals("JsonBoolean false stringify failed", "false", jsonFalse.stringify())
    }

    @Test
    fun testJsonNumberStringify() {
        val number = JsonNumber(42)
        assertEquals("JsonNumber stringify failed", "42", number.stringify())
    }

    @Test
    fun testJsonStringStringify() {
        val string = JsonString("hello \"world\"")
        assertEquals("JsonString stringify with escape failed", "\"hello \\\"world\\\"\"", string.stringify())

    }

    @Test
    fun testArrayMapping() {
        val array = JsonArray(listOf(JsonNumber(1), JsonNumber(2), JsonNumber(3)))
        val mapped = array.map { if (it is JsonNumber) JsonNumber(it.value.toInt() * 2) else it }
        assertEquals("Map test failed", "[2,4,6]", mapped.stringify())
    }

    @Test
    fun testArrayFiltering() {
        val array = JsonArray(listOf(JsonNumber(1), JsonNumber(2), JsonNumber(3)))
        val filtered = array.filter { it is JsonNumber && it.value.toInt() > 1 }
        assertEquals("Filter test failed", "[2,3]", filtered.stringify())
    }

    @Test
    fun testObjectStringify() {
        val json = JsonObject(mapOf(
            "x" to JsonNumber(10),
            "y" to JsonBoolean(true),
            "z" to JsonNull()
        ))
        assertEquals("Object stringify failed", "{\"x\":10,\"y\":true,\"z\":null}", json.stringify())
    }

    @Test
    fun testInvalidArrayValidation() {
        val invalidArray = JsonArray(listOf(JsonNumber(1), JsonString("a")))
        assertEquals("Validation should fail for mixed array", false, invalidArray.validate())
    }

    @Test
    fun testValidObjectValidation() {
        val validObject = JsonObject(mapOf(
            "a" to JsonNumber(1),
            "b" to JsonNumber(2)
        ))
        assertTrue("Valid object should pass validation", validObject.validate())
    }

    @Test
    fun testNestedValidation() {
        val nested = JsonObject(
            mapOf(
                "nestedArray" to JsonArray(listOf(JsonNumber(1), JsonNumber(2))),
                "nestedObject" to JsonObject(mapOf("k" to JsonString("v")))
            )
        )
        assertTrue("Nested structure should validate", nested.validate())
    }

    @Test
    fun testEmptyArrayStringify() {
        val array = JsonArray(emptyList())
        assertEquals("Empty array should stringify to []", "[]", array.stringify())
    }

    @Test
    fun testEmptyObjectStringify() {
        val obj = JsonObject(emptyMap())
        assertEquals("Empty object should stringify to {}", "{}", obj.stringify())
    }

    @Test
    fun testEmptyStringValue() {
        val str = JsonString("")
        assertEquals("Empty string should stringify to \"\"", "\"\"", str.stringify())
    }

    @Test
    fun testEmptyArrayValidation() {
        val array = JsonArray(emptyList())
        assertEquals("Empty array should validate", true, array.validate())
    }

    @Test
    fun testEmptyObjectValidation() {
        val obj = JsonObject(emptyMap())
        assertTrue("Empty object should validate", obj.validate())
    }

    @Test
    fun testArrayWithOnlyNullsValidation() {
        val array = JsonArray(listOf(JsonNull(), JsonNull()))
        assertEquals("Array with only nulls should validate", true, array.validate())
    }

    @Test
    fun testArrayWithNullAndValueFailsValidation() {
        val array = JsonArray(listOf(JsonNull(), JsonNumber(1)))
        assertEquals("Array with null and number should fail validation", false, array.validate())
    }

    @Test
    fun testManualDuplicateKeyFailsValidation() {
        val object1 = JsonObject(mapOf("a" to JsonNumber(1)))
        val object2 = JsonObject(mapOf("a" to JsonNumber(2)))
        val parent = JsonArray(listOf(object1, object2))
        assertEquals("Two objects with same keys should validate (independently)", true, parent.validate())
    }

    @Test
    fun testStringWithEscapes() {
        val json = JsonString("line1\nline2\t\"quoted\"")
        assertEquals(
            "String with escape characters",
            "\"line1\nline2\t\\\"quoted\\\"\"",
            json.stringify()
        )
    }
    @Test
    fun testDeeplyNestedStructure() {
        val deep = JsonObject(mapOf(
            "data" to JsonArray(listOf(
                JsonObject(mapOf("value" to JsonString("ok"))),
                JsonObject(mapOf("value" to JsonString("fail")))
            ))
        ))
        assertEquals(
            "Deep structure should serialize correctly",
            "{\"data\":[{\"value\":\"ok\"},{\"value\":\"fail\"}]}",
            deep.stringify()
        )
    }
    @Test
    fun testFilterObjectRemovesOneKey() {
        val obj = JsonObject(
            mapOf(
                "name" to JsonString("Catarina"),
                "age" to JsonNumber(37),
                "isStudent" to JsonBoolean(true)
            )
        )

        val objexpected = JsonObject(
            mapOf(
                "name" to JsonString("Catarina"),
                "isStudent" to JsonBoolean(true)
            )
        )

        val filtered = obj.filter { it.key != "age" }

        assertEquals(
            "Filtering should remove 'age'",
            objexpected,
            filtered
        )
    }

    @Test
    fun testFilterObjectKeepsAll() {
        val obj = JsonObject(
            mapOf("a" to JsonNumber(1), "b" to JsonNumber(2))
        )

        val filtered = obj.filter { true }

        assertEquals(
            "Filtering with always-true should keep all entries",
            "{\"a\":1,\"b\":2}",
            filtered.stringify()
        )
    }

    @Test
    fun testFilterObjectRemovesAll() {
        val obj = JsonObject(
            mapOf("a" to JsonNumber(1), "b" to JsonNumber(2))
        )

        val filtered = obj.filter { false }

        assertEquals(
            "Filtering with always-false should return empty object",
            "{}",
            filtered.stringify()
        )
    }

}