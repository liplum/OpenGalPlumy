package net.liplum.plumy.test

import net.liplum.plumy.main
import net.liplum.plumy.test.extension.Memory
import net.liplum.plumy.test.extension.Timing
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(Timing::class, Memory::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@Tag("fast")
class TestCompile {

    @Test
    fun testCompileSingle() {
        main(
            arrayOf(
                "-c",
                galFile.absolutePath
            )
        )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun writeSourceToFile() {
            val temp = System.getenv("TEMP")
            galFile = File(temp, "testGalCompile.gal")
            galFile.writeText(source)
        }
        lateinit var galFile: File
        val source = """
         @file TestGAL  
         @input Plum
         
        :action output(10)
        :action output(1)
        :if @IsTrue
            :action output("Wow, you got true!")
            :if @IsEnd
                :action output("Not yet End!")
            :else
                :action output("Haha, it's end ahead of time.XD")
                :action output(2)
                :action output(1)
                :action output(3)
                :if @Third
                    :action output("In the 3rd.")
                :end
            :end
            :entry WhenTrue
        :else
            :entry WhenFalse
            :action output("Oh no, you got false... TAT")
        :end
        :action output("But you're still alive!")
        
        WhenTrue:
            :bind @Plum
            :action output("YES!")
            :return
        end WhenTrue
        
        WhenFalse:
            :action output("NO!")
            :return
        end WhenFalse  
        """.trimIndent()
    }
}