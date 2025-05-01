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

fun JsonValue.validate(): Boolean {
    return this.validateObjects() && this.validateArrays()
}

fun JsonValue.validateObjects(): Boolean {
    var isValid = true
    val visitedKeys = mutableSetOf<String>()

    this.accept { value ->
        if (value is JsonObject) {
            if (!visitedKeys.addAll(value.members.keys)) {
                isValid = false
            }
        }
    }

    return isValid
}

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

interface Printer {
    fun print(text: String)
    fun newLine()
    override fun toString(): String
}

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

/** Generalizar com função de entrada? */

class CurlyBracketDecorator(val printer: Printer) : Printer {
    override fun print(text: String) {
        printer.print("{${text}}")
    }
    override fun newLine() {
        printer.newLine()
    }
    override fun toString(): String = printer.toString()
}

class QuoteDecorator(val printer: Printer) : Printer {
    override fun print(text: String) {
        printer.print("\"$text\"")
    }
    override fun newLine() {
        printer.newLine()
    }
    override fun toString(): String = printer.toString()
}

class SquareBracketDecorator(val printer: Printer) : Printer {
    override fun print(text: String) {
        printer.print("[${text}]")
    }
    override fun newLine() {
        printer.newLine()
    }
    override fun toString(): String = printer.toString()
}

class ColonDecorator(val printer: Printer) : Printer {
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

    JsonTests()
}