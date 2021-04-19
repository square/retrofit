/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.*
import kotlinx.metadata.impl.extensions.*
import kotlinx.metadata.jvm.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

internal class JvmMetadataExtensions : MetadataExtensions {

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext) {
        val ext = v.visitExtensions(JvmFunctionExtensionVisitor.TYPE) as? JvmFunctionExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmMethodSignature(proto, c.strings, c.types)?.wrapAsPublic())

        ext.visitEnd()
    }

    override fun createFunctionExtension(): KmFunctionExtension = JvmFunctionExtension()
}
