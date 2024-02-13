package life.freeapp.view

import io.ktor.http.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import life.freeapp.service.dto.WaveFormDto






fun HTML.index() = layout {
    h1 { +"FFMPEG + KTOR + HTMX Audio Analyzer" }
    form {
        id = "file-form"
        attributes["hx-post"] = "/upload"
        attributes["hx-encoding"] = "multipart/form-data"
//        attributes["hx-target"] = "body"
        input {
            type = InputType.file
            name = "file"
        }
        button {
            +"upload"
        }
        progress {
            id = "progress"
            value = "0"
            max = "100"
        }
    }

    script(type = ScriptType.textJavaScript) {
        unsafe {
            raw(
                """
                htmx.on('#file-form', 'htmx:xhr:progress', function(evt) {
                  htmx.find('#progress').setAttribute('value', evt.detail.loaded/evt.detail.total * 100)
                });
            """.trimIndent()
            )
        }
    }

}

fun chart(waveFormDto: WaveFormDto): String {

    return createHTML().body {
        div {
            canvas {
                id = "myChart"
                width = "1000"
                height = "500"
            }
        }

        chartJsScript(waveFormDto)
    }
}


fun HtmlBlockTag.test(){
    div {  }
}

private fun BODY.chartJsScript(waveFormDto: WaveFormDto) {
    script(type = ScriptType.textJavaScript) {
        unsafe {
            raw(
                """

                  console.log(${waveFormDto.xValues})
                  const ctx = document.getElementById('myChart');

                    new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: ${waveFormDto.xValues},
                        datasets: [{
                            label: 'Waveform',
                            data: ${waveFormDto.yValues},
                            borderColor: 'blue',
                            borderWidth: 1,
                            fill: false
                        }]
                    },
                    options: {
                        responsive: false, // Adjust as needed
                        scales: {
                            x: {
                                title: {
                                    display: true,
                                    text: 'Time'
                                }
                            },
                            y: {
                                title: {
                                    display: true,
                                    text: 'Amplitude'
                                }
                            }
                        }
                    }
                });

            """
            )
        }
    }
}


fun HTML.layout(body: BODY.() -> Unit) {
    head {
        script { src = "https://cdn.jsdelivr.net/npm/chart.js" }
        script {
            src = "https://unpkg.com/htmx.org@1.9.10"
            integrity = "sha384-D1Kt99CQMDuVetoL1lrYwg5t+9QdHe7NLX/SoJYkXDFfX37iInKRy5xLSi8nO7UC"
            attributes["crossorigin"] = "anonymous"
        }

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
        body()
    }
}



