package life.freeapp.service


import amplitudeToDB
import calculateFFT
import calculateRMS
import calculateSTFT
import io.ktor.http.content.*
import life.freeapp.plugins.logger
import life.freeapp.service.dto.AudioAnalyzerDto
import life.freeapp.service.dto.ChartDto
import readAudioData
import readAudioData2
import timesLike
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.sound.sampled.AudioSystem
import kotlin.math.*


/**
 * https://passwd.tistory.com/entry/AWS-cli-s3-%ED%8C%8C%EC%9D%BC%EB%94%94%EB%A0%89%ED%84%B0%EB%A6%AC-%EB%8B%A4%EC%9A%B4%EB%A1%9C%EB%93%9C
 * https://panggu15.github.io/basic/sound_anal/
 * https://github.com/csteinmetz1/pyloudnorm
 * https://github.com/Guadalajara-KUG/Ktor-HTML/blob/master/src/main/kotlin/octuber/content/HomePage.kt
 *
 */

class AnalyzerService(
    private val ffmpegService: FfmpegService
) {

    private val log = logger()
    private val uploadFolderPath = "src/main/resources/static/upload"

    init {
        log.info("koin lazy init check=>${this.hashCode()}")
    }

    suspend fun upload(multipartData: MultiPartData): AudioAnalyzerDto {

        val part =
            multipartData.readPart() ?: throw IllegalArgumentException("cant read file")

        val fileItem = when (part) {
            is PartData.FileItem -> {
                part
            }

            else -> throw IllegalArgumentException("cant read file")
        }

        val originalName =
            fileItem.originalFileName ?: throw IllegalArgumentException("cant read filename")

        val extension = originalName.substringAfterLast('.', "")
        if (extension != "wav") throw IllegalArgumentException("only accept wav")

        val fileBytes = fileItem.streamProvider().readAllBytes()
        val file = File("${uploadFolderPath}/${createFilename(originalName)}")
        file.writeBytes(fileBytes)

        val resamplingFile = ffmpegService.resamplingFile(file)
        part.dispose()


        val audioInputStream = AudioSystem.getAudioInputStream(resamplingFile)
        val audioData: DoubleArray = readAudioData2(audioInputStream)

        val rmsData = getRmsEnergy(audioData)
        val maxRms = rmsData.yValues.maxOrNull() ?: Double.NaN

        return AudioAnalyzerDto(
            rms = maxRms,
            truePeak = getIntegratedLoudness(audioData),
            loudness = calculateTruePeak(audioData),
            waveForm = getWaveformEnergyFromFile(resamplingFile),
            fftData = getFftDataFromAudioFile(audioData),
            stftData = getSpectrumDataFromFile(audioData),
            rmsData = rmsData
        )

    }

    private fun createFilename(filename: String): String {

        val currentTimeMillis = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSSS")
        val format = sdf.format(Date(currentTimeMillis))

        return format + "__" + filename
    }


    fun getIntegratedLoudness(audioData: DoubleArray): Double {
        val sumOfSquares = audioData.fold(0.0)
        { acc, value -> acc + value * value }
        val rms = sqrt(sumOfSquares / audioData.size)
        return rms
    }

    fun calculateTruePeak(originData: DoubleArray): Double {
        val maxSample = originData.maxOrNull() ?: return Double.NaN
        val truePeak = 20 * log10(abs(maxSample))
        return truePeak
    }



    private fun getRmsEnergy(audioData: DoubleArray): ChartDto {


        val rms = calculateRMS(audioData)
        val times = timesLike(rms, 1378)
        val rmsDB = amplitudeToDB(rms)


        return ChartDto(
            xValues = times.toList(),
            yValues = rmsDB.toList(),
            "RmsEnergy"
        )
    }


    private fun getSpectrumDataFromFile(
        audioData: DoubleArray,
    ): ChartDto {

        // Compute STFT
        val dbData = calculateSTFT(audioData)
        val times = timesLike(dbData, 1378)

        return ChartDto(
            xValues = times.toList(),
            yValues = dbData.toList(),
            "Spectrum"
        )
    }




    private fun getFftDataFromAudioFile(audioData: DoubleArray): ChartDto {


        val (doubles, doubles1) = calculateFFT(audioData)

        return ChartDto(
            xValues = doubles.toList().filterIndexed { index, _ -> index % 100 == 0 },
            yValues = doubles1.toList().filterIndexed { index, _ -> index % 100 == 0 },
            label = "PowerSpectrum"
        )
    }


    private fun getWaveformEnergyFromFile(wavFile: File): ChartDto {

        val audioInputStream =
            AudioSystem.getAudioInputStream(wavFile)

        val format = audioInputStream.format
        val frameSize = format.frameSize
        val bytesPerSample = frameSize / format.channels
        val sampleSizeInBits = format.sampleSizeInBits
        val sampleRate = format.sampleRate

        println("!!!=>$sampleRate")

        val buffer = ByteArray(1024 * frameSize)
        val xValues: MutableList<Double> = ArrayList()
        val yValues: MutableList<Double> = ArrayList()

        var rateCount = 0
        val addCycle = 100  //100개 주기로 짤라서,


        var time = 0f

        while (audioInputStream.available() > 0) {
            val bytesRead = audioInputStream.read(buffer, 0, buffer.size)
            var i = 0
            while (i < bytesRead) {
                var value = 0f
                if (sampleSizeInBits == 16) {
                    // Convert two bytes to short (16-bit sample)
                    val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                    value = sample / 32768f // Normalize to [-1.0, 1.0]
                } else if (sampleSizeInBits == 8) {
                    // Convert byte to float (8-bit sample)
                    value = buffer[i] / 128f // Normalize to [-1.0, 1.0]
                }

                if (rateCount % addCycle == 0) {
                    xValues.add(time.toDouble())
                    yValues.add(value.toDouble())
                }
                time += 1f / sampleRate
                i += bytesPerSample
                rateCount++
            }
        }

        audioInputStream.close()

        return ChartDto(
            xValues = xValues.toList(),
            yValues = yValues.toList(),
            label = "waveForm"
        )
    }



}