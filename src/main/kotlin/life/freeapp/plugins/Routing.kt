package life.freeapp.plugins

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import life.freeapp.service.AnalyzerService
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
            val multipartData = call.receiveMultipart()
            service.upload(multipartData)
            call.respondText("uploaded")
        }



        staticResources("/static", "static")
    }
}

