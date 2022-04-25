package org.archguard.doc.generator

import org.archguard.doc.generator.compiler.KotlinAnalysis
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

internal class KotlinAnalysisTest {
    @Test
    internal fun compiler_file() {
        val scriptFile = "src/"
        val parser = KotlinAnalysis()

        val analyzeContext = parser.parse(File(scriptFile))

        val clazz = analyzeContext.allClasses.filter {
            it.name.asString() == "KotlinAnalysis"
        }

        assertEquals(1, clazz.size)
    }

    @Test
    @Disabled
    internal fun for_rules() {
        val scriptFile = "../"
        val parser = KotlinAnalysis()
        val files = File(scriptFile)
            .walk(FileWalkDirection.BOTTOM_UP).filter {
                it.name.startsWith("rule_") && it.isDirectory
            }.map {
                File(it.path + File.separator + "src")
            }.toList().toTypedArray()

        val analyzeContext = parser.parse(*files)

        val clazz = analyzeContext.allClasses.filter {
            it.name.asString() == "KotlinAnalysis"
        }

        assertEquals(1, clazz.size)
    }
}