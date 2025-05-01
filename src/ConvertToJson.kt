fun convertToJson(obj: Any?): JsonValue {
    return when (obj) {
        is Int, is Double -> JsonNumber(obj as Number)
        is Boolean -> JsonBoolean(obj)
        is String -> JsonString(obj)
        is List<*> -> JsonArray(obj.map { convertToJson(it) })
        is Enum<*> -> JsonString(obj.name)
        null -> JsonNull()
        is Map<*, *> -> {
            val converted = obj.entries.associate { (k, v) ->
                require(k is String) { "Map keys must be Strings" }
                k to convertToJson(v)
            }
            JsonObject(converted)
        }
        else -> {
            val fields = obj.javaClass.declaredFields
            val map = mutableMapOf<String, JsonValue>()

            for (field in fields) {
                field.isAccessible = true
                val value = field.get(obj)
                map[field.name] = convertToJson(value)
            }

            JsonObject(map)
        }
    }
}

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

fun main() {
    val course = Course(
        "PA", 6, listOf(
            EvalItem("quizzes", 0.2, false, null),
            EvalItem("project", 0.8, true, EvalType.PROJECT)
        )
    )

    val json = convertToJson(course)
    println(json.stringify())
}