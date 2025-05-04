import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import java.net.InetSocketAddress
import java.net.URLDecoder
import kotlin.reflect.*
import kotlin.reflect.full.*

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Mapping(val value: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param

class GetJson(vararg controllers: KClass<*>) {
    private val routeMap = mutableMapOf<Regex, Route>()

    init {
        controllers.forEach { controller ->
            val basePath = controller.findAnnotation<Mapping>()?.value?.trim('/') ?: ""
            val instance = controller.createInstance()

            controller.memberFunctions.forEach { method ->
                val methodPath = method.findAnnotation<Mapping>()?.value?.trim('/') ?: return@forEach
                val fullPath = "/$basePath/$methodPath".replace("//", "/")
                val pathVars = Regex("\\{([^}]+)}").findAll(fullPath).map { it.groupValues[1] }.toList()
                val pattern = Regex("^" + fullPath.replace(Regex("\\{[^}]+}"), "([^/]+)") + "$")
                routeMap[pattern] = Route(pattern, pathVars, instance, method)
            }
        }
    }

    fun start(port: Int) {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange ->
            if (exchange.requestMethod != "GET") {
                exchange.sendResponseHeaders(405, 0)
                exchange.responseBody.close()
                return@createContext
            }

            val path = exchange.requestURI.path
            val route = routeMap.entries.firstOrNull { it.key.matches(path) }?.value
            if (route == null) {
                exchange.sendResponseHeaders(404, 0)
                exchange.responseBody.write("Not Found".toByteArray())
                exchange.responseBody.close()
                return@createContext
            }

            try {
                val args = buildArgs(route, exchange)
                val result = route.function.call(*args.toTypedArray())
                val response = convertToJson(result).stringify().toByteArray()
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.write(response)
            } catch (e: Exception) {
                e.printStackTrace()
                exchange.sendResponseHeaders(500, 0)
                exchange.responseBody.write("Internal server error".toByteArray())
            } finally {
                exchange.responseBody.close()
            }
        }

        server.executor = null
        println("Server started at http://localhost:$port")
        server.start()
    }

    private fun buildArgs(route: Route, exchange: HttpExchange): List<Any?> {
        val path = exchange.requestURI.path
        val query = exchange.requestURI.rawQuery ?: ""
        val pathValues = route.pattern.matchEntire(path)?.groupValues?.drop(1) ?: emptyList()
        val queryParams = query.split("&").mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8") else null
        }.toMap()

        return route.function.parameters.map { param ->
            when {
                param.kind == KParameter.Kind.INSTANCE -> route.instance
                param.hasAnnotation<Path>() -> pathValues[route.pathVars.indexOf(param.name!!)]
                param.hasAnnotation<Param>() -> cast(queryParams[param.name] ?: error("Missing param: ${param.name}"), param.type)
                else -> null
            }
        }
    }

    private fun cast(value: String, type: KType): Any = when (type.classifier) {
        Int::class -> value.toInt()
        Double::class -> value.toDouble()
        Boolean::class -> value.toBoolean()
        else -> value
    }

    data class Route(
        val pattern: Regex,
        val pathVars: List<String>,
        val instance: Any,
        val function: KFunction<*>
    )
}

@Mapping("api")
class Controller {
    @Mapping("ints")
    fun demo(): List<Int> = listOf(1, 2, 3)

    @Mapping("pair")
    fun obj(): Pair<String, String> = Pair("um", "dois")

    @Mapping("path/{pathvar}")
    fun path(@Path pathvar: String): String = "$pathvar!"

    @Mapping("args")
    fun args(@Param n: Int, @Param text: String): Map<String, String> =
        mapOf(text to text.repeat(n))
}

fun main() {
    val app = GetJson(Controller::class)
    app.start(8080)
}