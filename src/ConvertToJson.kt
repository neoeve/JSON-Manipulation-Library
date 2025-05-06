import kotlin.reflect.full.*

/**
 * Converts a Kotlin object into a JSON model.
 * Supports Kotlin types: Int, Double, Boolean, String, List, Enums,
 * null, data classes with properties whose type is supported and
 * maps that associate Strings with any of the mentioned types.
 * Uses Kotlin reflection to inspect the structure of data classes.
 *
 * @param obj Kotlin object to convert.
 * @return Corresponding JsonValue instance.
 */
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
            val kClass = obj::class
            val propsByName = kClass.memberProperties.associateBy { it.name }
            val constructorParams = kClass.primaryConstructor?.parameters ?: emptyList()
            val orderedMap = linkedMapOf<String, JsonValue>()
            for (param in constructorParams) {
                val name = param.name ?: continue
                val prop = propsByName[name] ?: continue
                val value = prop.call(obj)
                orderedMap[name] = convertToJson(value)
            }
            JsonObject(orderedMap)
        }
    }
}

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

/**
 * Main function to test JSON model inference.
 */
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