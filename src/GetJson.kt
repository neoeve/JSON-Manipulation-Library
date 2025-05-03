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
    private val controllers = controllers.toList()
    private val routeMap = mutableMapOf<Regex, RouteHandler>()

    fun start(port: Int) {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        controllers.forEach { registerController(it) }

        server.createContext("/") { exchange ->
            if (exchange.requestMethod != "GET") {
                exchange.sendResponseHeaders(405, 0)
                exchange.responseBody.close()
                return@createContext
            }

            val path = exchange.requestURI.path
            val handlerEntry = routeMap.entries.firstOrNull { (regex, _) ->
                regex.matches(path)
            }

            if (handlerEntry == null) {
                exchange.sendResponseHeaders(404, 0)
                exchange.responseBody.write("Not Found".toByteArray())
                exchange.responseBody.close()
                return@createContext
            }

            val handler = handlerEntry.value

            try {
                val response = handler.handle(exchange)
                val responseBody = response.stringify().toByteArray()
                exchange.sendResponseHeaders(200, responseBody.size.toLong())
                exchange.responseBody.write(responseBody)
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

    private fun registerController(controllerKClass: KClass<*>) {
        val basePath = controllerKClass.findAnnotation<Mapping>()?.value?.trim('/') ?: ""
        val controllerInstance = controllerKClass.createInstance()

        for (method in controllerKClass.memberFunctions) {
            val methodPath = method.findAnnotation<Mapping>()?.value?.trim('/') ?: continue
            val fullPath = "/$basePath/$methodPath".replace("//", "/")

            val route = buildRoute(fullPath)
            routeMap[route.pattern] = RouteHandler(route, method, controllerInstance)
        }
    }

    private fun buildRoute(path: String): Route {
        val regexStr = path.replace(Regex("\\{([^}]+)}")) { "([^/]+)" }
        val paramNames = Regex("\\{([^}]+)}").findAll(path).map { it.groupValues[1] }.toList()
        return Route(Regex("^$regexStr$"), paramNames)
    }
}

data class Route(val pattern: Regex, val pathVars: List<String>)

class RouteHandler(
    val route: Route,
    val function: KFunction<*>,
    val instance: Any
) {
    fun handle(exchange: HttpExchange): JsonValue {
        val path = exchange.requestURI.path
        val query = exchange.requestURI.rawQuery ?: ""

        val pathValues = route.pattern.matchEntire(path)
            ?.groupValues?.drop(1)
            ?: throw IllegalArgumentException("Path mismatch")

        val queryParams = parseQueryParams(query)

        val args = function.parameters.map { param ->
            when {
                param.kind == KParameter.Kind.INSTANCE -> instance
                param.hasAnnotation<Path>() -> {
                    val name = param.name!!
                    val index = route.pathVars.indexOf(name)
                    pathValues[index]
                }
                param.hasAnnotation<Param>() -> {
                    val name = param.name!!
                    val raw = queryParams[name] ?: error("Missing param: $name")
                    castParam(raw, param.type)
                }
                else -> null
            }
        }

        val result = function.call(*args.toTypedArray())
        return convertToJson(result)
    }

    private fun castParam(value: String, type: KType): Any {
        return when (type.classifier) {
            Int::class -> value.toInt()
            Double::class -> value.toDouble()
            Boolean::class -> value.toBoolean()
            else -> value
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        return query.split("&").mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2)
                URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
            else null
        }.toMap()
    }
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