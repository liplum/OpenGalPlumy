package net.liplum.plumy

import opengal.experssion.ExprUtils
import opengal.experssion.ExpressionParser
import opengal.tree.*
import java.util.*
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class KwArgTypes(vararg val types: KClass<*>)
class KeywordNoImplementException(msg: String) : RuntimeException(msg)
abstract class Keywordy(var name: String = "") {
    @JvmField var argNum = 0
    @JvmField var isVarargs = false
    @JvmField var varargsNums = listOf(0)
    override fun toString() = name
    open fun gen(context: Whisper, args: List<Any>): AnalyNode {
        throw KeywordNoImplementException(name)
    }
}

fun String.parse(): Any {
    try {
        return this.toInt()
    } catch (_: Exception) {
        try {
            return this.toBooleanStrict()
        } catch (_: Exception) {
            return this
        }
    }
}

fun String.splitArgs(left: Char, right: Char, comma: Char): List<String> {
    val res = LinkedList<String>()
    val cur = StringBuilder()
    var isQuoted = false
    for (c in this) {
        if (c == left) {
            isQuoted = true
        } else if (isQuoted) {
            if (c == right) {
                res.add(cur.toString())
                cur.clear()
                isQuoted = false
            } else {
                cur.append(c)
            }
        } else {
            if (c == comma) {
                res.add(cur.toString())
                cur.clear()
            } else {
                cur.append(c)
            }
        }
    }
    if (cur.isNotEmpty())
        res.add(cur.toString())
    return res
}

object Actionum : Keywordy("action") {
    init {
        argNum = 1
    }
    @JvmStatic
    val EmptyArg = emptyArray<Any>()
    override fun gen(context: Whisper, args: List<Any>): PlainNode {
        val logos = context.logos
        val s = args[0] as String
        val n = ActionNode()
        n.args = EmptyArg
        val inBracket = logos.inBracketRegex.find(s)
        if (inBracket == null) {
            // zero-arg action
            if (
                logos.leftBracketSyl in s ||
                logos.rightBracketSyl in s
            ) throw UnboundParenthesisException(s)
            n.actionName = s
        } else {
            n.actionName = s.allBefore(logos.leftBracketSyl)
            val actionArgStr = inBracket.value.trim()
            if (actionArgStr.isEmpty()) {
                // zero-arg action
                n.actionName = s
            } else {
                val actionArgs = actionArgStr.splitArgs(
                    left = logos.leftQuote,
                    right = logos.rightQuote,
                    comma = logos.commaSyl,
                )
                n.args = Array(actionArgs.size) {
                    actionArgs[it].parse()
                }
            }
        }
        return PlainNode(n)
    }
}

object Ify : Keywordy("if") {
    init {
        argNum = 1
    }
    @KwArgTypes(String::class)
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        val s = args[0] as String
        val tokens = ExprUtils.splitTokens(s)
        val parser = ExpressionParser(tokens)
        val expr = parser.parse<Boolean>()
        return AnalyIfNode(expr)
    }
}

object Elcey : Keywordy("else") {
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        return AnalyElseNode()
    }
}

object Returna : Keywordy("return") {
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        return PlainNode(ReturnNode.X)
    }
}

object Enden : Keywordy("end") {
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        return AnalyIfEndNode()
    }
}

object Entry : Keywordy("entry") {
    init {
        argNum = 1
    }
    @KwArgTypes(String::class)
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        return AnalyEntryNode(args[0] as String)
    }
}

object Bindle : Keywordy("bind") {
    init {
        argNum = 1
    }
    @KwArgTypes(String::class)
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        val n = BindNode()
        val s = args[0] as String
        n.boundName = s.substring(1)
        return PlainNode(n)
    }
}

object Unbindle : Keywordy("unbind") {
    @KwArgTypes(String::class)
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        return PlainNode(UnbindNode())
    }
}

object Stopy : Keywordy("return") {
    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        return PlainNode(StopNode.X)
    }
}

object Yieldy : Keywordy("yield") {
    init {
        isVarargs = true
        varargsNums = listOf(0, 1)
    }

    override fun gen(context: Whisper, args: List<Any>): AnalyNode {
        val n = if (args.isEmpty()) {
            // No arg
            YieldNode()
        } else {
            val s = args[0] as String
            val tokens = ExprUtils.splitTokens(s)
            val parser = ExpressionParser(tokens)
            val parsed = parser.parse<Any>()
            YieldNode().apply { expr = parsed }
        }
        return PlainNode(n)
    }
}