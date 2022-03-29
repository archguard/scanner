// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.archguard.scanner.bytecode

import chapi.domain.core.*
import org.archguard.scanner.bytecode.module.CodeModule
import org.archguard.scanner.bytecode.module.ModuleUtil.getModule
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.stream.Collectors

class ByteCodeParser {
    private val logger = LoggerFactory.getLogger(ByteCodeParser::class.java)
    private var MODIFIERS: MutableMap<Int, String> = mutableMapOf(
        CodeConstants.ACC_PUBLIC to "public",
        CodeConstants.ACC_PROTECTED to "protected",
        CodeConstants.ACC_PRIVATE to "private",
        CodeConstants.ACC_ABSTRACT to "abstract",
        CodeConstants.ACC_STATIC to "static",
        CodeConstants.ACC_FINAL to "final",
        CodeConstants.ACC_STRICT to "strictfp",
        CodeConstants.ACC_TRANSIENT to "transient",
        CodeConstants.ACC_VOLATILE to "volatile",
        CodeConstants.ACC_SYNCHRONIZED to "synchronized",
        CodeConstants.ACC_NATIVE to "native",
    )

    private val CLASS_ALLOWED =
        CodeConstants.ACC_PUBLIC or CodeConstants.ACC_PROTECTED or CodeConstants.ACC_PRIVATE or CodeConstants.ACC_ABSTRACT or
                CodeConstants.ACC_STATIC or CodeConstants.ACC_FINAL or CodeConstants.ACC_STRICT
    private val FIELD_ALLOWED =
        CodeConstants.ACC_PUBLIC or CodeConstants.ACC_PROTECTED or CodeConstants.ACC_PRIVATE or CodeConstants.ACC_STATIC or
                CodeConstants.ACC_FINAL or CodeConstants.ACC_TRANSIENT or CodeConstants.ACC_VOLATILE
    private val METHOD_ALLOWED =
        CodeConstants.ACC_PUBLIC or CodeConstants.ACC_PROTECTED or CodeConstants.ACC_PRIVATE or CodeConstants.ACC_ABSTRACT or
                CodeConstants.ACC_STATIC or CodeConstants.ACC_FINAL or CodeConstants.ACC_SYNCHRONIZED or CodeConstants.ACC_NATIVE or
                CodeConstants.ACC_STRICT

    private val CLASS_EXCLUDED = CodeConstants.ACC_ABSTRACT or CodeConstants.ACC_STATIC
    private val FIELD_EXCLUDED = CodeConstants.ACC_PUBLIC or CodeConstants.ACC_STATIC or CodeConstants.ACC_FINAL
    private val METHOD_EXCLUDED = CodeConstants.ACC_PUBLIC or CodeConstants.ACC_ABSTRACT

    private val ACCESSIBILITY_FLAGS =
        CodeConstants.ACC_PUBLIC or CodeConstants.ACC_PROTECTED or CodeConstants.ACC_PRIVATE

    private val importCollector: ImportCollector = ImportCollector()

    @Throws(Exception::class, IOException::class)
    fun parseClassFile(file: File): CodeDataStruct {
        logger.debug("ByteCodeParser parser: {}", file)
        val classNode = getAsmClasNode(file)
        val module = getModule(file.toPath())
        val ds = createDataStruct(classNode, module)

        ds.Imports = importCollector.packImports().map {
            CodeImport(Source = it.key, AsName = it.value)
        }.toTypedArray()

        return ds
    }

    @Throws(IOException::class)
    private fun getAsmClasNode(file: File): ClassNode {
        val fileInputStream = FileInputStream(file)
        val classNode = ClassNode()

        val cr = ClassReader(fileInputStream)
        cr.accept(classNode, 0)

        fileInputStream.close()

        return classNode
    }

    private fun createModifiers(
        classAccess: Int,
        allowField: Int,
        isInterface: Boolean,
        fieldExcluded: Int
    ): Array<String> {
        var flags = classAccess
        var excluded = fieldExcluded
        flags = flags and allowField
        if (!isInterface) excluded = 0

        var modifiers: Array<String> = arrayOf()
        for (modifier in MODIFIERS.keys) {
            if (flags and modifier == modifier && modifier and excluded == 0) {
                modifiers = modifiers.plus(MODIFIERS[modifier].toString())
            }
        }

        return modifiers
    }

