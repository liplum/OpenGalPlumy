@file:JvmName("PlumyCompiler")

package net.liplum.plumy

import net.liplum.plumy.Parallel.Companion.toParallel
import opengal.core.GalCompiler
import opengal.exceptions.AnalysisException
import java.io.File
import java.util.*
import kotlin.system.exitProcess

const val VERSION = "v0.1.those2"
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        showHead()
        return
    }
    if (args.size == 1) {
        if (args[0].isHelp)
            showHelp()
        else if (args[0].isVersion)
            showVersion()
        return
    }
    try {
        if (args[0].isCompile)
            compileTask(args)
    } catch (e: Exception) {
        println("[Error]${e.totalMessage}")
        exitProcess(1)
    }
    exitProcess(0)
}

val Exception.totalMessage: String
    get() {
        val s = StringBuffer()
        var preCause: Throwable = this
        var curCause: Throwable? = this
        while (curCause != null) {
            s.append(curCause.javaClass.name)
            s.append('\n')
            s.append(curCause.message?:"No message.")
            s.append('\n')
            preCause = curCause
            curCause = curCause.cause
        }
        s.append(preCause.stackTraceToString())
        return s.toString()
    }
private val HelpKeywords = setOf(
    "-help", "-h", "help"
)
private val VersionKeywords = setOf(
    "-help", "-h", "help"
)
private val CompileKeywords = setOf(
    "-c", "-compile", "compile"
)
private val TargetKeywords = setOf(
    "-t", "-target", "target"
)
private val Head = """
██████╗ ██╗     ██╗   ██╗███╗   ███╗██╗   ██╗
██╔══██╗██║     ██║   ██║████╗ ████║╚██╗ ██╔╝
██████╔╝██║     ██║   ██║██╔████╔██║ ╚████╔╝ 
██╔═══╝ ██║     ██║   ██║██║╚██╔╝██║  ╚██╔╝  
██║     ███████╗╚██████╔╝██║ ╚═╝ ██║   ██║   
╚═╝     ╚══════╝ ╚═════╝ ╚═╝     ╚═╝   ╚═╝   
""".trimIndent()
private val HelpInfo = """
compile *.gal to *.node: -c [args] <*.gal> -t <*.node>
args format: -key=[*default*,value1,value2,...]
-lang=[ *default*, zh ]
-batch=[ *file*, folder ]
-recursive=[ *false* , true]
""".trimIndent()
private val String.isHelp: Boolean
    get() = this in HelpKeywords
private val String.isVersion: Boolean
    get() = this in VersionKeywords
private val String.isCompile: Boolean
    get() = this in CompileKeywords
private val String.isTarget: Boolean
    get() = this in TargetKeywords

private fun showHelp() {
    println(HelpInfo)
}

private fun showHead() {
    println(Head)
    showVersion()
}

private fun showVersion() {
    println("Open GAL -- Plumy $VERSION")
}

private typealias Args = Map<String, String>

private class PathNotGivenException(msg: String) : RuntimeException(msg)

private fun compileTask(appArgs: Array<String>) {
    val args = appArgs.extractCompileArgs(1)
    val galIndex = appArgs.indexOfFirst { !it.startsWith('-') }
    if (galIndex < 0) throw PathNotGivenException("Gal path not given")
    val galPath = appArgs[galIndex]
    val nodePath = appArgs.findNodePath()
    compile(args, galPath, nodePath)
}

private class LangInfoNotFoundException(msg: String) : RuntimeException(msg)

private val Analyzer = PlumyAnalyzer()
private val Compiler = GalCompiler().apply {
    analyzer = Analyzer
}

private class NotFolderException(msg: String) : RuntimeException(msg)

private fun compile(args: Args, galPath: String, nodePath: String?) {
    val lang = args.getArgOrDefault("lang")
    val logos = Lang2Info[lang] ?: throw LangInfoNotFoundException("Can't found language $lang")
    Analyzer.info = logos
    val batch = args.getArgOrDefault("batch")
    if (batch == "file") {
        val galFile = File(galPath)
        val nodeFile = if (nodePath == null)
            File(galFile.parentFile, galFile.name.replaceExtension(".gal", ".node"))
        else
            File(nodePath)
        galFile compileInto nodeFile
    } else if (batch == "folder") {
        val recursive = args.getArgOrDefault("recursive").toBoolean()
        val nodeFolder = nodePath.toFile()
        val galFolder = File(galPath)
        if (!galFolder.isDirectory) throw NotFolderException(galFolder.absolutePath)
        galFolder
            .subFiles(recursive)
            .filter { it.name.endsWith(".gal", ignoreCase = true) }
            .toParallel()
            .parallel {
                if (nodeFolder == null)
                    it.parentFile.subFile(it.name.galToNode())
                else
                    it.mapFolder(galFolder, nodeFolder) { n -> n.galToNode() }.createParent()
            }.handle { gal, node, e ->
                println("[Error] ${e.totalMessage} on ${gal.absolutePath} to ${node.absolutePath}")
            }.foreach { gal, node ->
                gal compileInto node
            }
    }
}

