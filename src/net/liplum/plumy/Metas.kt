package net.liplum.plumy

import opengal.core.NodeTree

abstract class MetaType(
    val name: String
) {
    open fun handle(tree: NodeTree, arg: String) {
        tree.metas[name] = arg
    }

    override fun toString() = name
}

object FileMeta : MetaType("file") {
    override fun handle(tree: NodeTree, arg: String) {
        tree.fileName = arg
    }
}


object InputMeta : MetaType("input") {
    override fun handle(tree: NodeTree, arg: String) {
        tree.inputs.add(arg)
    }
}