    private fun createDataStruct(node: ClassNode, module: CodeModule): CodeDataStruct {
        val ds = CodeDataStruct()
        val names = importCollector.splitPackageAndClassName(classNameFromType(node.name, false))
        ds.Package = names.first
        ds.NodeName = names.second

        val isInterface = CodeConstants.ACC_INTERFACE == node.access

        ds.Type = if (isInterface) DataStructType.INTERFACE else DataStructType.CLASS

        // todo: add modifiers to Chapi
        createModifiers(node.access, CLASS_ALLOWED, isInterface, CLASS_EXCLUDED)

        node.methods.forEach {
            ds.Functions += this.createFunction(it, node)
        }

        val superName = node.superName
        ds.Extend = classNameFromType(superName, true)

        ds.Implements = node.interfaces?.map {
            val className = classNameFromType(it, true)
            className
        }?.toTypedArray() ?: arrayOf()

        ds.Annotations = node.visibleAnnotations?.map {
            createAnnotation(it)
        }?.toTypedArray() ?: arrayOf()

        ds.Fields = node.fields?.map {
            createField(it)
        }?.toTypedArray() ?: arrayOf()

        return ds
    }

    private fun createField(field: FieldNode): CodeField {
        val isInterface = CodeConstants.ACC_INTERFACE == field.access

        // todo: add field annotation
//        field.Annotations = field.visibleAnnotations?.map {
//            createAnnotation(it)
//        }?.toTypedArray() ?: arrayOf()

        val className = Type.getType(field.desc).className

        importCollector.processClassName(className)

        return CodeField(
            TypeType = className,
            TypeValue = field.name,
            Modifiers = createModifiers(field.access, FIELD_ALLOWED, isInterface, FIELD_EXCLUDED)
        )
    }

    private fun createAnnotation(annotation: AnnotationNode): CodeAnnotation {
        val className: String = Type.getType(annotation.desc).className
        val codeAnnotation = CodeAnnotation(Name = className)

        importCollector.processClassName(className)

        if (annotation.values != null) {
            val values: List<Any> = annotation.values
            var i = 0
            while (i < values.size) {
                val key = values[i].toString()
                val value = values[i + 1].toString()

                codeAnnotation.KeyValues += AnnotationKeyValue(key, value)
                i += 2
            }
        }

        return codeAnnotation
    }

    private fun createFunction(methodNode: MethodNode, node: ClassNode): CodeFunction {
        val codeFunction = CodeFunction(Name = methodNode.name)

        if (methodNode.name == CodeConstants.INIT_NAME) {
            codeFunction.IsConstructor = true
        }

        val isInterface: Boolean = CodeConstants.ACC_INTERFACE == methodNode.access
        codeFunction.Modifiers = createModifiers(methodNode.access, METHOD_ALLOWED, isInterface, METHOD_EXCLUDED)
        codeFunction.ReturnType = Type.getType(methodNode.desc).returnType.className.orEmpty()

        if (methodNode.parameters != null) {
            codeFunction.Parameters = getParamsFromDesc(methodNode.desc, methodNode.parameters)
        }

        codeFunction.Annotations = methodNode.visibleAnnotations?.map {
            createAnnotation(it)
        }?.toTypedArray() ?: arrayOf()

        codeFunction.FunctionCalls = createFunctionCalls(methodNode, node)

        return codeFunction
    }

    private fun createFunctionCalls(methodNode: MethodNode, node: ClassNode): Array<CodeCall> {
        var calls: Array<CodeCall> = arrayOf()
        methodNode.instructions.map {
            when (it) {
                is MethodInsnNode -> {
                    val qualifiedName = refineMethodOwner(it.name, it.owner, node).orEmpty()

                    val names = importCollector.splitPackageAndClassName(qualifiedName)
                    importCollector.processClassName(qualifiedName)

                    calls += CodeCall(
                        Package = names.first,
                        NodeName = names.second,
                        FunctionName = it.name,
                        Parameters = getArgsFromDesc(it.desc),

                        // todo: add return type for code call
                        // Type = Type.getType(it.desc).returnType.className
                    )
                }
                else -> {}
            }
        }

        return calls
    }

    private fun refineMethodOwner(method: String, owner: String, classNode: ClassNode): String? {
        var ownerName: String? = owner
        if (classNode.name.equals(owner, ignoreCase = true)) {
            val methodNameList = classNode.methods.stream().map { v: MethodNode -> v.name }
                .collect(Collectors.toList())
            if (!methodNameList.contains(method)) {
                ownerName = classNode.superName
            }
        }

        return classNameFromType(ownerName!!, true)
    }

    private fun classNameFromType(superName: String, isImport: Boolean): String {
        val className = Type.getObjectType(superName).className
        if (isImport) {
            importCollector.processClassName(className)
        }

        return className
    }

    private fun getParamsFromDesc(desc: String, parameters: MutableList<ParameterNode>): Array<CodeProperty> {
        return Type.getType(desc).argumentTypes.mapIndexed { index, it ->
            importCollector.processClassName(it.className)

            CodeProperty(
                TypeType = it.className,
                TypeValue = parameters[index].name
            )
        }.toTypedArray()
    }

    private fun getArgsFromDesc(desc: String): Array<CodeProperty> {
        return Type.getType(desc).argumentTypes.map {
            importCollector.processClassName(it.className)

            CodeProperty(
                TypeType = it.className,
                TypeValue = it.internalName
            )
        }.toTypedArray()
    }
}
