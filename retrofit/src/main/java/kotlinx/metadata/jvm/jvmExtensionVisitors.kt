/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*

/**
 * A visitor to visit JVM extensions for a function.
 */
open class JvmFunctionExtensionVisitor @JvmOverloads constructor(
    private val delegate: JvmFunctionExtensionVisitor? = null
) : KmFunctionExtensionVisitor {
    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits the JVM signature of the function, or null if the JVM signature of this function is unknown.
     *
     * Example: `JvmMethodSignature("equals", "(Ljava/lang/Object;)Z")`
     *
     * @param signature the signature of the function
     */
    open fun visit(signature: JvmMethodSignature?) {
        delegate?.visit(signature)
    }

    /**
     * Visits the end of JVM extensions for the function.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmFunctionExtensionVisitor::class)
    }
}
