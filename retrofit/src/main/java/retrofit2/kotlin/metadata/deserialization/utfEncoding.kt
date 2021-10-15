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
 * This file was adapted from https://github.com/JetBrains/kotlin/blob/af18b10da9d1e20b1b35831a3fb5e508048a2576/core/metadata.jvm/src/org/jetbrains/kotlin/metadata/jvm/deserialization/utfEncoding.kt
 * by removing the unused parts.
 */

// The maximum possible length of the byte array in the CONSTANT_Utf8_info structure in the bytecode, as per JVMS7 4.4.7
const val MAX_UTF8_INFO_LENGTH = 65535

const val UTF8_MODE_MARKER = 0.toChar()

fun stringsToBytes(strings: Array<String>): ByteArray {
    val resultLength = strings.sumBy { it.length }
    val result = ByteArray(resultLength)

    var i = 0
    for (s in strings) {
        for (si in 0..s.length - 1) {
            result[i++] = s[si].toInt().toByte()
        }
    }

    assert(i == result.size) { "Should have reached the end" }

    return result
}
