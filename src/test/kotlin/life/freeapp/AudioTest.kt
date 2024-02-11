package life.freeapp

import life.freeapp.service.AnalyzerService
import java.io.File
import kotlin.test.Test


class AudioTest(


) {



    @Test
    fun justTest(){
        println("test")
    }


    @Test
    fun audioTest(){

        val filePath = "/Users/stella6767/IdeaProjects/audio-analyzer/src/test/resources/testfile/test1.wav"
        val file = File(filePath)

//        val rms = analyzerService.testRms(file)
//        println(rms)
//        val spectrumDataFromFile = analyzerService.getSpectrumDataFromFile(filePath)
//
//        //println(spectrumDataFromFile)
//        analyzerService.readWavFile(filePath)

    }


}