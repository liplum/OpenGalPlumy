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
class TestMultiCompile {
    @Test
    fun testCompileSingle() {
        val temp = System.getenv("TEMP")
        val tempFolder = File(temp, "TestFolderGalCompile")
        val targetFolder = File(temp, "CompiledNodes")
        main(
            arrayOf(
                "-c",
                "-batch=folder",
                "-recursive=true",
                tempFolder.absolutePath,
                "-t",
                targetFolder.absolutePath
            )
        )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun writeSourceToFolder() {
            val temp = System.getenv("TEMP")
            val tempFolder = File(temp, "TestFolderGalCompile")
            tempFolder.mkdirs()
            for (i in 0..10) {
                val file = File(tempFolder, "test$i.gal")
                file.writeText(TestCompile.source)
            }
        }
    }
}