/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl.extensions

import kotlinx.metadata.*
import kotlinx.metadata.impl.ReadContext
import org.jetbrains.kotlin.metadata.ProtoBuf
import java.util.*

interface MetadataExtensions {
    fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext)

    fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext)

    fun readModuleFragmentExtensions(v: KmModuleFragmentVisitor, proto: ProtoBuf.PackageFragment, c: ReadContext)

    fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext)

    fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext)

    fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext)

    fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext)

    fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext)

    fun readTypeAliasExtensions(v: KmTypeAliasVisitor, proto: ProtoBuf.TypeAlias, c: ReadContext)

    fun readValueParameterExtensions(v: KmValueParameterVisitor, proto: ProtoBuf.ValueParameter, c: ReadContext)

    fun createFunctionExtension(): KmFunctionExtension

    fun createTypeExtension(): KmTypeExtension

    fun createValueParameterExtension(): KmValueParameterExtension

    companion object {
        val INSTANCES: List<MetadataExtensions> by lazy {
            ServiceLoader.load(MetadataExtensions::class.java, MetadataExtensions::class.java.classLoader).toList().also {
                if (it.isEmpty()) error(
                    "No MetadataExtensions instances found in the classpath. Please ensure that the META-INF/services/ " +
                            "is not stripped from your application and that the Java virtual machine is not running " +
                            "under a security manager"
                )
            }
        }
    }
}
