package net.liplum.plumy

import opengal.exceptions.AnalysisException
import java.util.*

/**
 * Code can be a meta or statement but not a blocky(start/end)
 */
@JvmInline
value class Code(val code: String) {
    fun isMetila(metilaSyl: Char): Boolean =
        code.startsWith(metilaSyl) && '=' !in code
}

data class Metila(val name: String, val args: List<String>)
class CodeBlocky(
    val name: String,
    val lines: List<Code>
) {
    val length: Int
        get() = lines.size
}

class Blocky(
    val name: String,
    val lines: MutableList<Statement>
) {
    val length: Int
        get() = lines.size
}

fun CodeBlocky.toBlocky(context: PlumyLogos): Blocky =
    Blocky(name, lines.toStatement(context))

fun List<CodeBlocky>.toBlocky(context: PlumyLogos): List<Blocky> {
    val res = ArrayList<Blocky>(this.size)
    for (codeB in this) {
        res.add(codeB.toBlocky(context))
    }
    return res
}

data class Statement(val kw: Keywordy, val args: List<String> = emptyList()) {
    override fun toString() = ":${kw.name} $args"
}
/**
 * ##Contract
 * - split source codes line by line.
 * - ignore comment (starting with `#`)
 * - ignore empty line
 */
fun splitByLine(code: String, commentSyl: Char = '#'): ArrayList<String> {
    val res = ArrayList<String>()
    val lines = code.split("\\r?\\n".toRegex())
    for (line in lines) {
        if (line.isBlank()) continue
        val realLine = line.replace('\t', ' ').allAfterWhen { it != ' ' }.allBefore(commentSyl)
        res.add(realLine)
    }
    return res
}
/**
 * ##Contract
 * - every line doesn't start with space.
 * - no empty line
 * @param kwStartSyls ':' or '@'
 * @param blockStartSyl ':'
 * @param blockEndKw "end"
 * @return (blockies,rest line)
 */
fun splitBlocky(
    kwStartSyls: List<Char>,
    blockStartSyl: Char,
    blockEndKw: String,
    lines: List<String>
): Pair<ArrayList<CodeBlocky>, ArrayList<Code>> {
    val blockies = ArrayList<CodeBlocky>()
    var cache = LinkedList<Code>()
    val rest = ArrayList<Code>()
    val nameRegx = "\\S+(?=${blockStartSyl})".toRegex()
    var name = ""
    for (line in lines) {
        if (!line.startsWithChar(kwStartSyls) && name.isEmpty()) {   // doesn't start with ':' or '@'
            val res = nameRegx.find(line)// such as "BlockA:"
            // If it's a block head, add it.
            if (res != null) {
                name = res.value
            }
            // If it's something else, ignore it.
        } else if (line.startsWith(blockEndKw)) {
            // such as "end BlockA"
            blockies.add(CodeBlocky(name, cache))
            name = ""
            cache = LinkedList()
        } else if (name.isNotEmpty()) {
            cache.add(Code(line.trim()))
        } else {
            rest.add(Code(line.trim()))
        }
    }
    return Pair(blockies, rest)
}
/**
 * ##Contract
 * - every code can be meta or a statement
 * - separate them
 */
fun separateMetaFromCode(
    metilaSyl: Char,
    codes: List<Code>
): Pair<ArrayList<Metila>, ArrayList<Code>> {
    val metilas = ArrayList<Metila>()
    val restCodes = ArrayList<Code>()
    for (code in codes) {
        if (code.isMetila(metilaSyl)) {
            val parts = code.code.split(' ', ignoreCase = true)
            metilas.add(Metila(parts[0].substring(1), parts.subList(1, parts.size)))
        } else {
            restCodes.add(code)
        }
    }
    return Pair(metilas, restCodes)
}

class KeywordNotFoundException(msg: String) : AnalysisException(msg)
/**
 * @exception KeywordNotFoundException raises when keyword not found
 */
fun Code.toStatement(context: PlumyLogos): Statement {
    val kwstr = context.kwRegex.find(code)
    if (kwstr != null) {
        val kwIn = kwstr.value
        val kw = context.name2Kw[kwIn] ?: throw KeywordNotFoundException(code)
        val rest = code.substring(1 + kwIn.length)
        val trimmedRest = rest.trim()
        return if (kw.isVarargs) {
            val args = trimmedRest.tryMatchVararg(context, kw.varargsNums)
            if (args == null)
                Statement(kw)
            else
                Statement(kw, args)
        } else {
            when (val argNum = kw.argNum) {
                0 -> Statement(kw)
                1 -> Statement(kw, listOf(trimmedRest))
                else -> Statement(kw, trimmedRest.splitArgs(context, argNum))
            }
        }
    } else throw KeywordNotFoundException(code)
}
/**
 * ## Contract:
 * - **input:** the trimmed string of args.
 * @param possibilities the rules
 * @return the args or null if matches 0 arg.
 */
fun String.tryMatchVararg(context: PlumyLogos, possibilities: List<Int>): List<String>? {
    for (num in possibilities) {
        if (num == 0) {
            if (this.isBlank())
                return null
            else
                continue
        } else if (num == 1) {
            if (isBlank())
                continue
            else
                return listOf(this)
        } else {
            try {
                val args = this.splitArgs(context, num)
                if (args.size == num)
                    return args
            } catch (_: Exception) {
                continue
            }
        }
    }
    return null
}

fun List<Code>.toStatement(context: PlumyLogos): MutableList<Statement> =
    ArrayList(this.map { it.mapCode(context).toStatement(context) })

class UnboundParenthesisException(msg: String) : AnalysisException(msg)
/**
 * ##Contract:
 * - doesn't start/end with space
 * - can only handle with **1 parenthesis**
 */
fun String.splitArgs(context: PlumyLogos, argNum: Int): List<String> {
    if (argNum == 0) return emptyList()
    val args = ArrayList<String>(argNum)
    var parenthesis = false
    val s = StringBuilder()
    for (c in this) {
        if (c == context.leftBracketSyl) {
            if (parenthesis) throw UnboundParenthesisException(this)
            s.append(c)
            parenthesis = true
        } else if (c == context.rightBracketSyl) {
            if (!parenthesis) throw UnboundParenthesisException(this)
            s.append(c)
            parenthesis = false
        } else if (c == ' ') {
            if (!parenthesis) {// unbracketed
                if (s.isNotEmpty()) {// already appended: next arg
                    args.add(s.toString())
                    s.clear()
                }
                // else: ignore space
            } else  // bracketed: append
                s.append(c)
        } else {// not space: append
            s.append(s)
        }
    }
    if (s.isNotEmpty()) {
        args.add(s.toString())
    }
    return args
}

class NotQuotedButSpaceException(msg: String) : AnalysisException(msg)

fun String.toQuotedString(context: PlumyLogos): String {
    val res = context.quotedRegex.find(this)
    return if (res != null) {
        res.value
    } else {
        if (' ' in this) throw NotQuotedButSpaceException(this)
        this
    }
}


