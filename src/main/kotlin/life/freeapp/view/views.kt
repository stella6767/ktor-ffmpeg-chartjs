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


        tableComponent(analyzerDto.rms, analyzerDto.loudness, analyzerDto.truePeak)

        chart(analyzerDto.waveForm)
        chart(analyzerDto.fftData)
        chart(analyzerDto.stftData)
        chart(analyzerDto.rmsData)

        chartJsScript(analyzerDto.waveForm.xValues, analyzerDto.waveForm.yValues, analyzerDto.waveForm.label)
        chartJsScript(analyzerDto.fftData.xValues, analyzerDto.fftData.yValues, analyzerDto.fftData.label)
        chartJsScript(analyzerDto.stftData.xValues, analyzerDto.stftData.yValues, analyzerDto.stftData.label)
        chartJsScript(analyzerDto.rmsData.xValues, analyzerDto.rmsData.yValues, analyzerDto.rmsData.label)
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




fun BODY.tableComponent(rms: Double, loudness: Double, truePeak: Double) {
    div {
        classes = setOf("flex flex-col")
        div {
            classes = setOf("overflow-x-auto sm:-mx-6 lg:-mx-8")
            div {
                classes = setOf("inline-block min-w-full py-2 sm:px-6 lg:px-8")

                div {
                    classes = setOf("overflow-hidden")

                    table {
                        classes = setOf("min-w-full text-left text-sm font-light")

                        tbody {
                            tr {
                                classes = setOf("border-b dark:border-neutral-500")
                                td {
                                    classes = setOf("whitespace-nowrap px-6 py-4 font-medium")
                                    +"rms"

                                }
                                td {
                                    classes = setOf("whitespace-nowrap px-6 py-4")
                                    +"${rms}"
                                }
                            }
                            tr {
                                classes = setOf("border-b dark:border-neutral-500")
                                td {
                                    classes = setOf("whitespace-nowrap px-6 py-4 font-medium")
                                    +"loudness"

                                }
                                td {
                                    classes = setOf("whitespace-nowrap px-6 py-4")
                                    +"${loudness}"
                                }
                            }
                            tr {
                                classes = setOf("border-b dark:border-neutral-500")
                                td {
                                    classes = setOf("whitespace-nowrap px-6 py-4 font-medium")
                                    +"truePeak"
                                }
                                td {
                                    classes = setOf("whitespace-nowrap px-6 py-4")
                                    +"${truePeak}"
                                }
                            }
                        }
                    }

                }
            }

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
        script {
            src = "https://cdn.tailwindcss.com"
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



