/**
 * Base sealed class representing any JSON value.
 */
sealed class JsonValue

/**
 * Represents a JSON object (key-value pairs).
 * @property members map of keys to JSON values.
 */
class JsonObject(
    val members: Map<String, JsonValue>
) : JsonValue() {

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
class JsonArray(
    val elements: List<JsonValue>
) : JsonValue() {

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
class JsonBoolean(val value: Boolean) : JsonValue()

/** Represents a JSON number. */
class JsonNumber(val value: Number) : JsonValue()

/** Represents a JSON string. */
class JsonString(val value: String) : JsonValue()

/** Represents a JSON null value. */
class JsonNull : JsonValue()

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

/* Generalizar decoradores abaixo numa unica função que aceita uma função de entrada? */

/**
 * Decorator that wraps output with curly brackets `{ ... }`.
 */
class CurlyBracketDecorator(val printer: Printer) : Printer {
    override fun print(text: String) {
        printer.print("{${text}}")
    }
    override fun newLine() {
        printer.newLine()
    }
    override fun toString(): String = printer.toString()
}

/**
 * Decorator that wraps output with quotes `\...\`.
 */
class QuoteDecorator(val printer: Printer) : Printer {
    override fun print(text: String) {
        printer.print("\"$text\"")
    }
    override fun newLine() {
        printer.newLine()
    }
    override fun toString(): String = printer.toString()
}

/**
 * Decorator that wraps output with square brackets `[ ... ]`.
 */
class SquareBracketDecorator(val printer: Printer) : Printer {
    override fun print(text: String) {
        printer.print("[${text}]")
    }
    override fun newLine() {
        printer.newLine()
    }
    override fun toString(): String = printer.toString()
}
/**
 * Decorator responsible for printing key-value pairs in JSON format.
 */
class ColonDecorator(val printer: Printer) : Printer {

    /**
     * Prints a key-value pair with JSON formatting: `"key":value`
     * @param key the string key
     * @param value the associated JsonValue
     */
    fun printPair(key: String, value: JsonValue) {
        val quotedKey = QuoteDecorator(printer)
        quotedKey.print(key)
        printer.print(":")
        value.stringify(printer)
    }

    override fun print(text: String) { printer.print(text) }
    override fun newLine() { printer.newLine() }
    override fun toString(): String = printer.toString()
}

/**
 * Decorator that inserts commas between printed values.
 */
class CommaDecorator(val printer: Printer) : Printer {
    private var first = true
    override fun print(text: String) {
        if (!first) printer.print(",")
        printer.print(text)
        first = false
    }
    override fun newLine() = printer.newLine()
    override fun toString(): String = printer.toString()
}

/**
 * Serializes a JsonValue into a JSON-compliant string using decorators.
 * @param printer optional custom printer for output formatting
 * @return a valid JSON string
 */
fun JsonValue.stringify(printer: Printer = StringPrinter()): String {
    when (this) {
        is JsonNull -> printer.print("null")
        is JsonBoolean -> printer.print(value.toString())
        is JsonNumber -> printer.print(value.toString())
        is JsonString -> {
            val quoted = QuoteDecorator(printer)
            quoted.print(value.replace("\"", "\\\""))
        }

        is JsonArray -> {
            val inner = StringPrinter()
            val commaPrinter = CommaDecorator(inner)
            values.forEach { it.stringify(commaPrinter) }
            val square = SquareBracketDecorator(printer)
            square.print(inner.toString())
        }

        is JsonObject -> {
            val inner = StringPrinter()
            val commaPrinter = CommaDecorator(inner)
            entries.forEach { (key, value) ->
                val colonPrinter = StringPrinter()
                val colon = ColonDecorator(colonPrinter)
                colon.printPair(key, value)
                commaPrinter.print(colonPrinter.toString())
            }
            val curly = CurlyBracketDecorator(printer)
            curly.print(inner.toString())
        }
    }

    return printer.toString()
}

/** Testes generalizados **/
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

    println("Is valid: ${json.validate()}")
    println("Serialized: ${json.stringify()}")

    val filtered = json.filter { it.key != "age" }
    println("Filtered: ${filtered.stringify()}")
}