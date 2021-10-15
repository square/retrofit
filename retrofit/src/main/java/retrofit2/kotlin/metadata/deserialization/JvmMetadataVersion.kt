/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.kotlin.metadata.deserialization

/**
 * This file was adapted from https://github.com/JetBrains/kotlin/blob/af18b10da9d1e20b1b35831a3fb5e508048a2576/core/metadata.jvm/src/org/jetbrains/kotlin/metadata/jvm/deserialization/JvmMetadataVersion.kt
 * by removing the unused parts.
 */

/**
 * The version of the metadata serialized by the compiler and deserialized by the compiler and reflection.
 * This version includes the version of the core protobuf messages (metadata.proto) as well as JVM extensions (jvm_metadata.proto).
 */
class JvmMetadataVersion(versionArray: IntArray, val isStrictSemantics: Boolean) : BinaryVersion(*versionArray) {
    constructor(vararg numbers: Int) : this(numbers, isStrictSemantics = false)

    override fun isCompatible(): Boolean =
        // NOTE: 1.0 is a pre-Kotlin-1.0 metadata version, with which the current compiler is incompatible
        (major != 1 || minor != 0) &&
                if (isStrictSemantics) {
                    isCompatibleTo(INSTANCE)
                } else {
                    // Kotlin 1.N is able to read metadata of versions up to Kotlin 1.{N+1} (unless the version has strict semantics).
                    major == INSTANCE.major && minor <= INSTANCE.minor + 1
                }

    companion object {
        @JvmField
        val INSTANCE = JvmMetadataVersion(1, 6, 0)
    }
}
