import org.junit.Assert.assertEquals
import org.junit.Test

class Tests {
    val json = JsonObject(
        mapOf(
            "name" to JsonString("Catarina"),
            "age" to JsonNumber(37),
            "isStudent" to JsonBoolean(true),
            "scores" to JsonArray(
                listOf(JsonNumber(15), JsonNumber(14), JsonNumber(18))
            )
        )
    )

    @Test
    fun testJson() {
        val Serialized = json.stringify()
        val string = "{\"name\":\"Catarina\", \"age\":37, \"isStudent\":true, \"scores\":[15, 14, 18]}"
        assertEquals(Serialized, string)
    }
}