@file:Suppress("FunctionName")

package net.liplum.plumy

import opengal.core.IAnalyzer
import opengal.core.NodeTree
import opengal.exceptions.AnalysisException
import opengal.exceptions.NoSuchBlockException
import opengal.tree.BlockEndNode
import opengal.tree.Node
import java.util.*

class PlumyAnalyzer : IAnalyzer {
    var info: PlumyLogos = PlumyLogos.Default

    companion object {
        @JvmStatic val Default = PlumyAnalyzer()
        @JvmStatic val Chinese = PlumyAnalyzer().apply {
            info = PlumyLogos.Chinese
        }
    }

    override fun analyze(code: String): NodeTree {
        // Split the source code line by line
        val lines = splitByLine(code)
        // Split all lines into code blockies and rest
        val (codeBlockies, rest) = splitBlocky(
            kwStartSyls = info.kwStartSyls,
            blockStartSyl = info.kwSyl,
            blockEndKw = info.blockEndSyl,
            lines,
        )
        // Split rest into gal codes and meta
        val (metilas, codes) = separateMetaFromCode(info.metaSyl, rest)
        // Transform the code blockies to (statement) blockies
        val blockies = codeBlockies.toBlocky(info)
        // Make a main blocky using the rest gal codes
        val mainBlocky = CodeBlocky("Main", codes).toBlocky(info).endsWithOrAdd(Stopy)
        // Connect the blockies end to end
        val allBlockies = ArrayList<Blocky>().apply {
            add(mainBlocky)
            addAll(blockies)
        }
        // Create a context
        val context = Whisper().apply {
            logos = info
        }
        // New an array to keep the analy-node nodes
        val nodesInBlocky = arrayOfNulls<List<AnalyNode>>(allBlockies.size)
        // Generate the analy-node nodes
        var totalIndex = 0
        for ((i, blocky) in allBlockies.withIndex()) {
            val blockName = blocky.name
            // Generate all analy-nodes in this blocky with the context
            val genedNodes = blocky.lines.generateNodeBy(context)
            // Add a block entry node at head
            genedNodes.addAtHead(AnalyEntryNode(blockName))
            // Save analy-node nodes
            nodesInBlocky[i] = genedNodes
            // Add a block end node at tail
            genedNodes.add(PlainNode(BlockEndNode.X))
            // Set the index for each analy-node
            var index = 0
            for (node in genedNodes) {
                node.index = index
                if (!node.fakeNode)
                    index++
            }
            // Find all if-end (else optional) block and link them
            genedNodes.linkIfElseEnd()
            // Create current blocky information
            context.block2Info[blockName] = BlockyInfo().apply {
                length = genedNodes.size
                headIndex = totalIndex
            }
            totalIndex += index
        }
        // Transform all analy-node to runtime-node
        val finalNodes = bakeNodes(context, nodesInBlocky)
        // Create a node tree
        val tree = NodeTree(finalNodes).apply {
            metas = HashMap()
            inputs = HashSet()
        }
        // handle with meta
        tree.handleMeta(metilas, context)
        // Output the result
        return tree
    }
}

fun NodeTree.handleMeta(inputMetas: List<Metila>, context: Whisper) {
    val info = context.logos
    for (meta in inputMetas) {
        val metaType = info.name2Meta[meta.name] ?: throw AnalysisException("No such meta ${meta.name}")
        metaType.handle(this, meta.args[0])
    }
}

fun Code.mapCode(context: PlumyLogos): Code {
    // Maps the calcu node
    if (!isMetila(context.metaSyl) && code.startsWith(context.referenceSyl)) {
        return Code("${context.kwSyl}${context.kw2Name[Calcu]} $code")
    }
    return this
}

