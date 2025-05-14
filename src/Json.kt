/**
 * interface representing any JSON value.
 */
interface JsonValue

/**
 * Represents a JSON object (key-value pairs).
 * @property members map of keys to JSON values.
 */
data class JsonObject(
    val members: Map<String, JsonValue>
)
    : JsonValue {

    /**
     * Exposes a defensive copy of the members.
     */
    val entries: Map<String, JsonValue> get() = members.toMap()

    /**
     * Filters key-value pairs based on the given predicate.
     * @return a new JsonObject with filtered entries.
     */
    fun filter(predicate: (Map.Entry<String, JsonValue>) -> Boolean): JsonObject =
        JsonObject(members.filter(predicate))
}

/**
 * Represents a JSON array (ordered list of JSON values).
 * @property elements list of JSON values.
 */
data class JsonArray(
    val elements: List<JsonValue>
) : JsonValue {

    /**
     * Returns a copy of the elements.
     */
    val values: List<JsonValue> get() = elements.toList()

    /**
     * Applies a transformation to each element.
     * @return a new JsonArray with transformed elements.
     */
    fun map(transform: (JsonValue) -> JsonValue): JsonArray =
        JsonArray(elements.map(transform))

    /**
     * Filters elements based on the given predicate.
     * @return a new JsonArray with filtered elements.
     */
    fun filter(predicate: (JsonValue) -> Boolean): JsonArray =
        JsonArray(elements.filter(predicate))
}

/** Represents a JSON boolean value. */
data class JsonBoolean(val value: Boolean) : JsonValue

/** Represents a JSON number. */
data class JsonNumber(val value: Number) : JsonValue

/** Represents a JSON string. */
data class JsonString(val value: String) : JsonValue

/** Represents a JSON null value. */
data class JsonNull(val ignored: Unit = Unit) : JsonValue

/**
 * Recursively visits each node in the JSON structure, applying the given visitor function.
 * @param visitor function to apply to each JsonValue in the tree.
 */
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

/**
 * Validates the current JSON structure for:
 * - Unique keys in each object
 * - Homogeneous types in arrays
 * @return true if the structure is valid
 */
fun JsonValue.validate(): Boolean {
    return this.validateObjects() && this.validateArrays()
}

/**
 * Validates that all objects have unique keys.
 * @return true if all objects are valid
 */
fun JsonValue.validateObjects(): Boolean {
    var isValid = true

    this.accept { value ->
        if (value is JsonObject) {
            val keys = value.members.keys
            if (keys.size != keys.toSet().size) {
                isValid = false
            }
        }
    }

    return isValid
}

/**
 * Validates that all arrays contain only values of the same type (if not empty).
 * @return true if all arrays are homogeneous
 */
fun JsonValue.validateArrays(): Boolean {
    var isValid = true

    this.accept { value ->
        if (value is JsonArray) {
            if (value.values.isNotEmpty()) {
                val firstType = value.values.first()::class
                if (!value.values.all { it::class == firstType }) {
                    isValid = false
                }
            }
        }
    }

    return isValid
}

/**
 * Interface for printing JSON components with optional formatting or decoration.
 */
interface Printer {
    fun print(text: String)
    fun newLine()
    override fun toString(): String
}

/**
 * A Printer implementation that accumulates printed output into a StringBuilder.
 */
class StringPrinter : Printer   {
    private val builder = StringBuilder()

    override fun print(text: String) {
        builder.append(text)
    }

    override fun newLine() {
        builder.append("\n")
    }

    override fun toString(): String = builder.toString()
}

/**
 * Serializes a JsonValue into a JSON-compliant string
 * @param printer optional custom printer for output formatting
 * @return a valid JSON string
 */
fun JsonValue.stringify(printer: Printer = StringPrinter()): String {
    when (this) {
        is JsonNull -> printer.print("null")
        is JsonBoolean -> printer.print(value.toString())
        is JsonNumber -> printer.print(value.toString())

        is JsonString -> {
            val escaped = value.replace("\"", "\\\"")
            printer.print("\"$escaped\"")
        }

        is JsonArray -> {
            printer.print("[")
            values.forEachIndexed { index, item ->
                item.stringify(printer)
                if (index < values.lastIndex) printer.print(",")
            }
            printer.print("]")
        }

        is JsonObject -> {
            printer.print("{")
            entries.entries.forEachIndexed { index, (key, value) ->
                val escapedKey = key.replace("\"", "\\\"")
                printer.print("\"$escapedKey\":")
                value.stringify(printer)
                if (index < entries.size - 1) printer.print(",")
            }
            printer.print("}")
        }
    }

    return printer.toString()
}
