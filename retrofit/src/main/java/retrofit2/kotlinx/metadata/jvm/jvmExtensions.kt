/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package retrofit2.kotlinx.metadata.jvm

import retrofit2.kotlinx.metadata.jvm.impl.jvm
import retrofit2.kotlinx.metadata.KmFunction

/**
 * JVM signature of the function, or null if the JVM signature of this function is unknown.
 *
 * Example: `JvmMethodSignature("equals", "(Ljava/lang/Object;)Z")`.
 */
var KmFunction.signature: JvmMethodSignature?
    get() = jvm.signature
    set(value) {
        jvm.signature = value
    }