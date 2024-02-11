package life.freeapp.plugins

import com.google.gson.Gson
import io.ktor.server.application.*
import life.freeapp.plugins.dto.FfmpegProperty
import life.freeapp.service.AnalyzerService
import life.freeapp.service.FfmpegService
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger


fun Map<*, *>.toFfmpeg(gson: Gson): FfmpegProperty {
    val ffmpeg = this["ffmpeg"]
    val jsonString = gson.toJson(ffmpeg)
    return gson.fromJson(jsonString, FfmpegProperty::class.java)
        ?: throw IllegalArgumentException("cant find ffmpeg property")
}


private fun dependencyInjectModule(toFfmpeg: FfmpegProperty) = module {
    single { AnalyzerService() }
    single { FfmpegService(toFfmpeg) }
}


fun Application.configureDependencyInject() {

    val map = environment.config.toMap()
    val toFfmpeg = map.toFfmpeg(Gson())

    install(Koin) {
        slf4jLogger()
        modules(dependencyInjectModule(toFfmpeg))
    }
}


