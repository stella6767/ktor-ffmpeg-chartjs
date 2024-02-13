package life.freeapp.view

import io.ktor.http.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import life.freeapp.service.dto.AudioAnalyzerDto
import life.freeapp.service.dto.ChartDto


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


fun charts(analyzerDto: AudioAnalyzerDto): String {

    val (waveForm, fft, stft, rms) = analyzerDto

    return createHTML().body {

        chart(waveForm)
        chart(fft)
        chart(stft)
        chart(rms)

        chartJsScript(waveForm.xValues, waveForm.yValues, waveForm.label)
        chartJsScript(fft.xValues, fft.yValues, fft.label)
        chartJsScript(stft.xValues, stft.yValues, stft.label)
        chartJsScript(rms.xValues, rms.yValues, rms.label)
    }
}

fun BODY.chart(chartDto: ChartDto) {
    div {
        canvas {
            id = "${chartDto.label}"
            width = "1000"
            height = "500"
        }
    }
}


fun HtmlBlockTag.test() {
    div { }
}

private fun BODY.chartJsScript(
    xValues: List<Double>,
    yValues: List<Double>,
    label: String
) {
    script(type = ScriptType.textJavaScript) {
        unsafe {
            raw(
                """                
                  console.log($xValues)
                  console.log('======================================')                     
                  console.log($yValues)  
                                          
                  const $label = document.getElementById('$label');

                    new Chart('$label', {
                    type: 'line',
                    data: {
                        labels: ${xValues},
                        datasets: [{
                            label: '$label',
                            data: ${yValues},
                            borderColor: 'blue',
                            borderWidth: 1,
                            fill: false
                        }]
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



