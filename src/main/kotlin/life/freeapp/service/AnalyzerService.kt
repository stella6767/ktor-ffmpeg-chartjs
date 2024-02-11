package life.freeapp.service

import io.ktor.http.content.*
import life.freeapp.plugins.logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.sound.sampled.AudioSystem
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt


class AnalyzerService(
    private val ffmpegService: FfmpegService
) {

    private val log = logger()
    private val uploadFolderPath = "src/main/resources/static/upload"

    init {
        log.info("koin lazy init check=>${this.hashCode()}")
    }

    suspend fun upload(multipartData: MultiPartData) {

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
        if (extension != "wav") throw  IllegalArgumentException("only accept wav")

        val fileBytes = fileItem.streamProvider().readAllBytes()
        val file = File("${uploadFolderPath}/${createFilename(originalName)}")
        file.writeBytes(fileBytes)

        ffmpegService.resamplingFile(file)

        part.dispose()
    }


    private fun createFilename(filename: String): String {

        val currentTimeMillis = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSSS")
        val format = sdf.format(Date(currentTimeMillis))

        return format + "__" + filename
    }


    fun test(file: File): Double {

        val audioInputStream = AudioSystem.getAudioInputStream(file)

        val bytes = audioInputStream.readAllBytes()

        val sampleSizeInBits = audioInputStream.format.sampleSizeInBits
        val bytesPerSample = sampleSizeInBits / 8

        var sum = 0.0
        for (i in bytes.indices step bytesPerSample) {
            // Convert bytes to signed 16-bit samples
            val sample = bytes[i].toInt() or (bytes[i + 1].toInt() shl 8)
            sum += sample * sample
        }

        val rms = sqrt(sum / (bytes.size / bytesPerSample))

        return rms
    }


    fun test2(file: File) {

        val audioInputStream = AudioSystem.getAudioInputStream(file)
        val format = audioInputStream.format

        // 읽을 샘플 수 설정 (1초 분량의 샘플)
        val bufferSize = format.frameSize * format.frameRate.toInt()
        val buffer = ByteArray(bufferSize)

        var sum = 0.0
        var count = 0

        // 파일에서 반복하여 샘플을 읽고 RMS 에너지 계산
        var bytesRead: Int
        while (audioInputStream.read(buffer).also { bytesRead = it } != -1) {
            for (i in 0 until bytesRead step format.frameSize) {
                // Convert bytes to signed 16-bit samples
                val sample = buffer[i].toInt() or (buffer[i + 1].toInt() shl 8)
                sum += sample * sample
                count++
            }
        }
        val rms = sqrt(sum / count)
    }


    fun getSpectrumDataFromFile(filePath: String): List<Double> {
        val audioInputStream = AudioSystem.getAudioInputStream(File(filePath))
        val audioBytes = ByteArray(audioInputStream.available())
        audioInputStream.read(audioBytes)

        // 주파수 영역의 크기 설정 (FFT를 위해 2의 제곱수 사용)
        val fftSize = 2048

        // FFT 수행
        val fft = DoubleArray(fftSize)
        for (i in 0 until fftSize) {
            val j = i * 2
            fft[i] = ((audioBytes[j].toInt() shl 8) + audioBytes[j + 1].toInt()).toDouble()
        }

        // FFT 결과를 주파수 스펙트럼으로 변환
        val spectrumData = mutableListOf<Double>()
        for (i in 0 until fftSize step 2) {
            val re = fft[i]
            val im = fft[i + 1]
            val magnitude = 2 * log10(sqrt(re * re + im * im))
            spectrumData.add(magnitude)
        }

        return spectrumData
    }


    fun getWaveformEnergyFromFile(filePath: String): List<Double> {
        val audioInputStream = AudioSystem.getAudioInputStream(File(filePath))
        val sampleSize = audioInputStream.format.sampleSizeInBits / 8 // 샘플 크기 (바이트 단위)
        val frameSize = audioInputStream.format.frameSize // 프레임 크기 (바이트 단위)
        val buffer = ByteArray(frameSize)
        val waveformEnergy = mutableListOf<Double>()

        // WAV 파일을 프레임 단위로 읽어서 각 프레임의 에너지 계산
        while (audioInputStream.read(buffer) != -1) {
            var energy = 0.0
            for (i in 0 until buffer.size step sampleSize) {
                // 각 샘플의 값 읽기 (리틀 엔디언으로 인코딩된 데이터일 경우)
                val sample = buffer[i].toInt() and 0xFF or (buffer[i + 1].toInt() shl 8) // 16비트 샘플링
                energy += sample.toDouble().pow(2) // 샘플의 제곱을 에너지에 더함
            }
            waveformEnergy.add(energy)
        }

        return waveformEnergy
    }


    fun readWavFile(audioFilePath: String) {

        // WAV 파일을 읽어들임
        val file = File(audioFilePath)
        val audioInputStream = AudioSystem.getAudioInputStream(file)

        // 오디오 파일 형식 가져오기
        val fileFormat = AudioSystem.getAudioFileFormat(file)
        val format = audioInputStream.format

        // 샘플 속도(샘플링 레이트) 가져오기
        val sampleRate = format.sampleRate


        // 결과 출력
        println("Sample Rate: $sampleRate Hz")
        // 입력 스트림 닫기
        audioInputStream.close()

    }


    fun getLoudness() {

    }


}