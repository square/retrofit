/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl.extensions

import kotlinx.metadata.KmExtensionType
import kotlinx.metadata.KmExtensionVisitor

internal fun <N : KmExtension<*>> Collection<N>.singleOfType(type: KmExtensionType): N {
    var result: N? = null
    for (node in this) {
        if (node.type != type) continue
        if (result != null) {
            throw IllegalStateException("Multiple extensions handle the same extension type: $type")
        }
        result = node
    }
    if (result == null) {
        throw IllegalStateException("No extensions handle the extension type: $type")
    }
    return result
}
