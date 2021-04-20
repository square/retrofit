/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package retrofit2.kotlinx.metadata

import retrofit2.kotlin.metadata.ProtoBuf.*
import retrofit2.kotlin.metadata.deserialization.Flags as F

/**
 * Represents a boolean flag that is either present or not in a Kotlin declaration. A "flag" is a boolean trait that is either present
 * or not in a declaration. To check whether the flag is present in the bitmask, call [Flag.invoke] on the flag, passing the bitmask
 * as the argument:
 *
 *     override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
 *         if (Flag.Function.IS_INLINE(flags)) {
 *             ...
 *         }
 *     }
 *
 * To construct a bitmask out of several flags, call [flagsOf] on the needed flags:
 *
 *     v.visitFunction(flagsOf(Flag.Function.IS_DECLARATION, Flag.Function.IS_INLINE), "foo")
 *
 * Flag common to multiple kinds of Kotlin declarations ("common flags") are declared in [Flag.Common].
 * Flag applicable to specific kinds of declarations ("declaration-specific flags") are declared in nested objects of the [Flag] object.
 *
 * Some flags are mutually exclusive, i.e. there are "flag groups" such that no more than one flag from each group can be present
 * in the same bitmask. Among common flags, there are the following flag groups:
 * * visibility flags: [IS_INTERNAL], [IS_PRIVATE], [IS_PROTECTED], [IS_PUBLIC], [IS_PRIVATE_TO_THIS], [IS_LOCAL]
 * * modality flags: [IS_FINAL], [IS_OPEN], [IS_ABSTRACT], [IS_SEALED]
 *
 * Some declaration-specific flags form other flag groups, see the documentation of the corresponding containers for more information.
 *
 * @see Flags
 * @see flagsOf
 */
class Flag(private val offset: Int, private val bitWidth: Int, private val value: Int) {
    internal constructor(field: F.FlagField<*>, value: Int) : this(field.offset, field.bitWidth, value)

    internal constructor(field: F.BooleanFlagField) : this(field, 1)

    internal operator fun plus(flags: Flags): Flags =
        (flags and (((1 shl bitWidth) - 1) shl offset).inv()) + (value shl offset)

    /**
     * Checks whether the flag is present in the given bitmask.
     */
    operator fun invoke(flags: Flags): Boolean =
        (flags ushr offset) and ((1 shl bitWidth) - 1) == value

    /**
     * A container of flags applicable to Kotlin types.
     */
    object Type {
        /**
         * Signifies that the corresponding type is marked as nullable, i.e. has a question mark at the end of its notation.
         */
        @JvmField
        val IS_NULLABLE = Flag(0, 1, 1)
    }
}
