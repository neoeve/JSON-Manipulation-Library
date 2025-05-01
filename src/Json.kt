sealed class JsonValue

class JsonObject(
    val members: Map<String, JsonValue>
) : JsonValue() {

    val entries: Map<String, JsonValue> get() = members.toMap()

    fun filter(predicate: (Map.Entry<String, JsonValue>) -> Boolean): JsonObject =
        JsonObject(members.filter(predicate))
}

class JsonArray(
    val elements: List<JsonValue>
) : JsonValue() {

    val values: List<JsonValue> get() = elements.toList()

    fun map(transform: (JsonValue) -> JsonValue): JsonArray =
        JsonArray(elements.map(transform))

    fun filter(predicate: (JsonValue) -> Boolean): JsonArray =
        JsonArray(elements.filter(predicate))
}

class JsonBoolean(val value: Boolean) : JsonValue()

class JsonNumber(val value: Number) : JsonValue()

class JsonString(val value: String) : JsonValue()

class JsonNull : JsonValue()

fun JsonValue.accept(visitor: (JsonValue) -> Unit) {
    when (this) {
        is JsonBoolean -> visitor(this)
        is JsonNumber -> visitor(this)
        is JsonString -> visitor(this)
        is JsonNull -> visitor(this)

        is JsonArray -> {
            visitor(this)
            this.values.forEach {
                it.accept(visitor)
            }
        }

        is JsonObject -> {
            visitor(this)
            this.entries.values.forEach {
                it.accept(visitor)
            }
        }
    }
}

fun JsonValue.stringify(): String =
    when (this) {
        is JsonNull -> "null"
        is JsonBoolean -> value.toString()
        is JsonNumber -> value.toString()
        is JsonString -> "\"${value.replace("\"", "\\\"")}\""
        is JsonArray -> values.joinToString(prefix = "[", postfix = "]") { it.stringify() }
        is JsonObject -> entries.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\":${it.value.stringify()}"}
}

fun JsonValue.validate(): Boolean {
    var isValid = true
    val visitedKeys = mutableSetOf<String>()

    this.accept { value ->
        when (value) {
            is JsonObject -> {
                if (!visitedKeys.addAll(value.members.keys)) {
                    isValid = false
                }
            }
            is JsonArray -> {
                if (value.values.isNotEmpty()) {
                    val firstType = value.values.first()::class
                    if (!value.values.all { it::class == firstType }) {
                        isValid = false
                    }
                }
            }

            is JsonBoolean, is JsonNull, is JsonNumber, is JsonString -> {}
        }
    }

    return isValid
}

fun main() {
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

    println("Serialized: ${json.stringify()}")
    val filtered = json.filter { it.key != "age" }
    println("Filtered: ${filtered.stringify()}")

    val scores = json.entries["scores"]
    if (scores is JsonArray) {
        val incrementedScores = scores.map {
            if (it is JsonNumber) JsonNumber(it.value.toInt() + 1) else it
        }
        println("Mapped scores: ${incrementedScores.stringify()}")

        val highScores = scores.filter {
            it is JsonNumber && it.value.toInt() > 85
        }
        println("Filtered scores: ${highScores.stringify()}")
    }

    JsonTests()
}