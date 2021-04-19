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
    override fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext) {
        val ext = v.visitExtensions(JvmClassExtensionVisitor.TYPE) as? JvmClassExtensionVisitor ?: return

        val anonymousObjectOriginName = proto.getExtensionOrNull(JvmProtoBuf.anonymousObjectOriginName)
        if (anonymousObjectOriginName != null) {
            ext.visitAnonymousObjectOriginName(c[anonymousObjectOriginName])
        }

        for (property in proto.getExtension(JvmProtoBuf.classLocalVariable)) {
            ext.visitLocalDelegatedProperty(
                property.flags, c[property.name], property.getPropertyGetterFlags(), property.getPropertySetterFlags()
            )?.let { property.accept(it, c) }
        }

        ext.visitModuleName(proto.getExtensionOrNull(JvmProtoBuf.classModuleName)?.let(c::get) ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)

        ext.visitEnd()
    }

    override fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext) {
        val ext = v.visitExtensions(JvmPackageExtensionVisitor.TYPE) as? JvmPackageExtensionVisitor ?: return

        for (property in proto.getExtension(JvmProtoBuf.packageLocalVariable)) {
            ext.visitLocalDelegatedProperty(
                property.flags, c[property.name], property.getPropertyGetterFlags(), property.getPropertySetterFlags()
            )?.let { property.accept(it, c) }
        }

        ext.visitModuleName(proto.getExtensionOrNull(JvmProtoBuf.packageModuleName)?.let(c::get) ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME)

        ext.visitEnd()
    }

    // ModuleFragment is not used by JVM backend.
    override fun readModuleFragmentExtensions(v: KmModuleFragmentVisitor, proto: ProtoBuf.PackageFragment, c: ReadContext) {}

    override fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext) {
        val ext = v.visitExtensions(JvmFunctionExtensionVisitor.TYPE) as? JvmFunctionExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmMethodSignature(proto, c.strings, c.types)?.wrapAsPublic())

        val lambdaClassOriginName = proto.getExtensionOrNull(JvmProtoBuf.lambdaClassOriginName)
        if (lambdaClassOriginName != null) {
            ext.visitLambdaClassOriginName(c[lambdaClassOriginName])
        }

        ext.visitEnd()
    }

    override fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext) {
        val ext = v.visitExtensions(JvmPropertyExtensionVisitor.TYPE) as? JvmPropertyExtensionVisitor ?: return
        val fieldSignature = JvmProtoBufUtil.getJvmFieldSignature(proto, c.strings, c.types)
        val propertySignature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature)
        val getterSignature =
            if (propertySignature != null && propertySignature.hasGetter()) propertySignature.getter else null
        val setterSignature =
            if (propertySignature != null && propertySignature.hasSetter()) propertySignature.setter else null
        ext.visit(
            proto.getExtension(JvmProtoBuf.flags),
            fieldSignature?.wrapAsPublic(),
            getterSignature?.run { JvmMethodSignature(c[name], c[desc]) },
            setterSignature?.run { JvmMethodSignature(c[name], c[desc]) }
        )

        val syntheticMethod =
            if (propertySignature != null && propertySignature.hasSyntheticMethod()) propertySignature.syntheticMethod else null
        ext.visitSyntheticMethodForAnnotations(syntheticMethod?.run { JvmMethodSignature(c[name], c[desc]) })

        ext.visitEnd()
    }

    override fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext) {
        val ext = v.visitExtensions(JvmConstructorExtensionVisitor.TYPE) as? JvmConstructorExtensionVisitor ?: return
        ext.visit(JvmProtoBufUtil.getJvmConstructorSignature(proto, c.strings, c.types)?.wrapAsPublic())
    }

    override fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext) {
        val ext = v.visitExtensions(JvmTypeParameterExtensionVisitor.TYPE) as? JvmTypeParameterExtensionVisitor ?: return
        for (annotation in proto.getExtension(JvmProtoBuf.typeParameterAnnotation)) {
            ext.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        ext.visitEnd()
    }

    override fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext) {
        val ext = v.visitExtensions(JvmTypeExtensionVisitor.TYPE) as? JvmTypeExtensionVisitor ?: return
        ext.visit(proto.getExtension(JvmProtoBuf.isRaw))
        for (annotation in proto.getExtension(JvmProtoBuf.typeAnnotation)) {
            ext.visitAnnotation(annotation.readAnnotation(c.strings))
        }
        ext.visitEnd()
    }

    override fun readTypeAliasExtensions(v: KmTypeAliasVisitor, proto: ProtoBuf.TypeAlias, c: ReadContext) {}

    override fun readValueParameterExtensions(v: KmValueParameterVisitor, proto: ProtoBuf.ValueParameter, c: ReadContext) {}

    override fun createFunctionExtension(): KmFunctionExtension = JvmFunctionExtension()

    override fun createTypeExtension(): KmTypeExtension = JvmTypeExtension()

    override fun createValueParameterExtension(): KmValueParameterExtension = JvmValueParameterExtension()
}
