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
 * This file was adapted from https://github.com/JetBrains/kotlin/blob/26673d2b08f01dec1a9007b9b75436a50fa497e9/core/metadata.jvm/src/org/jetbrains/kotlin/metadata/jvm/deserialization/JvmNameResolver.kt
 * by removing the unused parts.
 */

internal class JvmNameResolver(private val types: StringTableTypes, private val strings: Array<String>) {

    fun getString(index: Int): String {
        val record = types.records[index]

        var string = when {
            record.hasString() -> record.string
            record.hasPredefinedIndex() && record.predefinedIndex in PREDEFINED_STRINGS.indices ->
                PREDEFINED_STRINGS[record.predefinedIndex]
            else -> strings[index]
        }
        requireNotNull(string)

        if (record.substringIndexList.size >= 2) {
            val (begin, end) = record.substringIndexList
            if (begin in 0..end && end <= string.length) {
                string = string.substring(begin, end)
            }
        }

        if (record.replaceCharList.size >= 2) {
            val (from, to) = record.replaceCharList
            string = string.replace(from.toChar(), to.toChar())
        }

        when (record.operation) {
            Record.OPERATION_NONE -> {
                // Do nothing
            }
            Record.OPERATION_INTERNAL_TO_CLASS_ID -> {
                string = string.replace('$', '.')
            }
            Record.OPERATION_DESC_TO_CLASS_ID -> {
                if (string.length >= 2) {
                    string = string.substring(1, string.length - 1)
                }
                string = string.replace('$', '.')
            }
        }

        return string
    }

    companion object {
        private val PREDEFINED_STRINGS = listOf(
            "kotlin/Any",
            "kotlin/Nothing",
            "kotlin/Unit",
            "kotlin/Throwable",
            "kotlin/Number",

            "kotlin/Byte", "kotlin/Double", "kotlin/Float", "kotlin/Int",
            "kotlin/Long", "kotlin/Short", "kotlin/Boolean", "kotlin/Char",

            "kotlin/CharSequence",
            "kotlin/String",
            "kotlin/Comparable",
            "kotlin/Enum",

            "kotlin/Array",
            "kotlin/ByteArray", "kotlin/DoubleArray", "kotlin/FloatArray", "kotlin/IntArray",
            "kotlin/LongArray", "kotlin/ShortArray", "kotlin/BooleanArray", "kotlin/CharArray",

            "kotlin/Cloneable",
            "kotlin/Annotation",

            "kotlin/collections/Iterable", "kotlin/collections/MutableIterable",
            "kotlin/collections/Collection", "kotlin/collections/MutableCollection",
            "kotlin/collections/List", "kotlin/collections/MutableList",
            "kotlin/collections/Set", "kotlin/collections/MutableSet",
            "kotlin/collections/Map", "kotlin/collections/MutableMap",
            "kotlin/collections/Map.Entry", "kotlin/collections/MutableMap.MutableEntry",

            "kotlin/collections/Iterator", "kotlin/collections/MutableIterator",
            "kotlin/collections/ListIterator", "kotlin/collections/MutableListIterator"
        )
    }
}