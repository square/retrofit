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

import retrofit2.KotlinMetadata
import java.io.ByteArrayInputStream

class MetadataParser(val input: ByteArrayInputStream, val nameResolver: JvmNameResolver) {

    fun parseClass(): List<KotlinMetadata.Function> {
        val functions = mutableListOf<KotlinMetadata.Function>()
        while (true) {
            val tag = input.readTag()
            if (tag == 0) break

            val field = tag ushr 3
            val wire = tag and 7
            when (field) {
                9 -> { functions += parseFunction() }
                else -> { input.skipProto(wire) }
            }
        }

        return functions
    }

    private fun parseFunction(): KotlinMetadata.Function {
        lateinit var returnType: KotlinMetadata.ReturnType
        lateinit var name: String
        lateinit var signature: Signature

        input.readMessage { field, wire ->
            when (field) {
                2 -> {
                    val nameIndex = input.readRawVarint32()
                    name = nameResolver.getString(nameIndex)
                }
                3 -> {
                    returnType = parseReturnType()
                }
                100 -> {
                    signature = parseSignature()
                }
                else -> { input.skipProto(wire) }
            }
        }

        val actualName = if (signature.nameIndex != -1) nameResolver.getString(signature.nameIndex) else name
        return KotlinMetadata.Function(actualName + signature.desc, returnType)
    }

    private fun parseReturnType(): KotlinMetadata.ReturnType {
        var nullable = false
        lateinit var name: String

        input.readMessage { field, wire ->
            when (field) {
                3 -> {
                    nullable = input.readRawVarint32() != 0
                }
                6 -> {
                    val nameIndex = input.readRawVarint32()
                    name = nameResolver.getString(nameIndex)
                }
                else -> input.skipProto(wire)
            }
        }

        return KotlinMetadata.ReturnType(nullable, name == "kotlin/Unit")
    }

    class Signature(val nameIndex: Int, val desc: String)

    private fun parseSignature(): Signature {
        var nameIndex = -1
        var desc = ""
        input.readMessage { field, wireType ->
            when (field) {
                1 -> {
                    nameIndex = input.readRawVarint32()
                }
                2 -> {
                    desc = nameResolver.getString(input.readRawVarint32())
                }
                else -> input.skipProto(wireType)
            }
        }

        return Signature(nameIndex, desc)
    }
}