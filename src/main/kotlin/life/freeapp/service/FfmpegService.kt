package life.freeapp.service

import life.freeapp.plugins.dto.FfmpegProperty
import life.freeapp.plugins.logger
import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe

class FfmpegService(
    val property: FfmpegProperty
) {

    private val log = logger()
    private val ffMpeg = FFmpeg(property.mpeg)
    private val ffProbe = FFprobe(property.probe)
    private val executor = FFmpegExecutor(ffMpeg, ffProbe)





}