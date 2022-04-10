package net.liplum.plumy

import opengal.exceptions.AnalysisException
import opengal.experssion.Expression
import opengal.tree.BlockEntryNode
import opengal.tree.ConditionNode
import opengal.tree.JumpNode
import opengal.tree.Node

class FakeNodeRegenException : AnalysisException()
abstract class AnalyNode {
    var fakeNode = false
    var index = 0
    abstract fun regen(context: Whisper): Node
}

class PlainNode(
    var node: Node
) : AnalyNode() {
    override fun regen(context: Whisper): Node = node
    override fun toString() = "[$index]$node"
}

class AnalyIfNoInfoException(msg: String) : AnalysisException(msg)
class AnalyIfNode(
    var condition: Expression<*>
) : AnalyNode() {
    var elseNode: AnalyElseNode? = null
    var endNode: AnalyIfEndNode? = null
    override fun regen(context: Whisper): Node {
        val endNode = endNode ?: throw AnalyIfNoInfoException("No index of end node.")
        val elseNode = elseNode
        val n = ConditionNode()
        @Suppress("UNCHECKED_CAST")
        n.condition = condition as Expression<Boolean>
        n.trueDestination = index + 1
        if (elseNode == null)
            n.falseDestination = endNode.index + 1
        else
            n.falseDestination = elseNode.index + 1
        return n
    }

    override fun toString() = "[$index]:if $condition : $elseNode,$endNode"
}

class AnalyElseNoEndIndexException : AnalysisException()
class AnalyElseNode : AnalyNode() {
    var endNode: AnalyIfEndNode? = null
    override fun regen(context: Whisper): Node {
        val endIndex = endNode?.index ?: throw AnalyElseNoEndIndexException()
        return JumpNode().apply {
            destination = endIndex
        }
    }

    override fun toString() = "[$index]:else $endNode"
}

class AnalyIfEndNode :
    AnalyNode() {
    init {
        fakeNode = true
    }

    override fun regen(context: Whisper): Node =
        throw FakeNodeRegenException()

    override fun toString() = "[$index]:end"
}

class AnalyEntryNode(
    var blockName: String
) : AnalyNode() {
    override fun regen(context: Whisper): Node {
        val info = context.getBlockInfo(blockName)
        return BlockEntryNode().apply {
            blockHead = info.headIndex
        }
    }

    override fun toString() = "[$index]:entry $blockName"
}

class AnalyBlockHeadNode(
    var blockName: String
) : AnalyNode() {
    override fun regen(context: Whisper): Node {
        val info = context.getBlockInfo(blockName)
        return JumpNode().apply {
            destination = info.headIndex + info.length
        }
    }

    override fun toString() = "[$index]$blockName:"
}