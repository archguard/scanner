package org.archguard.scanner.analyser.sca.base

import org.archguard.scanner.core.sca.DeclFileTree
import org.archguard.scanner.core.sca.PackageDependencies
import java.io.File

abstract class Finder {
    abstract val parser: Parser

    open fun isMatch(it: File): Boolean {
        return false
    }

    open fun process(path: String): List<PackageDependencies> {
        return File(path).walk(FileWalkDirection.BOTTOM_UP)
            .filter {
                isMatch(it)
            }
            .flatMap {
                val file = DeclFileTree(filename = it.name, path = it.canonicalPath, content = it.readText())
                parser.lookupSource(file)
            }.toList()
    }
}
