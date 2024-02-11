package life.freeapp.view

import io.ktor.http.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML


fun HTML.index() = layout {
    h1 { +"Audio Analyzer" }
    form {
        method = FormMethod.post
        action = "/upload"
        encType = FormEncType.multipartFormData
        input {
            type = InputType.file
            name = "file"
        }
        input {
            type = InputType.submit
        }
    }
}




fun HTML.layout(e: BODY.() -> Unit) {
    head {
        //    script { src = "/htmx.org/$htmxVersion/dist/htmx.js" }
        //link { href = "/modest-variation.css"; rel = "stylesheet" }
        link(rel = "icon", type = ContentType.Image.JPEG.toString(), href = "/static/favicon.jpeg")
        link(rel = "shortcut icon", type = ContentType.Image.JPEG.toString(), href = "/static/favicon.jpeg")

        link(rel = "stylesheet", href = "https://cdn.simplecss.org/simple.min.css")
        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
        meta {
            httpEquiv = "Content-Type"
            content = "${ContentType.Text.Html}; charset=UTF-8"
        }
        meta { charset = "UTF-8" }
        meta(name = "author", content = "stella")
        meta(name = "keywords", content = arrayOf("Kotlin", "Ktor").joinToString(","))
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
    }

    body {
        e()
    }
}
