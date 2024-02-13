package life.freeapp.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.HtmlBlockTag
import life.freeapp.service.AnalyzerService
import life.freeapp.view.chart
import life.freeapp.view.index
import org.koin.ktor.ext.inject


/**
 * https://ktor.io/docs/requests.html#form_data
 */

fun Application.configureRouting() {

    // Lazy inject HelloService
    val service: AnalyzerService by inject()

    routing {
        get("/") {
            call.respondHtml(HttpStatusCode.OK) {
                index()
            }
        }
        post("/upload") {
            val waveFormDto =
                service.upload(call.receiveMultipart())

            call.respondText {
                chart(waveFormDto)
            }

        }

        staticResources("/static", "static")
    }
}