class PlumyLogos(
    var name: String = "Default"
) {
    @JvmField var kwStartSyls = listOf(':', '@')
    @JvmField val metaSyl = '@'
    @JvmField val referenceSyl = '@'
    @JvmField var kwSyl = ':'
    @JvmField var blockEndSyl = "end"
    @JvmField var commaSyl = ','
    @JvmField var leftBracketSyl = '('
    @JvmField var rightBracketSyl = ')'
    @JvmField var leftQuote = '"'
    @JvmField var rightQuote = '"'
    @JvmField var name2Meta: Map<String, MetaType> = emptyMap()
    @JvmField var name2Kw: Map<String, Keywordy> = emptyMap()
    @JvmField var kw2Name: Map<Keywordy, String> = emptyMap()
    @JvmField var kwRegex = "(?<=:)\\S+".toRegex()
    @JvmField var inBracketRegex = "(?<=\\().*(?=\\))".toRegex()
    @JvmField var quotedRegex = "(?<=\").*(?=\")".toRegex()
    inline fun initWith(func: PlumyLogos.() -> Unit): PlumyLogos {
        this.func()
        kw2Name = name2Kw.reversed()
        inBracketRegex = ("(?<=\\$leftBracketSyl).*(?=\\$rightBracketSyl)").toRegex()
        quotedRegex = "(?<=$leftQuote).*(?=$rightQuote)".toRegex()
        kwRegex = "(?<=$kwSyl)\\S+".toRegex()
        kwStartSyls = listOf(kwSyl, metaSyl)
        return this
    }

    override fun toString() = name

    companion object {
        @JvmStatic
        val Default = PlumyLogos().initWith {
            name2Kw = mapOf(
                "if" to Ify,
                "else" to Elcey,
                "end" to Enden,
                "action" to Actionum,
                "entry" to Entry,
                "bind" to Bindle,
                "unbind" to Unbindle,
                "return" to Returna,
                "stop" to Stopy,
                "yield" to Yieldy,
                "calcu" to Calcu,
            )
            name2Meta = mapOf(
                "file" to FileMeta,
                "input" to InputMeta,
            )
        }
        @JvmStatic
        val Chinese = PlumyLogos("Chinese").initWith {
            name2Kw = mapOf(
                "如果" to Ify,
                "否则" to Elcey,
                "结束" to Enden,
                "行为" to Actionum,
                "进入" to Entry,
                "绑定" to Bindle,
                "解绑" to Unbindle,
                "返回" to Returna,
                "终止" to Stopy,
                "让步" to Yieldy,
                "计算" to Calcu,
            )
            name2Meta = mapOf(
                "文件" to FileMeta,
                "输入" to InputMeta,
            )
            kwSyl = '：'
            blockEndSyl = "结束"
            commaSyl = '，'
            leftBracketSyl = '（'
            rightBracketSyl = '）'
            leftQuote = '“'
            rightQuote = '”'
        }
    }
}

class BlockyInfo {
    /**
     * The real length which considers the fake nodes.
     */
    var length = 0
    /**
     * The real head index which considers the offset and fake nodes.
     */
    var headIndex = 0
}

class Whisper {
    @JvmField var logos: PlumyLogos = PlumyLogos.Default
    @JvmField var block2Info: MutableMap<String, BlockyInfo> = HashMap()
    fun getBlockInfo(blockName: String): BlockyInfo =
        block2Info[blockName] ?: throw NoSuchBlockException(blockName)
}

fun <T> MutableList<T>.addAtHead(e: T) {
    this.add(0, e)
}
/**
 * ##Contract
 * - One statement corresponds to one node.
 * - Size of input equals size of output.
 */
fun List<Statement>.generateNodeBy(context: Whisper): ArrayList<AnalyNode> {
    val res = ArrayList<AnalyNode>(this.size + 2)
    for (code in this) {
        val kw = code.kw
        val node = kw.gen(context, code.args)
        res.add(node)
    }
    return res
}

fun bakeNodes(context: Whisper, nodesInBlocky: Array<List<AnalyNode>?>): ArrayList<Node> {
    val res = ArrayList<Node>()
    for (analyNodes in nodesInBlocky) {
        val nodes = analyNodes ?: throw AnalysisException("Null nodes.")
        for (node in nodes) {
            if (!node.fakeNode)
                res.add(node.regen(context))
        }
    }
    return res
}

class IfElsePair(var ifNode: AnalyIfNode) {
    var elseNode: AnalyElseNode? = null
}

fun ArrayList<AnalyNode>.linkIfElseEnd() {
    val stack = Stack<IfElsePair>()
    for (node in this) {
        if (node is AnalyIfNode) {
            stack.add(IfElsePair(node))
        } else if (node is AnalyElseNode) {
            stack.peek().elseNode = node
        } else if (node is AnalyIfEndNode) {
            val ifElse = stack.pop()
            val ifNode = ifElse.ifNode
            val elseNode = ifElse.elseNode
            ifNode.endNode = node
            if (elseNode != null) {
                ifNode.elseNode = elseNode
                elseNode.endNode = node
            }
        }
    }
}

fun Blocky.endsWithOrAdd(
    kw: Keywordy,
    gen: () -> Statement = { Statement(kw) }
): Blocky {
    if (lines.isEmpty() || lines.last().kw != kw) {
        lines.add(gen())
    }
    return this
}