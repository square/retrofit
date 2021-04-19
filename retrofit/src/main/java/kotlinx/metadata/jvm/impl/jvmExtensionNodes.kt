/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.*
import kotlinx.metadata.jvm.*

internal val KmFunction.jvm: JvmFunctionExtension
    get() = visitExtensions(JvmFunctionExtensionVisitor.TYPE) as JvmFunctionExtension

internal class JvmFunctionExtension : JvmFunctionExtensionVisitor(), KmFunctionExtension {
    var signature: JvmMethodSignature? = null
    var lambdaClassOriginName: String? = null

    override fun visit(signature: JvmMethodSignature?) {
        this.signature = signature
    }

    override fun visitLambdaClassOriginName(internalName: String) {
        this.lambdaClassOriginName = internalName
    }

    override fun accept(visitor: KmFunctionExtensionVisitor) {
        require(visitor is JvmFunctionExtensionVisitor)
        visitor.visit(signature)
        lambdaClassOriginName?.let(visitor::visitLambdaClassOriginName)
        visitor.visitEnd()
    }
}

internal class JvmTypeExtension : JvmTypeExtensionVisitor(), KmTypeExtension {
    var isRaw: Boolean = false
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun visit(isRaw: Boolean) {
        this.isRaw = isRaw
    }

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations.add(annotation)
    }

    override fun accept(visitor: KmTypeExtensionVisitor) {
        require(visitor is JvmTypeExtensionVisitor)
        visitor.visit(isRaw)
        annotations.forEach(visitor::visitAnnotation)
        visitor.visitEnd()
    }
}

internal class JvmValueParameterExtension : JvmValueParameterExtensionVisitor(), KmValueParameterExtension {
    override fun accept(visitor: KmValueParameterExtensionVisitor) {
        require(visitor is JvmValueParameterExtensionVisitor)
        visitor.visitEnd()
    }
}
