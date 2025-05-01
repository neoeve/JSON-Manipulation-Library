import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Mapping(val value: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param

class GetJson(vararg controllers: Any) {
    private val routes = mutableListOf<Route>()

    init {
        controllers.forEach { controller ->
            when (controller) {
                is Controller -> {
                    routes += Route("/api/ints", controller::demo)
                    routes += Route("/api/pair", controller::obj)
                    routes += Route("/api/path/{pathvar}", controller::path)
                    routes += Route("/api/args", controller::args)
                }
            }
        }
    }

    fun start(port: Int) {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        for (route in routes) {
            server.createContext(route.path) { exchange ->
                if (exchange.requestMethod != "GET") {
                    exchange.sendResponseHeaders(405, 0)
                    exchange.responseBody.close()
                    return@createContext
                }

                val path = exchange.requestURI.path
                val pathParams = extractPathParams(path, route.path)
                val queryParams = parseQueryParams(exchange.requestURI)


                /*
                val args = route.function.parameters.mapNotNull { param ->
                    when {
                        param.name == "pathvar" -> pathParams[param.name]
                        param.name == "n" || param.name == "text" -> queryParams[param.name]
                        else -> null
                    }
                }

                val result = route.function.call(*args.toTypedArray())
                val json = toJson(result).stringify()

                val bytes = json.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                */
            }
        }
        server.executor = null
        server.start()
    }

    private fun parseQueryParams(uri: URI): Map<String, String> =
        uri.query?.split("&")?.mapNotNull {
            val (key, value) = it.split("=").let { pair -> pair[0] to pair.getOrElse(1) { "" } }
            key to value
        }?.toMap() ?: emptyMap()

    private fun extractPathParams(path: String, routePath: String): Map<String, String> {
        val pathVariables = routePath.split("/").filter { it.startsWith("{") && it.endsWith("}") }
        val pathParams = mutableMapOf<String, String>()

        pathVariables.forEachIndexed { index, variable ->
            val paramName = variable.substring(1, variable.length - 1)
            val pathParts = path.split("/")
            if (index < pathParts.size) {
                pathParams[paramName] = pathParts[index + 1]
            }
        }

        return pathParams
    }

    private data class Route(
        val path: String,
        val function: Function<*>
    )
}

@Mapping("api")
class Controller {
    // ADICIONAR OUTROS  EXEMPLOS
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
    val app = GetJson(Controller())
    app.start(8080)
}