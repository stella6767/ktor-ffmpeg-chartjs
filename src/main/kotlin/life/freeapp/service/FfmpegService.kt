package life.freeapp.service

import ch.qos.logback.core.util.FileUtil
import life.freeapp.plugins.dto.FfmpegProperty
import life.freeapp.plugins.logger
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.builder.FFmpegOutputBuilder
import java.io.File
import java.util.*

class FfmpegService(
    val property: FfmpegProperty
) {

    private val log = logger()
    private val ffMpeg = FFmpeg(property.mpeg)
    private val ffProbe = FFprobe(property.probe)
    private val executor = FFmpegExecutor(ffMpeg, ffProbe)

    private val savePath = "src/main/resources/static/convert"

    fun resamplingFile(file: File): File {

        val outPath =
            "$savePath/${file.name}"

        log.info("outPath: $outPath")

        try {
            val builder = createFfmpegBuilder(file, outPath)
            builder.setAudioSampleRate(1373)
            executor.createJob(builder.done()) { p ->
                if (p.isEnd) {
                    log.info("make success")
                    file.delete()
                }
            }.run()
            return File(outPath)
        } catch (e: Exception) {
            file.delete()
            throw e
        }

    }


    private fun createFfmpegBuilder(file: File, outPath: String): FFmpegOutputBuilder {
        return FFmpegBuilder()
            .overrideOutputFiles(true)
            .setInput(file.absolutePath)
            .addOutput(outPath)
    }

}