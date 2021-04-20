/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package retrofit2.kotlinx.metadata.impl

import retrofit2.kotlinx.metadata.ClassName
import retrofit2.kotlin.metadata.deserialization.NameResolver

internal fun NameResolver.getClassName(index: Int): ClassName {
    val name = getQualifiedClassName(index)
    return if (isLocalClassName(index)) ".$name" else name
}