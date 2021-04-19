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

    override fun visit(signature: JvmMethodSignature?) {
        this.signature = signature
    }


    override fun accept(visitor: KmFunctionExtensionVisitor) {
        require(visitor is JvmFunctionExtensionVisitor)
        visitor.visit(signature)
        visitor.visitEnd()
    }
}

