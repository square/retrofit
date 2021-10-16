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

internal class MetadataParser(private val reader: ProtobufReader, private val strings: Array<String>) {

    fun parse(): List<KotlinMetadata.Function> {
        val table = StringTableTypes.parse(makeDelimited(reader, true))
        val nameResolver = JvmNameResolver(table, strings)

        val klass = Klass.parse(reader)

        val functions = mutableListOf<KotlinMetadata.Function>()

        klass.functions.forEach { f ->
            val functionName = f.getName(nameResolver)
            val signatureDesc = f.signature.getDesc(nameResolver)

            val returnType = KotlinMetadata.ReturnType(f.returnType.isNullable, f.returnType.isUnit(nameResolver))

            functions += KotlinMetadata.Function(functionName + signatureDesc, returnType)
        }

        return functions
    }
}