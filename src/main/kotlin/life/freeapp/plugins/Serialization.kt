package life.freeapp.plugins

import com.google.gson.Gson
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import java.text.DateFormat

fun Application.configureSerialization() {

    install(ContentNegotiation){
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

}