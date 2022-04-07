package net.liplum.plumy

fun <T> ArrayList<T>.addMany(vararg elements: T) {
    this.addAll(elements)
}
/**
 * @return whether this string starts with any one in [heads]
 */
fun String.startsWith(heads: List<String>): Boolean {
    for (head in heads) {
        if (this.startsWith(head))
            return true
    }
    return false
}
/**
 * @return whether this string starts with any one in [heads]
 */
fun String.startsWithChar(heads: List<Char>): Boolean {
    for (head in heads) {
        if (this.startsWith(head))
            return true
    }
    return false
}

fun String.allBefore(char: Char): String {
    val b = StringBuilder()
    for (c in this) {
        if (c == char) break
        b.append(c)
    }
    return b.toString()
}

inline fun String.allUntil(whenTrue: (Char) -> Boolean): String {
    val b = StringBuilder()
    for (i in this.indices) {
        val c = this[i]
        if (whenTrue(c)) {
            b.append(this.substring(i))
            break
        }
    }
    return b.toString()
}

fun List<*>.printline() {
    for (s in this)
        println(s)
}

fun <K, V> Map<K, V>.reversed(): Map<V, K> {
    val hashMap = HashMap<V, K>()
    for ((k, v) in this) {
        hashMap[v] = k
    }
    return hashMap
}

fun List<String>.trim(): List<String> =
    this.map { it.trim() }

data class WrapBool(var bool: Boolean) {
    override fun toString() = bool.toString()
}

fun String.toStrList(): List<String> {
    return this.map { it.toString() }
}