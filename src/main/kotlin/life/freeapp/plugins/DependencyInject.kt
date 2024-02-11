package life.freeapp.plugins
import io.ktor.server.application.*
import life.freeapp.service.AnalyzerService
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger
import org.koin.logger.slf4jLogger

val dependencyInjectModule = module {
    single { AnalyzerService() }
}

fun Application.configureDependencyInject() {
    install(Koin){
        slf4jLogger()
        modules(dependencyInjectModule)
    }
}
