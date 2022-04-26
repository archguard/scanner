package org.archguard.doc.generator.render

abstract class Render<T> {
    abstract fun T.buildLink(address: String, content: T.() -> Unit)
    abstract fun T.buildLineBreak()
}

interface ContentNode

data class DocPage(val content: List<ContentNode>) : ContentNode
data class DocHeader(val title: String, val content: List<ContentNode>, val level: Int) : ContentNode
data class DocText(val text: String) : ContentNode
data class DocBreakLine(val text: String) : ContentNode
data class DocCodeBlock(val language: String, val text: String) : ContentNode