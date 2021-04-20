/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package retrofit2.kotlinx.metadata.jvm.impl

import retrofit2.kotlin.metadata.ProtoBuf
import retrofit2.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import retrofit2.kotlinx.metadata.KmFunctionVisitor
import retrofit2.kotlinx.metadata.impl.ReadContext
import retrofit2.kotlinx.metadata.impl.extensions.KmFunctionExtension
import retrofit2.kotlinx.metadata.impl.extensions.MetadataExtensions
import retrofit2.kotlinx.metadata.jvm.JvmFunctionExtensionVisitor
import retrofit2.kotlinx.metadata.jvm.wrapAsPublic

internal class JvmMetadataExtensions : MetadataExtensions {

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext) {
        val ext = v.visitExtensions(JvmFunctionExtensionVisitor.TYPE) as? JvmFunctionExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmMethodSignature(proto, c.strings, c.types)?.wrapAsPublic())

        ext.visitEnd()
    }

    override fun createFunctionExtension(): KmFunctionExtension = JvmFunctionExtension()
}
