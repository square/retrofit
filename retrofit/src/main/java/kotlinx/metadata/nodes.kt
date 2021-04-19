/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package kotlinx.metadata

import kotlinx.metadata.impl.extensions.*
import java.lang.IllegalStateException

/**
 * Represents a Kotlin declaration container, such as a class or a package fragment.
 */
interface KmDeclarationContainer {
    /**
     * Functions in the container.
     */
    val functions: MutableList<KmFunction>
}

/**
 * Represents a Kotlin class.
 */
class KmClass : KmClassVisitor(), KmDeclarationContainer {

    /**
     * Functions in the class.
     */
    override val functions: MutableList<KmFunction> = ArrayList()

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor =
        KmFunction(flags, name).addTo(functions)

    /**
     * Populates the given visitor with data in this class.
     *
     * @param visitor the visitor which will visit data in this class
     */
    fun accept(visitor: KmClassVisitor) {
        functions.forEach { visitor.visitFunction(it.flags, it.name)?.let(it::accept) }
    }
}

/**
 * Represents a Kotlin function declaration.
 *
 * @property flags function flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Function] flags
 * @property name the name of the function
 */
class KmFunction(
    var flags: Flags,
    var name: String
) : KmFunctionVisitor() {

    /**
     * Return type of the function.
     */
    lateinit var returnType: KmType

    private val extensions: List<KmFunctionExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createFunctionExtension)

    override fun visitReturnType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { returnType = it }

    override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this function.
     *
     * @param visitor the visitor which will visit data in this function
     */
    fun accept(visitor: KmFunctionVisitor) {
        visitor.visitReturnType(returnType.flags)?.let(returnType::accept)
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
    }
}

/**
 * Represents a type.
 *
 * @property flags type flags, consisting of [Flag.Type] flags
 */
class KmType(var flags: Flags) : KmTypeVisitor() {
    /**
     * Classifier of the type.
     */
    lateinit var classifier: KmClassifier

    override fun visitClass(name: ClassName) {
        classifier = KmClassifier.Class(name)
    }

    /**
     * Populates the given visitor with data in this type.
     *
     * @param visitor the visitor which will visit data in this type
     */
    fun accept(visitor: KmTypeVisitor) {
        when (val classifier = classifier) {
            is KmClassifier.Class -> visitor.visitClass(classifier.name)
            else -> throw IllegalStateException("Invalid classifier type: ${classifier::class}")
        }
    }
}

/**
 * Represents a classifier of a Kotlin type. A classifier is a class, type parameter or type alias.
 * For example, in `MutableMap<in String?, *>`, `MutableMap` is the classifier.
 */
sealed class KmClassifier {
    /**
     * Represents a class used as a classifier in a type.
     *
     * @property name the name of the class
     */
    data class Class(val name: ClassName) : KmClassifier()
}

internal fun <T> T.addTo(collection: MutableCollection<T>): T {
    collection.add(this)
    return this
}
