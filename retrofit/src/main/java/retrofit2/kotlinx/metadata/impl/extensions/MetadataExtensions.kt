/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package retrofit2.kotlinx.metadata.impl.extensions

import retrofit2.kotlinx.metadata.impl.ReadContext
import retrofit2.kotlinx.metadata.jvm.impl.JvmMetadataExtensions
import retrofit2.kotlin.metadata.ProtoBuf
import retrofit2.kotlinx.metadata.KmFunctionVisitor

interface MetadataExtensions {

    fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext)

    fun createFunctionExtension(): KmFunctionExtension

    companion object {
        val INSTANCES: List<MetadataExtensions> by lazy {
            listOf(JvmMetadataExtensions())
        }
    }
}
