package net.liplum.plumy.test

import net.liplum.plumy.PlumyAnalyzer
import net.liplum.plumy.test.extension.Memory
import net.liplum.plumy.test.extension.Timing
import opengal.core.Interpreter
import opengal.core.NodeTree
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(Timing::class, Memory::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestPlumy {
    @Test
    @Tag("fast")
    @Order(0)
    fun testCompile() {
        tree = PlumyAnalyzer.Default.analyze(source)
    }
    @Test
    @Tag("fast")
    @Order(1)
    fun testRun() {
        val inter = Interpreter()
        inter.addAction("output") {
            if (!silent) {
                output.add("${inter.curBound} ")
                for (arg in it) {
                    output.add(arg.toString())
                }
            }
        }
        inter.uniform("Plum", "Plum#5978")
        inter["IsTrue"] = true
        inter["IsEnd"] = false
        inter["Third"] = true
        inter.tree = tree
        if (!silent) {
            inter.beforeExecute { output.add("[" + inter.curIndex + "]") }
            inter.afterExecute { output.add("\n") }
        }

        inter.start()
        while (!inter.isEnd) {
            inter.execute()
        }
        val testVar = inter.get<Any>("TestVar")
        output.add("TestVar is $testVar\n")
    }
    @Test
    @Tag("fast")
    @Order(2)
    fun testCN() {
        val treeCN = PlumyAnalyzer.Chinese.analyze(sourceCN)
        val inter = Interpreter()
        inter.addAction("输出") {
            if (!silent) {
                output.add("${inter.curBound} ")
                for (arg in it) {
                    output.add(arg.toString())
                }
            }
        }
        inter.uniform("Plum", "Plum#5978")
        inter["是真的"] = true
        inter["结束了"] = false
        inter["第三"] = true
        inter.tree = treeCN
        if (!silent) {
            inter.beforeExecute { output.add("[" + inter.curIndex + "]") }
            inter.afterExecute { output.add("\n") }
        }

        inter.start()
        while (!inter.isEnd) {
            inter.execute()
        }
    }
    @AfterEach
    fun printOutput() {
        if (!silent) {
            println("Output:")
            for (line in output) {
                print(line)
            }
            System.out.flush()
        }
        output.clear()
    }

    companion object {
        lateinit var tree: NodeTree
        val output = LinkedList<String>()
        const val silent = false
        val random = Random()
        val source = """
         @file TestGAL  
         @input Plum
         
        :action output(10)
        :action output(1)       #this is a comment
        @TestVar = 9 + 10
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
        val sourceCN = """
         @文件 测试GAL  
         @输入 Plum
         
        ：行为 输出（10）
        ：行为 输出（10）
        ：行为 输出（1）
        ：如果 @是真的
            ：行为 输出（“哇，你得到了真！”）
            ：如果 @结束了
                ：行为 输出(”还没结束呢“)
            ：否则
                ：行为 输出（“哈哈，提前结束啦！”）
                ：行为 输出（2）
                ：行为 输出（1）
                ：行为 输出（3）
                ：如果 @第三
                    ：行为 输出（“在第三个里“）
                ：结束
            ：结束
            ：进入 正确时
        ：否则
            ：进入 错误时
            ：行为 输出（”哦不，你得到了否…… TAT“）
        ：结束
        ：行为 输出（“但你还活着！“）
        
        正确时：
            ：绑定 @Plum
            ：行为 输出（“耶！“）
            ：返回
        结束 正确时
        
        错误时：
            ：行为 输出（”不！“）
            ：返回
        结束 错误时  
        """.trimIndent()
    }
}
