# JSON Manipulation Library

## 📌 Overview

This Kotlin library provides a clean and extensible object model for representing and manipulating JSON data programmatically. It supports full composition of JSON structures, functional operations like `map` and `filter`, recursive traversal via visitors, and serialization to valid JSON strings using the **Decorator pattern**.

---

## ✨ Features

- ✅ **Structured JSON Model**: Represents all standard JSON types (`object`, `array`, `string`, `number`, `boolean`, `null`) using a sealed class hierarchy.
- 🧪 **Validation**: Verifies object key uniqueness and array type homogeneity.
- 🧰 **Functional Operations**:
    - `JsonArray.map(transform)`
    - `JsonArray.filter(predicate)`
    - `JsonObject.filter(predicate)`
- 🔁 **Visitor Support**: Recursive structure traversal via `accept(visitor)` for extensibility.
- 🧵 **Stringify with Decorators**: Flexible JSON string generation using composable decorators (e.g. quote wrapping, brackets, commas).
- 💡 **Immutable & Pure**: Operations return new instances without modifying originals.

---

## 📦 Project Structure

| Component       | Description                                |
|-----------------|--------------------------------------------|
| `JsonValue`     | Sealed base class for all JSON elements    |
| `JsonObject`    | Represents JSON objects                    |
| `JsonArray`     | Represents JSON arrays                     |
| `JsonString`    | JSON string literal                        |
| `JsonNumber`    | JSON number (as Kotlin `Number`)           |
| `JsonBoolean`   | JSON booleans                              |
| `JsonNull`      | JSON null representation                   |
| `Printer`       | Interface for controlled output            |
| `*Decorator`    | Output decorators (e.g. `CommaDecorator`)  |

---

## ✅ Example

```kotlin
val json = JsonObject(
    mapOf(
        "name" to JsonString("Catarina"),
        "age" to JsonNumber(37),
        "scores" to JsonArray(listOf(JsonNumber(17), JsonNumber(15)))
    )
)

println(json.validate()) // true
println(json.stringify()) // {"name":"Catarina","age":37,"scores":[17,15]}
```

---

## 🧪 Tests

The project includes unit tests covering:

- Primitive values (string, number, boolean, null)
- Arrays and objects
- Mapping and filtering
- Nested structures
- Validation and serialization logic

Tests are organized by category for clarity.

---

## 📚 Technologies

- **Language**: Kotlin
- **Testing**: JUnit 4
- **Paradigms**: Functional programming, Object-Oriented Design, Decorator & Visitor Patterns

---

## 👩‍💻 Authors
Developed as part of the **Advanced Programming** course, Master in computer engineering (MEI)
2024/2025—University Institute of Lisbon (ISCTE)

Students:
- Catarina Loureiro
- Alexandre Rodrigues