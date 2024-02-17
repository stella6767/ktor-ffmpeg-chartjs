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


//fun calculateSTFT(
//    audioData: DoubleArray,
//    windowSize: Int,
//    hopSize: Int = windowSize / 4
//): Array<DoubleArray> {
//
//    val sampleRate = 22050
//    val startIdx = sampleRate / 4
//    val endIdx = sampleRate / 2
//    val slicedY = audioData.copyOfRange(startIdx, endIdx)
//
//
//    val stftData = Array(windowSize / 2 + 1) { DoubleArray(slicedY.size / hopSize + 1) }
//    val transformer = FastFourierTransformer(
//        DftNormalization.STANDARD
//    )
//
//    for (i in 0 until slicedY.size step hopSize) {
//        val slice = slicedY.copyOfRange(i, minOf(i + windowSize, slicedY.size))
//
//        // Apply window function (Hamming window)
//        val window = DoubleArray(windowSize) { 0.54 - 0.46 * cos(2 * PI * it / (windowSize - 1)) }
//        val windowedSlice = slice.mapIndexed { index, value -> value * window[index] }.toDoubleArray()
//
//        val paddedSamples = padToNextPowerOfTwo(windowedSlice)
//
//        val spectrum =
//            transformer.transform(paddedSamples, TransformType.FORWARD)
//
//        for (j in 0 until minOf(spectrum.size, stftData.size)) {
//            stftData[j][i / hopSize] = spectrum[j].abs()
//        }
//    }
//
//    return stftData
//}

fun transform(
    audioData: DoubleArray,
    windowSize: Int,
    hopSize: Int = windowSize / 4
): DoubleArray {

    val sampleSize = 22050 //power ^ 2
    val sampleRate = 22050.0f

    val Fs = sampleSize.toDouble()

    var frequency = 0.0
    // sin 샘플 데이터를 위한 변수
    val Ts = 1 / Fs
    val t = DoubleArray(sampleSize)
    val input = DoubleArray(sampleSize)

    for (i in 0 until sampleSize) {
        t[i] = Ts * i
    }
    for (i in 0 until sampleSize) {
        input[i] = 2 * sin(2 * PI * 4 * t[i])
    }

    val tempConversion = DoubleArray(input.size / 2)
    //100 is not a power of 2, consider padding for fix | 2의 거듭제곱의 개수로 해야함
    val fft = FastFourierTransformer(DftNormalization.STANDARD)

    try {
        val complx: Array<Complex> = fft.transform(input, TransformType.FORWARD)
        for (i in 0 until (complx.size / 2)) {           // 대칭되는 값을 제외하기위해 절반만을 사용
            val rr = (complx[i].real) / sampleSize * 2 // maginute값 계산을 위한 마사지
            val ri = (complx[i].imaginary) / sampleSize * 2 // maginute값 계산을 위한 마사지
            tempConversion[i] = sqrt((rr * rr) + (ri * ri)) // maginute계산
            val mag = tempConversion[i]
            frequency = ((sampleRate * i) / sampleSize).toDouble() // frequency계산
            println(frequency.toString() + "\t" + mag)
        }
    } catch (e: IllegalArgumentException) {
        println(e)
    }



    return tempConversion
}

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


fun readAudioData(audioInputStream: AudioInputStream): DoubleArray {
    val audioBytes = audioInputStream.readAllBytes()
    val audioData = DoubleArray(audioBytes.size / 2)

    // Convert bytes to doubles (16-bit PCM)
    for (i in audioData.indices) {
        audioData[i] = ((audioBytes[2 * i + 1].toInt() shl 8) or (audioBytes[2 * i].toInt() and 0xff)) / 32768.0
    }

    return audioData
}

internal fun calculateSTFT2(
    audioData: DoubleArray,
    n_fft: Int = 512,
    hopLength: Int = n_fft / 4
): DoubleArray {
    val transformer = FastFourierTransformer(DftNormalization.STANDARD)
    //val numFrames = 1 + (audioData.size - n_fft) / hopLength

    val sampleRate = 1373
    val startIdx = sampleRate / 4
    val endIdx = sampleRate / 2
    val slicedY = Arrays.copyOfRange(audioData, startIdx, endIdx)

    //val toTypedArray = slicedY.map { Complex(it, 0.0) }.toTypedArray()

    val paddedSamples = if (slicedY.size < n_fft) {
        val padding = DoubleArray(n_fft - slicedY.size)
        slicedY + padding
    } else {
        slicedY.sliceArray(0 until n_fft)
    }

    val spectrum =
        transformer.transform(paddedSamples, TransformType.FORWARD)

    val data = DoubleArray(spectrum.size) { abs(spectrum[it].abs()) }

    val dbData = DoubleArray(data.size) { 10 * log10(data[it]) }

    return dbData
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


