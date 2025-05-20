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
 * Annotation used to mark parameters extracted from the path variables.
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
    private val routeMap = mutableListOf<Route>()
    private val controllerInstances = mutableMapOf<KClass<*>, Any>()

    /**
     * Initializes GetJson instance scanning the controllers
     * and mapping routes based on the 'Mapping' annotation.
     */
    init {
        controllers.forEach { controller ->
            val basePath = controller.findAnnotation<Mapping>()?.value?.trim('/') ?: ""
            val instance = controllerInstances.getOrPut(controller) { controller.createInstance() }
            for (method in controller.memberFunctions) {
                val mappingAnnotation = method.findAnnotation<Mapping>()
                if (mappingAnnotation == null) continue
                val methodPath = mappingAnnotation.value.trim('/')
                if (methodPath.isEmpty()) continue
                val fullPath = listOf(basePath, methodPath).filter { it.isNotEmpty() }.joinToString("/")
                val segments = fullPath.split("/")
                val pathParamNames = segments.mapNotNull { s ->
                    if (s.startsWith("{") && s.endsWith("}")) s.drop(1).dropLast(1) else null
                }
                routeMap.add(Route(segments, pathParamNames, instance, method))
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
     * Handles incoming HTTP requests by matching the request path to a route,
     * building a list of arguments from path and query parameters
     * and invoking the corresponding controller method.
     *
     * @param exchange HTTP exchange representing the request and response.
     */
    private fun handleRequest(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            exchange.sendResponseHeaders(405, 0)
            exchange.responseBody.close()
            return
        }

        val requestPathSegments = exchange.requestURI.path.trim('/').split("/").filter { it.isNotEmpty() }
        val queryParams = (exchange.requestURI.rawQuery ?: "")
            .split("&")
            .mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) {
                    URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
                } else null
            }.toMap()
        val route = routeMap.firstOrNull { routeSegments ->
            if (routeSegments.fullPathSegments.size != requestPathSegments.size) {
                false
            } else {
                var match = true
                for (i in routeSegments.fullPathSegments.indices) {
                    val r = routeSegments.fullPathSegments[i]
                    val req = requestPathSegments[i]
                    if (!(r.startsWith("{") && r.endsWith("}")) && r != req) {
                        match = false
                        break
                    }
                }
                match
            }
        }

        if (route == null) {
            exchange.sendResponseHeaders(404, 0)
            exchange.responseBody.write("Not Found".toByteArray())
            exchange.responseBody.close()
            return
        }

        try {
            val pathValues = mutableMapOf<String, String>()
            for (i in route.fullPathSegments.indices) {
                val r = route.fullPathSegments[i]
                val req = requestPathSegments[i]
                if (r.startsWith("{") && r.endsWith("}")) {
                    pathValues[r.drop(1).dropLast(1)] = req
                }
            }
            val args = route.function.parameters.map { param ->
                when {
                    param.kind == KParameter.Kind.INSTANCE -> route.instance
                    param.hasAnnotation<Path>() -> pathValues[param.name]
                        ?: error("Missing path var: ${param.name}")
                    param.hasAnnotation<Param>() -> cast(queryParams[param.name]
                        ?: error("Missing query param: ${param.name}"), param.type)
                    else -> null
                }
            }
            val result = route.function.call(*args.toTypedArray())
            val response = convertToJson(result).stringify().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
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
     * Route in the application with its path segments, path parameter names, instance and function.
     *
     * @property fullPathSegments List of path segments used to match the request path.
     * @property pathParamNames List of path parameter names extracted from the segments.
     * @property instance Instance of the controller containing the method.
     * @property function Function to invoke when the route matches.
     */
    data class Route(
        val fullPathSegments: List<String>,
        val pathParamNames: List<String>,
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
    fun path(@Path pathvar: String): String = pathvar + "!"

    /**
     * Method mapped to the route '/api/args'.
     *
     * @param n Number of repetitions for the text.
     * @param text Text to repeat.
     * @return Map with the text as a key and the repeated text as a value.
     */
    @Mapping("args")
    fun args(@Param n: Int, @Param text: String): Map<String, String> = mapOf(text to text.repeat(n))
}

/**
 * Main function that starts the server on port 8080.
 */
fun main() {
    val app = GetJson(Controller::class)
    app.start(8080)
}