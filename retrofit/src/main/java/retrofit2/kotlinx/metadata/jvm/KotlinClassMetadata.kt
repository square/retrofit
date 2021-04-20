/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package retrofit2.kotlinx.metadata.jvm

import retrofit2.kotlinx.*
import retrofit2.kotlinx.metadata.impl.accept
import retrofit2.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import retrofit2.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import retrofit2.kotlinx.metadata.InconsistentKotlinMetadataException
import retrofit2.kotlinx.metadata.KmClass
import retrofit2.kotlinx.metadata.KmClassVisitor
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Represents the parsed metadata of a Kotlin JVM class file.
 *
 * To create an instance of [KotlinClassMetadata], first obtain a [KotlinClassHeader] instance by loading the contents
 * of the [Metadata] annotation on a class file, and then call [KotlinClassMetadata.read].
 */
sealed class KotlinClassMetadata(val header: KotlinClassHeader) {
    /**
     * Represents metadata of a class file containing a declaration of a Kotlin class.
     */
    class Class internal constructor(header: KotlinClassHeader) : KotlinClassMetadata(header) {
        private val classData by lazy(PUBLICATION) {
            val data1 = (header.data1.takeIf(Array<*>::isNotEmpty)
                ?: throw InconsistentKotlinMetadataException("data1 must not be empty"))
            JvmProtoBufUtil.readClassDataFrom(data1, header.data2)
        }

        /**
         * Visits metadata of this class with a new [KmClass] instance and returns that instance.
         */
        fun toKmClass(): KmClass =
            KmClass().apply(this::accept)

        /**
         * Makes the given visitor visit metadata of this class.
         *
         * @param v the visitor that must visit this class
         */
        fun accept(v: KmClassVisitor) {
            val (strings, proto) = classData
            proto.accept(v, strings)
        }
    }

    companion object {
        /**
         * Reads and parses the given header of a Kotlin JVM class file and returns the correct type of [KotlinClassMetadata] encoded by
         * this header, or `null` if this header encodes an unsupported kind of Kotlin classes or has an unsupported metadata version.
         *
         * Throws [InconsistentKotlinMetadataException] if the metadata has inconsistencies which signal that it may have been
         * modified by a separate tool.
         *
         * @param header the header of a Kotlin JVM class file to be parsed
         */
        @JvmStatic
        fun read(header: KotlinClassHeader): KotlinClassMetadata? {
            if (!JvmMetadataVersion(
                    header.metadataVersion,
                    (header.extraInt and (1 shl 3)/* see JvmAnnotationNames.METADATA_STRICT_VERSION_SEMANTICS_FLAG */) != 0
                ).isCompatible()
            ) return null

            return try {
                when (header.kind) {
                    KotlinClassHeader.CLASS_KIND -> Class(header)
                    else -> throw IllegalArgumentException("The metadata passed are not of a class")
                }
            } catch (e: InconsistentKotlinMetadataException) {
                throw e
            } catch (e: Throwable) {
                throw InconsistentKotlinMetadataException("Exception occurred when reading Kotlin metadata", e)
            }
        }
    }
}
