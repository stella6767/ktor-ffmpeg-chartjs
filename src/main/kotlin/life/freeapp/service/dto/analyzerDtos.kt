package life.freeapp.service.dto



data class ChartDto(
    val xValues: List<Double>,
    val yValues: List<Double>,
    val label:String
)

data class AudioAnalyzerDto(
    val waveForm: ChartDto,
    val fftData: ChartDto,
    val stftData: ChartDto,
    val rmsData: ChartDto,
)
