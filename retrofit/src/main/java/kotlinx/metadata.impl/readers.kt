/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl

import kotlinx.metadata.*
import kotlinx.metadata.Flags // Don't remove this import. See KT-45553
import kotlinx.metadata.impl.extensions.MetadataExtensions
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.deserialization.Flags as F

class ReadContext(
    val strings: NameResolver,
    val types: TypeTable
) {
    internal val extensions = MetadataExtensions.INSTANCES

    operator fun get(index: Int): String =
        strings.getString(index)

    fun className(index: Int): ClassName =
        strings.getClassName(index)
}

fun ProtoBuf.Class.accept(
    v: KmClassVisitor,
    strings: NameResolver
) {
    val c = ReadContext(
        strings,
        TypeTable(typeTable)
    )

    for (function in functionList) {
        v.visitFunction(function.flags, c[function.name])?.let { function.accept(it, c) }
    }
}

private fun ProtoBuf.Function.accept(v: KmFunctionVisitor, outer: ReadContext) {
    returnType(outer.types).let { returnType ->
        v.visitReturnType(returnType.typeFlags)?.let { returnType.accept(it, outer) }
    }

    for (extension in outer.extensions) {
        extension.readFunctionExtensions(v, this, outer)
    }
}

private fun ProtoBuf.Type.accept(v: KmTypeVisitor, c: ReadContext) {
    when {
        hasClassName() -> v.visitClass(c.className(className))
        else -> {
            throw InconsistentKotlinMetadataException("No classifier (class, type alias or type parameter) recorded for Type")
        }
    }
}

private val ProtoBuf.Type.typeFlags: Flags
    get() = (if (nullable) 1 shl 0 else 0) +
            (flags shl 1)
