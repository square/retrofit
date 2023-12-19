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
package retrofit2

import retrofit2.kotlin.metadata.deserialization.BitEncoding
import retrofit2.kotlin.metadata.deserialization.ByteArrayInput
import retrofit2.kotlin.metadata.deserialization.JvmMetadataVersion
import retrofit2.kotlin.metadata.deserialization.MetadataParser
import retrofit2.kotlin.metadata.deserialization.ProtobufReader
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation

object KotlinMetadata {

    data class Function(val signature: String, val returnType: ReturnType)
    data class ReturnType(val isNullable: Boolean, val isUnit: Boolean)

    private val kotlinFunctionsMap = ConcurrentHashMap<Class<*>, List<Function>>()

    /**
     * This helps to parse kotlin metadata of a compiled class to find out the nullability of a suspending method return
     * type.
     *
     * For example a suspending method with following declaration:
     *
     * ```
     *     @GET("/") suspend fun foo(@Query("x") arg: IntArray): String?
     * ```
     *
     * Will be compiled as a method returning [Object] and with injected [Continuation] argument with following java
     * method:
     *
     * ```
     *     public Object foo(int[], Continuation)
     * ```
     *
     * The information about the return type and its nullability is stored in a [Metadata] annotation of the containing
     * class. We process the metadata of a class the first time [isReturnTypeNullable] is called on one of its methods.
     * We extract necessary information about all of its methods and store this info in cache, so each class is
     * processed only once. Then we try to match the currently inspected [Method] to one extracted from metadata by
     * comparing their signatures.
     *
     * We use the method signature because:
     *   - it uniquely identifies a method
     *   - it requires just comparing 2 strings
     *   - it is already stored in kotlin metadata
     *   - it is trivial to create one from java reflection's [Method] instance
     *
     * For example the previous method's signature would be:
     *
     * ```
     *     foo([ILkotlin/coroutines/Continuation;)Ljava/lang/Object;
     * ```
     */
    @JvmStatic fun isReturnTypeNullable(method: Method): Boolean {
        if (method.declaringClass.getAnnotation(Metadata::class.java) == null) return false

        val javaMethodSignature = method.createSignature()
        val kotlinFunctions = loadKotlinFunctions(method.declaringClass)
        val candidates = kotlinFunctions.filter { it.signature == javaMethodSignature }

        require(candidates.isNotEmpty()) { "No match found in metadata for '${method}'" }
        require(candidates.size == 1) { "Multiple function matches found in metadata for '${method}'" }
        val match = candidates.first()

        return match.returnType.isNullable || match.returnType.isUnit
    }

    private fun Method.createSignature() = buildString {
        append(name)
        append('(')

        parameterTypes.forEach {
            append(it.typeToSignature())
        }

        append(')')

        append(returnType.typeToSignature())
    }

    private fun loadKotlinFunctions(clazz: Class<*>): List<Function> {
        var result = kotlinFunctionsMap[clazz]
        if (result != null) return result

        synchronized(kotlinFunctionsMap) {
            result = kotlinFunctionsMap[clazz]
            if (result == null) {
                result = readFunctionsFromMetadata(clazz)
            }
        }

        return result!!
    }

    private fun readFunctionsFromMetadata(clazz: Class<*>): List<Function> {
        val metadataAnnotation = clazz.getAnnotation(Metadata::class.java)

        val isStrictSemantics = (metadataAnnotation.extraInt and (1 shl 3)) != 0
        val isCompatible = JvmMetadataVersion(metadataAnnotation.metadataVersion, isStrictSemantics).isCompatible()

        require(isCompatible) { "Metadata version not compatible" }
        require(metadataAnnotation.kind == 1) { "Metadata of wrong kind: ${metadataAnnotation.kind}" }
        require(metadataAnnotation.data1.isNotEmpty()) { "data1 must not be empty" }

        val bytes: ByteArray = BitEncoding.decodeBytes(metadataAnnotation.data1)
        val reader = ProtobufReader(ByteArrayInput(bytes))
        val parser = MetadataParser(reader, metadataAnnotation.data2)

        return parser.parse()
    }

    private fun Class<*>.typeToSignature() = when {
        isPrimitive -> javaTypesMap[name]
        isArray -> name.replace('.', '/')
        else -> "L${name.replace('.', '/')};"
    }

    private val javaTypesMap = mapOf(
        "int" to "I",
        "long" to "J",
        "boolean" to "Z",
        "byte" to "B",
        "char" to "C",
        "float" to "F",
        "double" to "D",
        "short" to "S",
        "void" to "V"
    )
}
