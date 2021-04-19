/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl

import kotlinx.metadata.ClassName
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf.Annotation.Argument.Value.Type.*
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver

fun ProtoBuf.Annotation.readAnnotation(strings: NameResolver): KmAnnotation =
    KmAnnotation(
        strings.getClassName(id),
        argumentList.mapNotNull { argument ->
            argument.value.readAnnotationArgument(strings)?.let { value ->
                strings.getString(argument.nameId) to value
            }
        }.toMap()
    )

fun ProtoBuf.Annotation.Argument.Value.readAnnotationArgument(strings: NameResolver): KmAnnotationArgument? {
    if (Flags.IS_UNSIGNED[flags]) {
        return when (type) {
            BYTE -> KmAnnotationArgument.LiteralValue.UByteValue(intValue.toByte().toUByte())
            SHORT -> KmAnnotationArgument.LiteralValue.UShortValue(intValue.toShort().toUShort())
            INT -> KmAnnotationArgument.LiteralValue.UIntValue(intValue.toInt().toUInt())
            LONG -> KmAnnotationArgument.LiteralValue.ULongValue(intValue.toULong())
            else -> error("Cannot read value of unsigned type: $type")
        }
    }

    return when (type) {
        BYTE -> KmAnnotationArgument.LiteralValue.ByteValue(intValue.toByte())
        CHAR -> KmAnnotationArgument.LiteralValue.CharValue(intValue.toInt().toChar())
        SHORT -> KmAnnotationArgument.LiteralValue.ShortValue(intValue.toShort())
        INT -> KmAnnotationArgument.LiteralValue.IntValue(intValue.toInt())
        LONG -> KmAnnotationArgument.LiteralValue.LongValue(intValue)
        FLOAT -> KmAnnotationArgument.LiteralValue.FloatValue(floatValue)
        DOUBLE -> KmAnnotationArgument.LiteralValue.DoubleValue(doubleValue)
        BOOLEAN -> KmAnnotationArgument.LiteralValue.BooleanValue(intValue != 0L)
        STRING -> KmAnnotationArgument.LiteralValue.StringValue(strings.getString(stringValue))
        CLASS -> KmAnnotationArgument.LiteralValue.KClassValue(strings.getClassName(classId), arrayDimensionCount)
        ENUM -> KmAnnotationArgument.LiteralValue.EnumValue(strings.getClassName(classId), strings.getString(enumValueId))
        ANNOTATION -> KmAnnotationArgument.LiteralValue.AnnotationValue(annotation.readAnnotation(strings))
        ARRAY -> KmAnnotationArgument.LiteralValue.ArrayValue(arrayElementList.mapNotNull { it.readAnnotationArgument(strings) })
        null -> null
    }
}

internal fun NameResolver.getClassName(index: Int): ClassName {
    val name = getQualifiedClassName(index)
    return if (isLocalClassName(index)) ".$name" else name
}
