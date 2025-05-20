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