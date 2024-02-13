package life.freeapp.service

import io.ktor.http.content.*
import life.freeapp.plugins.logger
import life.freeapp.service.dto.AudioAnalyzerDto
import life.freeapp.service.dto.ChartDto
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.sound.sampled.AudioInputStream
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



        return AudioAnalyzerDto(
            waveForm = getWaveformEnergyFromFile(resamplingFile),
            fftData = getFftDataFromAudioFile(resamplingFile),
            stftData = getSpectrumDataFromFile(resamplingFile),
            getRmsEnergy(resamplingFile)

        )
    }


    private fun createFilename(filename: String): String {

        val currentTimeMillis = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSSS")
        val format = sdf.format(Date(currentTimeMillis))

        return format + "__" + filename
    }


    fun getRmsEnergy(file: File): ChartDto {

        val audioInputStream = AudioSystem.getAudioInputStream(file)
        // Read the audio data into a byte array
        val audioData = ByteArray(audioInputStream.available())
        audioInputStream.read(audioData)

        // Calculate RMS energy
        val rmsEnergy = calculateRMSEnergy(audioData)

        // Convert RMS energy to decibels (dB)
        val rmsEnergydB = 20 * Math.log10(rmsEnergy)

        // Sample rate of the audio file (Hz)
        val sampleRate = audioInputStream.format.sampleRate

        // Calculate time values (x) based on the number of samples and the sample rate
        val perSample = audioData.size / 2  // Assuming 16-bit audio samples (2 bytes per sample)
        val duration = perSample.toDouble() / sampleRate
        val timeValues = DoubleArray(perSample) { it.toDouble() / sampleRate }

        // Fill the decibel energy values (y) with the calculated RMS energy in dB
        val decibelEnergyValues = DoubleArray(perSample) { rmsEnergydB }

        // Optionally, you can print the time and decibel energy values
        println("Time (s)\tDecibel Energy (dB)")

//        for (i in timeValues.indices) {
//            println("${timeValues[i]}\t${decibelEnergyValues[i]}")
//        }

        return ChartDto(
            xValues = timeValues.toList(),
            yValues = decibelEnergyValues.toList(),
            "RmsEnergy"
        )
    }


    fun calculateRMSEnergy(audioData: ByteArray): Double {
        var sumSquared = 0.0
        val numSamples = audioData.size / 2  // Assuming 16-bit audio samples (2 bytes per sample)

        for (sampleIndex in 0 until numSamples) {
            // Convert two bytes to one short (little endian)
            val sample = (audioData[sampleIndex * 2].toInt() and 0xFF) or (audioData[sampleIndex * 2 + 1].toInt() shl 8)
            // Normalize to range [-1.0, 1.0]
            val normalizedSample = sample / 32768.0
            // Add squared sample to sum
            sumSquared += normalizedSample.pow(2)
        }

        // Calculate RMS energy
        return sqrt(sumSquared / numSamples)
    }




    private fun getSpectrumDataFromFile(
        audioFile: File,
        windowSize: Int = 1024,
        hopSize: Int = windowSize / 2
    ): ChartDto {

        val audioInputStream: AudioInputStream = AudioSystem.getAudioInputStream(audioFile)
        val audioBytes = audioInputStream.readAllBytes()
        val audioData = audioBytes.map { it.toDouble() }.toDoubleArray()

        // Perform STFT and calculate decibel values
        val sampleRate = audioInputStream.format.sampleRate.toDouble()
        val windowSize = 1024 // Adjust as needed
        val overlap = windowSize / 2 // Adjust as needed
        val fft = FastFourierTransformer(DftNormalization.STANDARD)
        val hopSize = windowSize - overlap
        val numFrames = (audioData.size - windowSize) / hopSize + 1
        val xValues = mutableListOf<Double>()
        val yValues = mutableListOf<Double>()

        for (i in 0 until numFrames) {
            val startIdx = i * hopSize
            val frame = audioData.copyOfRange(startIdx, startIdx + windowSize)
            val fftResult = fft.transform(frame, TransformType.FORWARD)

            // Convert FFT result to magnitude and calculate decibel
            val magnitude = fftResult.map { it.abs() }
            val decibel = magnitude.map { 20 * log10(it) }

            // Add time value to xValues array (you can use frame index or another time value)
            xValues.add(i.toDouble())
            // Add decibel values to yValues array
            yValues.addAll(decibel)
        }

        return ChartDto(
            xValues = xValues,
            yValues = yValues,
            "Spectrum"
        )
    }

    private fun applyWindow(data: ByteArray, windowSize: Int, bytesPerSample: Int): DoubleArray {
        val windowedData = DoubleArray(windowSize)
        for (i in 0 until windowSize) {
            var value = 0
            for (j in 0 until bytesPerSample) {
                value = value or ((data[i * bytesPerSample + j].toInt() and 0xFF) shl (j * 8))
            }
            windowedData[i] = value * hammingWindow(i, windowSize)
        }
        return windowedData
    }

    private fun hammingWindow(n: Int, windowSize: Int): Double {
        return 0.54 - 0.46 * cos(2 * Math.PI * n / (windowSize - 1))
    }

    private fun extractChannel(data: DoubleArray, channelIndex: Int, numChannels: Int): DoubleArray {
        val channelData = DoubleArray(data.size / numChannels)
        for (i in channelData.indices) {
            channelData[i] = data[i * numChannels + channelIndex]
        }
        return channelData
    }

    private fun getFftDataFromAudioFile(file: File): ChartDto {

        val audioInputStream = AudioSystem.getAudioInputStream(file)
        val audioData = audioInputStream.readAllBytes()

        val n = audioData.size
        val audioSamples = DoubleArray(n / 2)
        val sampleRate = 1378 //1378  22050


        var i = 0
        var j = 0

        while (i < audioData.size) {
            // Convert two bytes to one short (little endian)
            val sample = ((audioData[i].toInt() and 0xFF) or (audioData[i + 1].toInt() shl 8))
            audioSamples[j] = sample / 32768.0; // Normalize to range [-1, 1]
            i += 2
            j++
        }

        // Pad the audio samples to the next power of two
        val paddedSamples = padToNextPowerOfTwo(audioSamples)

        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
        val fftResult: Array<Complex> = transformer.transform(paddedSamples, TransformType.FORWARD)

        val magnitude = DoubleArray(fftResult.size)
        val frequencies = DoubleArray(fftResult.size)

        val deltaFrequency = sampleRate / n


        for (i in fftResult.indices) {
            magnitude[i] = fftResult[i].abs()
            frequencies[i] = i * deltaFrequency.toDouble()
        }

        return ChartDto(
            xValues = frequencies.toList().filterIndexed { index, _ -> index % 100 == 0 },
            yValues = magnitude.toList().filterIndexed { index, _ -> index % 100 == 0 },
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


    private fun padToNextPowerOfTwo(data: DoubleArray): DoubleArray {
        var length = data.size
        var nextPowerOfTwo = 1
        while (nextPowerOfTwo < length) {
            nextPowerOfTwo *= 2
        }
        if (nextPowerOfTwo != length) {
            val paddedData = DoubleArray(nextPowerOfTwo)
            for (i in data.indices) {
                paddedData[i] = data[i]
            }
            return paddedData
        }
        return data
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



}