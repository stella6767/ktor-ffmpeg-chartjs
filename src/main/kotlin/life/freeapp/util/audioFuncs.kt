import com.github.psambit9791.jdsp.transform.FastFourier
import com.github.psambit9791.jdsp.transform.ShortTimeFourier
import com.github.psambit9791.jdsp.transform._Fourier
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.util.*
import javax.sound.sampled.AudioInputStream
import kotlin.math.*




fun calculateRMS(y: DoubleArray): DoubleArray {
    val windowSize = 1024  // Choose an appropriate window size
    val hopLength = windowSize / 4  // Choose an appropriate hop length

    val numWindows = (y.size - windowSize) / hopLength + 1
    val rmsValues = DoubleArray(numWindows)

    for (i in 0 until numWindows) {
        val startIdx = i * hopLength
        val endIdx = startIdx + windowSize
        var sumSquared = 0.0

        for (j in startIdx until endIdx) {
            if (j < y.size) {
                sumSquared += y[j].pow(2)
            }
        }

        val rms = sqrt(sumSquared / windowSize)
        rmsValues[i] = rms
    }

    return rmsValues
}

fun timesLike(rmsValues: DoubleArray, sr: Int): DoubleArray {
    val hopLength = 256  // Assuming hop length used during calculation of RMS
    return DoubleArray(rmsValues.size) { it * hopLength.toDouble() / sr.toDouble() }
}

fun amplitudeToDB(rmsValues: DoubleArray): DoubleArray {
    val refValue = 1.0  // Reference value for dB calculation (default: 1.0)
    val amin = 1e-10  // Minimum amplitude (default: 1e-10)
    return DoubleArray(rmsValues.size) { 20.0 * log10(maxOf(amin, rmsValues[it]) / refValue) }
}




fun readAudioData(audioInputStream: AudioInputStream): DoubleArray {
    val audioBytes = audioInputStream.readAllBytes()
    val audioData = DoubleArray(audioBytes.size / 2)

    // Convert bytes to doubles (16-bit PCM)
    for (i in audioData.indices) {
        audioData[i] = ((audioBytes[2 * i + 1].toInt() shl 8) or (audioBytes[2 * i].toInt() and 0xff)) / 32768.0
    }

    return audioData
}


fun readAudioData2(audioInputStream: AudioInputStream): DoubleArray {
    val buffer = ByteArray(audioInputStream.available())
    var bytesRead = 0

    // 오디오 데이터를 바이트 배열로 읽어오기
    while (audioInputStream.available() > 0) {
        bytesRead += audioInputStream.read(buffer, bytesRead, buffer.size - bytesRead)
    }

    // 읽은 바이트 배열을 double 배열로 변환
    val audioData = DoubleArray(bytesRead / 2)  // 16-bit PCM이므로 2로 나누어야 함

    // 16-bit PCM 데이터를 double로 변환
    for (i in 0 until bytesRead / 2) {
        val sample = (buffer[i * 2 + 1].toInt() shl 8 or (buffer[i * 2].toInt() and 0xFF)).toDouble()
        audioData[i] = sample / 32768.0  // Normalize to range [-1.0, 1.0]
    }

    // AudioInputStream 닫기
    audioInputStream.close()

    return audioData
}

fun calculateSTFT(y: DoubleArray, n_fft: Int = 512): DoubleArray {

    val sr = 1378 // Sample rate
    val windowSize = 1024  // Choose an appropriate window size
    val hopLength = windowSize / 4  // Choose an appropriate hop length
//    val sliceStart = sr / 4
//    val sliceEnd = sr / 2

    // Compute the STFT
    val stft = ShortTimeFourier(y, n_fft, hopLength)
    stft.transform()
    val spectrogram = stft.spectrogram(true)

// Calculate absolute values of the spectrogram
    val data = Array(spectrogram.size) { DoubleArray(spectrogram[0].size) }
    for (i in spectrogram.indices) {
        for (j in spectrogram[i].indices) {
            data[i][j] = abs(spectrogram[i][j])
        }
    }

    // Calculate mean amplitude to dB
    val dbData = DoubleArray(data.size) { idx ->
        var sum = 0.0
        for (value in data[idx]) {
            sum += value
        }
        10 * log10(sum / data[idx].size)
    }


    return dbData
}


fun calculateFFT(y: DoubleArray, n_fft: Int = 512): Pair<DoubleArray, DoubleArray> {

    val sampleRate = 1378 // Sample rate
    val windowSize = 1024  // Choose an appropriate window size
    val hopLength = windowSize / 4  // Choose an appropriate hop length

    // Compute FFT
    val dft: _Fourier = FastFourier(y) //Works well for longer signals (>200 points)
    dft.transform()
    val onlyPositive = true
    val magnitude = dft.getMagnitude(onlyPositive) //Positive Absolute

    val f =
        (0 until magnitude.size).map { it.toDouble() / y.size * sampleRate }.toDoubleArray()

    val leftSpectrum = magnitude.copyOfRange(0, magnitude.size / 2)
    val leftF = f.copyOfRange(0, f.size / 2)

    return Pair(leftF, leftSpectrum)
}


