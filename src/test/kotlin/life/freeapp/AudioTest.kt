package life.freeapp

import life.freeapp.service.AnalyzerService
import java.io.File
import kotlin.test.Test


class AudioTest(


) {

    @Test
    fun justTest() {
        println("test")
    }


    @Test
    fun sliceTest(){

        val myList = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        val slicedList = myList.filterIndexed { index, _ -> index % 3 == 0 }

        println(slicedList)

    }



    @Test
    fun calculateTest() {

        var totalSamples = 0
        val isLife = totalSamples < 10000

        //println(totalSamples)
        //println(100%100)

        while (totalSamples < 10000) {
            while (totalSamples % 100 == 0) {
                println("여기==>" + totalSamples)

                Thread.sleep(100)
                totalSamples++
            }
            //println("여기2==>" + totalSamples)

            totalSamples++
        }

    }


    @Test
    fun audioTest() {

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