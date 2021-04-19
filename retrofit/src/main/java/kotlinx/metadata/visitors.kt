/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

/**
 * A visitor to visit Kotlin classes, including interfaces, objects, enum classes and annotation classes.
 *
 * When using this class, [visit] must be called first, followed by zero or more [visitTypeParameter] calls, followed by zero or more calls
 * to other visit* methods, followed by [visitEnd].
 */
abstract class KmClassVisitor @JvmOverloads constructor(private val delegate: KmClassVisitor? = null) {

    /**
     * Visits a function in the container.
     *
     * @param flags function flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Function] flags
     * @param name the name of the function
     */
    open fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        delegate?.visitFunction(flags, name)
}

/**
 * A visitor to visit a Kotlin function declaration.
 *
 * When using this class, zero or more calls to [visitTypeParameter] must be done first, followed by zero or more calls
 * to other visit* methods, followed by [visitEnd].
 */
abstract class KmFunctionVisitor @JvmOverloads constructor(private val delegate: KmFunctionVisitor? = null) {

    /**
     * Visits the return type of the function.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitReturnType(flags: Flags): KmTypeVisitor? =
        delegate?.visitReturnType(flags)

    /**
     * Visits the extensions of the given type on the function.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? =
        delegate?.visitExtensions(type)
}

/**
 * A visitor to visit a type. The type must have a classifier which is one of: a class [visitClass], type parameter [visitTypeParameter]
 * or type alias [visitTypeAlias]. If the type's classifier is a class or a type alias, it can have type arguments ([visitArgument] and
 * [visitStarProjection]). If the type's classifier is an inner class, it can have the outer type ([visitOuterType]), which captures
 * the generic type arguments of the outer class. Also, each type can have an abbreviation ([visitAbbreviatedType]) in case a type alias
 * was used originally at this site in the declaration (all types are expanded by default for metadata produced by the Kotlin compiler).
 * If [visitFlexibleTypeUpperBound] is called, this type is regarded as a flexible type, and its contents represent the lower bound,
 * and the result of the call represents the upper bound.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
abstract class KmTypeVisitor @JvmOverloads constructor(private val delegate: KmTypeVisitor? = null) {
    /**
     * Visits the name of the class, if this type's classifier is a class.
     *
     * @param name the name of the class
     */
    open fun visitClass(name: ClassName) {
        delegate?.visitClass(name)
    }
}
