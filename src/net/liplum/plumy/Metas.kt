package net.liplum.plumy

import opengal.core.NodeTree

abstract class MetaType(
    val name: String
) {
    open fun handle(tree: NodeTree, arg: String) {
        tree.metas.add(name, arg)
    }

    override fun toString() = name
}

fun <TK, TV> MutableMap<TK, MutableSet<TV>>.add(key: TK, value: TV) {
    val values = this[key]
    if (values == null) {
        val set = HashSet<TV>()
        this[key] = set
        set.add(value)
    } else {
        values.add(value)
    }
}

object FileMeta : MetaType("file") {
    override fun handle(tree: NodeTree, arg: String) {
        tree.file = arg
    }
}

object InputMeta : MetaType("input") {
    override fun handle(tree: NodeTree, arg: String) {
        tree.inputs.add(arg)
    }
}
