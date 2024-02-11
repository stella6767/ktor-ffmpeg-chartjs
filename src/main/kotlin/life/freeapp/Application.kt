package life.freeapp

import io.ktor.server.application.*
import life.freeapp.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureTemplating()
    configureRouting()
}