fun String.galToNode(): String =
    this.replaceExtension(".gal", ".node")

fun String?.toFile(): File? {
    if (this == null) return null
    return File(this)
}

fun File.subFile(subPath: String): File =
    File(this, subPath)

private inline fun File.mapFolder(
    fromRootFolder: File,
    toRootFolder: File,
    bakeName: (String) -> String = { it }
): File {
    val subParts = this - fromRootFolder
    val toRootPath = toRootFolder.toPath()
    val bakedSubParts = bakeName(subParts)
    val resolved = toRootPath.resolve(bakedSubParts)
    return resolved.toFile()
}
/**
 * @return self
 */
fun File.createParent(): File {
    parentFile.mkdirs()
    return this
}

operator fun File.minus(parent: File): String {
    // a - b
    val a = this.absolutePath
    val b = parent.absolutePath
    return a.substring(b.length + 1)
}

private infix fun File.compileInto(output: File) {
    val nodeLang = Compiler.compileNodeLang(this.readText())
    output.writeBytes(nodeLang)
}

private class FileIteratorException(msg: String) : AnalysisException(msg)

private fun File.subFiles(recursive: Boolean = false): List<File> {
    val res = LinkedList<File>()
    if (recursive) {
        fun recursiveAdd(folder: File) {
            val listFiles = folder.listFiles() ?: throw FileIteratorException(absolutePath)
            for (file in listFiles) {
                if (file.isFile)
                    res.add(file)
                else if (file.isDirectory)
                    recursiveAdd(file)
            }
        }
        recursiveAdd(this)
    } else {
        val listFiles = this.listFiles() ?: throw FileIteratorException(absolutePath)
        for (file in listFiles) {
            if (file.isFile)
                res.add(file)
        }
    }
    return res
}

private class Parallel<T>(
    val list: List<T>
) {
    var gen: (T) -> T = { it }
    var exceptionHandler: (T, T, Exception) -> Unit = { _, _, _ -> }
    fun parallel(gen: (T) -> T): Parallel<T> {
        this.gen = gen
        return this
    }

    fun handle(handler: (T, T, Exception) -> Unit): Parallel<T> {
        exceptionHandler = handler
        return this
    }

    inline fun foreach(func: (T, T) -> Unit): Parallel<T> {
        for (a in list) {
            val b = gen(a)
            try {
                func(a, b)
            } catch (e: Exception) {
                exceptionHandler(a, b, e)
            }
        }
        return this
    }

    companion object {
        fun <T> List<T>.toParallel(): Parallel<T> =
            Parallel(this)
    }
}

private val KeyRegex = "(?<=-).+(?==)".toRegex()
private val ValueRegex = "(?<==).+".toRegex()

private class EmptyArgException(msg: String) : AnalysisException(msg)

private fun Array<String>.extractCompileArgs(
    startIndex: Int
): Args {
    val map = HashMap<String, String>()
    for (i in startIndex until this.size) {
        val arg = this[i]
        if (arg.startsWith("-") && '=' in arg) {
            val key = KeyRegex.find(arg)?.value ?: throw EmptyArgException("The arg key \"$arg\" error.")
            val value = ValueRegex.find(arg)?.value ?: throw EmptyArgException("The arg value \"$arg\" error.")
            map[key] = value
        } else
            break
    }
    return map
}

private val DefaultArgs = mapOf(
    "lang" to "default",
    "batch" to "file",
    "recursive" to "false",
)

private fun Args.getArgOrDefault(key: String) =
    if (key in this)
        this[key]!!
    else
        DefaultArgs[key]!!

private val Lang2Info = mapOf(
    "default" to PlumyLogos.Default,
    "zh" to PlumyLogos.Chinese,
)

private fun Array<String>.findNodePath(): String? {
    for (i in this.indices) {
        val arg = this[i]
        if (arg.isTarget) {
            if (i != size - 1)
                return this[i + 1]
            else
                break
        }
    }
    return null
}
/**
 * Replace the extension name.
 * @param old the old extension starting with dot.
 * @param new the new extension starting with dot.
 */
private fun String.replaceExtension(old: String, new: String) =
    if (endsWith(old, ignoreCase = true))
        removeSuffix(old) + new
    else
        this + new
