/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package retrofit2.kotlinx.metadata.impl.extensions

import retrofit2.kotlinx.metadata.KmExtensionVisitor
import retrofit2.kotlinx.metadata.KmFunctionExtensionVisitor

interface KmExtension<V : KmExtensionVisitor> : KmExtensionVisitor {
    fun accept(visitor: V)
}

interface KmFunctionExtension : KmFunctionExtensionVisitor, KmExtension<KmFunctionExtensionVisitor>