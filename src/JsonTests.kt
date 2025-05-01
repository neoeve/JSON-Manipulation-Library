fun JsonTests() {
    val array = JsonArray(listOf(JsonNumber(1), JsonNumber(2), JsonNumber(3)))
    val mapped = array.map { if (it is JsonNumber) JsonNumber(it.value.toInt() * 2) else it }
    assert(mapped.stringify() == "[2,4,6]") { "Map test failed" }

    val filtered = array.filter { it is JsonNumber && it.value.toInt() > 1 }
    assert(filtered.stringify() == "[2,3]") { "Filter test failed" }

    val json = JsonObject(mapOf(
        "x" to JsonNumber(10),
        "y" to JsonBoolean(true),
        "z" to JsonNull()
    ))
    assert(json.stringify() == "{\"x\":10,\"y\":true,\"z\":null}") { "Object stringify failed" }

    val invalidArray = JsonArray(listOf(JsonNumber(1), JsonString("a")))
    assert(!invalidArray.validate()) { "Validation should fail for mixed array" }

    val validObject = JsonObject(mapOf(
        "a" to JsonNumber(1),
        "b" to JsonNumber(2)
    ))
    assert(validObject.validate()) { "Valid object should pass validation" }

    println("All tests passed.")
}