import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import java.net.InetSocketAddress
import java.net.URLDecoder
import kotlin.reflect.*
import kotlin.reflect.full.*

/**
 * Annotation used to map HTTP request paths to classes or methods.
 *
 * @property value Path to map to the class or method.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Mapping(val value: String)

/**
 * Annotation used to mark parameters extracted from the query string.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path

/**
 * Annotation used to mark parameters extracted from the query string.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param

/**
 * Sets up an HTTP server and maps routes from controllers and methods based on annotations.
 *
 * @param controllers List of controller classes that define the routes and corresponding methods.
 */
class GetJson(vararg controllers: KClass<*>) {
    private val routeMap = mutableMapOf<Regex, Route>()

    /**
     * Initializes GetJson instance scanning the controllers
     * and mapping routes based on the 'Mapping' annotation.
     */
    init {
        for (controller in controllers) {
            val basePath = controller.findAnnotation<Mapping>()?.value?.trim('/') ?: ""
            val instance = controller.createInstance()
            for (method in controller.memberFunctions) {
                val methodPath = method.findAnnotation<Mapping>()?.value?.trim('/')
                if (methodPath == null) continue
                val fullPath = "/$basePath/$methodPath".replace("//", "/")
                val pathVars = Regex("\\{([^}]+)}").findAll(fullPath).map { it.groupValues[1] }.toList()
                val pattern = Regex("^" + fullPath.replace(Regex("\\{[^}]+}"), "([^/]+)") + "$")
                routeMap[pattern] = Route(pattern, pathVars, instance, method)
            }
        }
    }

    /**
     * Starts the HTTP server on the specified port.
     *
     * @param port Port number where the server will listen for requests.
     */
    fun start(port: Int) {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange -> handleRequest(exchange) }

        server.executor = null
        println("Server started at http://localhost:$port")
        server.start()
    }

    /**
     * Handles incoming HTTP requests by matching the request path to a route and invoking the corresponding method.
     *
     * @param exchange HTTP exchange representing the request and response.
     */
    private fun handleRequest(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            exchange.sendResponseHeaders(405, 0)
            exchange.responseBody.close()
            return
        }
        val path = exchange.requestURI.path
        val route = routeMap.entries.firstOrNull { it.key.matches(path) }?.value
        if (route == null) {
            exchange.sendResponseHeaders(404, 0)
            exchange.responseBody.write("Not Found".toByteArray())
            exchange.responseBody.close()
            return
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

    /**
     * Builds a list of arguments for the function based on the route parameters and the request.
     *
     * @param route Route corresponding to the current request.
     * @param exchange HTTP exchange representing the request.
     * @return List of arguments to pass to the route function.
     */
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

    /**
     * Converts a string value to the corresponding type.
     *
     * @param value String value to convert.
     * @param type Type to that the value should be converted to.
     * @return Converted value of the corresponding type.
     */
    private fun cast(value: String, type: KType): Any = when (type.classifier) {
        Int::class -> value.toInt()
        Double::class -> value.toDouble()
        Boolean::class -> value.toBoolean()
        else -> value
    }

    /**
     * Route in the application with its pattern, path variables, instance and function.
     *
     * @property pattern Regex pattern used to match the request path.
     * @property pathVars List of path variable names.
     * @property instance Instance of the controller containing the method.
     * @property function Function to invoke when the route matches.
     */
    data class Route(
        val pattern: Regex,
        val pathVars: List<String>,
        val instance: Any,
        val function: KFunction<*>
    )
}

/**
 * Controller containing methods mapped to specific routes.
 */
@Mapping("api")
class Controller {
    /**
     * Method mapped to the route '/api/ints'.
     *
     * @return List of integers.
     */
    @Mapping("ints")
    fun demo(): List<Int> = listOf(1, 2, 3)

    /**
     * Method mapped to the route '/api/pair'.
     *
     * @return Pair of strings.
     */
    @Mapping("pair")
    fun obj(): Pair<String, String> = Pair("um", "dois")

    /**
     * Method mapped to the route '/api/path/{pathvar}'.
     *
     * @param pathvar Path parameter extracted from the URL.
     * @return String with the path variable + "!".
     */
    @Mapping("path/{pathvar}")
    fun path(@Path pathvar: String): String = "$pathvar!"

    /**
     * Method mapped to the route '/api/args'.
     *
     * @param n Number of repetitions for the text.
     * @param text Text to repeat.
     * @return Map with the text as a key and the repeated text as a value.
     */
    @Mapping("args")
    fun args(@Param n: Int, @Param text: String): Map<String, String> =
        mapOf(text to text.repeat(n))
}

/**
 * Main function that starts the server on port 8080.
 */
fun main() {
    val app = GetJson(Controller::class)
    app.start(8080)
}