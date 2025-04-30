sealed class JsonValue {
    abstract fun accept(visitor: JsonVisitor): Boolean
    abstract fun stringify(): String
}

class JsonNull : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visitNull(this)
    override fun stringify() = "null"
}

//Comment

class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visitBoolean(this)
    override fun stringify() = value.toString()
}

class JsonNumber(val value: Number) : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visitNumber(this)
    override fun stringify() = value.toString()
}

class JsonString(val value: String) : JsonValue() {
    override fun accept(visitor: JsonVisitor) = visitor.visitString(this)
    override fun stringify() = "\"${value.replace("\"", "\\\"")}\""
}

class JsonArray(private val elements: List<JsonValue>) : JsonValue() {
    val values: List<JsonValue> get() = elements.toList()

    fun map(transform: (JsonValue) -> JsonValue): JsonArray =
        JsonArray(elements.map(transform))

    fun filter(predicate: (JsonValue) -> Boolean): JsonArray =
        JsonArray(elements.filter(predicate))

    override fun accept(visitor: JsonVisitor): Boolean =
        visitor.visitArray(this)

    override fun stringify(): String =
        elements.joinToString(prefix = "[", postfix = "]") { it.stringify() }
}

class JsonObject(private val members: Map<String, JsonValue>) : JsonValue() {
    val entries: Map<String, JsonValue> get() = members.toMap()

    fun filter(predicate: (Map.Entry<String, JsonValue>) -> Boolean): JsonObject =
        JsonObject(members.filter(predicate))

    override fun accept(visitor: JsonVisitor): Boolean =
        visitor.visitObject(this)

    override fun stringify(): String =
        members.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${it.key}\":${it.value.stringify()}"
        }
}

interface JsonVisitor {
    fun visitNull(value: JsonNull): Boolean
    fun visitBoolean(value: JsonBoolean): Boolean
    fun visitNumber(value: JsonNumber): Boolean
    fun visitString(value: JsonString): Boolean
    fun visitArray(value: JsonArray): Boolean
    fun visitObject(value: JsonObject): Boolean
}

class ValidationVisitor : JsonVisitor {
    override fun visitNull(value: JsonNull) = true
    override fun visitBoolean(value: JsonBoolean) = true
    override fun visitNumber(value: JsonNumber) = true
    override fun visitString(value: JsonString) = true

    override fun visitArray(value: JsonArray): Boolean {
        if (value.values.isEmpty()) return true
        val firstType = value.values.first()::class
        return value.values.all { it::class == firstType && it.accept(this) }
    }

    override fun visitObject(value: JsonObject): Boolean {
        val keys = mutableSetOf<String>()
        return value.entries.all { (k, v) ->
            keys.add(k) && v.accept(this)
        }
    }
}

fun main() {
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

    println("Serialized: ${json.stringify()}")
    println("Is valid: ${json.accept(ValidationVisitor())}")

    val filtered = json.filter { it.key != "age" }
    println("Filtered: ${filtered.stringify()}")
}
