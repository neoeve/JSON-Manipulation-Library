import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlin.concurrent.thread

class GetJsonTests {

    companion object {
        private val client = OkHttpClient()
        private const val port = 8080
        private const val url = "http://localhost:$port/api"
        private var serverThread: Thread? = null

        @JvmStatic
        @BeforeClass
        fun startServer() {
            serverThread = thread {
                val app = GetJson(Controller::class)
                app.start(port)
            }
            Thread.sleep(1000)
        }

        fun get(url: String): String {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                assertEquals("Expected HTTP 200 for GET $url", 200, response.code)
                return response.body?.string() ?: throw IllegalStateException("Empty response body")
            }
        }
    }

    @Test
    fun testIntsEndpoint() {
        val jsonResponse = get("$url/ints")
        assertEquals("[1,2,3]", jsonResponse)
    }

    @Test
    fun testPairEndpoint() {
        val jsonResponse = get("$url/pair")
        assertEquals("""{"first":"um","second":"dois"}""", jsonResponse.replace(" ", ""))
    }

    @Test
    fun testPathPathvarEndpoint() {
        val jsonResponse = get("$url/path/a")
        assertEquals("\"a!\"", jsonResponse)
    }

    @Test
    fun testArgsEndpoint() {
        val jsonResponse = get("$url/args?n=3&text=PA")
        assertEquals("""{"PA":"PAPAPA"}""", jsonResponse)
    }
}