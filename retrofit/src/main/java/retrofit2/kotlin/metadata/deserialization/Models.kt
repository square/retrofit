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
 * These are classes representing https://github.com/JetBrains/kotlin/blob/c6697499153329c088b60404bc584bf4b85a105b/core/metadata.jvm/src/jvm_metadata.proto
 * and https://github.com/JetBrains/kotlin/blob/92d200e093c693b3c06e53a39e0b0973b84c7ec5/core/metadata/src/metadata.proto
 */

internal class StringTableTypes(val records: List<Record>) {

    companion object {
        private const val ID_RECORD = 1

        fun parse(reader: ProtobufReader): StringTableTypes {
            val records = mutableListOf<Record>()

            while (reader.readTag() != -1) {
                when (reader.currentId) {
                    ID_RECORD -> {
                        val record = Record.parse(makeDelimited(reader))
                        repeat(record.range) { records += record }
                    }
                    else -> { reader.skipElement() }
                }
            }

            return StringTableTypes(records)
        }
    }
}

internal class Record(
    val range: Int,
    val predefinedIndex: Int,
    val operation: Int,
    val string: String?,
    val substringIndexList: List<Int>,
    val replaceCharList: List<Int>
) {
    fun hasString() = string != null
    fun hasPredefinedIndex() = predefinedIndex != -1

    companion object {
        private const val ID_RANGE = 1
        private const val ID_PREDEFINED_INDEX = 2
        private const val ID_STRING = 6
        private const val ID_OPERATION = 3
        private const val ID_SUBSTRING_INDEX = 4
        private const val ID_REPLACE_CHAR = 5

        internal const val OPERATION_NONE = 0
        internal const val OPERATION_INTERNAL_TO_CLASS_ID = 1
        internal const val OPERATION_DESC_TO_CLASS_ID = 2

        fun parse(reader: ProtobufReader): Record {
            var range = 1
            var operation = 0
            var predefinedIndex = -1
            var string: String? = null
            val substringIndexList = mutableListOf<Int>()
            val replaceCharList = mutableListOf<Int>()

            while (reader.readTag() != -1) {
                when (reader.currentId) {
                    ID_RANGE -> {
                        range = reader.readInt(ProtoIntegerType.DEFAULT)
                    }
                    ID_PREDEFINED_INDEX -> {
                        predefinedIndex = reader.readInt(ProtoIntegerType.DEFAULT)
                    }
                    ID_OPERATION -> {
                        operation = reader.readInt(ProtoIntegerType.DEFAULT)
                    }
                    ID_SUBSTRING_INDEX -> {
                        readIntoList(reader, substringIndexList)
                    }
                    ID_REPLACE_CHAR -> {
                        readIntoList(reader, replaceCharList)
                    }
                    ID_STRING -> {
                        string = reader.readString()
                    }
                    else -> { reader.skipElement() }
                }
            }

            return Record(range, predefinedIndex, operation, string, substringIndexList, replaceCharList)
        }
    }
}

/**
 * Renamed from `Class` in .proto definition to [Klass] to avoid conflicting with [java.lang.Class]. We only parse the
 * info needed to determine nullability of the method's return type and skip other stuff. Thanks to
 * the protobuf format we will also be able to skip any new fields added in the future.
 */
internal class Klass(val functions: List<Function>) {

    companion object {
        private const val ID_FUNCTION = 9

        fun parse(reader: ProtobufReader): Klass {
            val functions = mutableListOf<Function>()

            while (reader.readTag() != -1) {
                if (reader.currentId == ID_FUNCTION) {
                    functions += Function.parse(makeDelimited(reader))
                } else {
                    reader.skipElement()
                }
            }

            return Klass(functions)
        }
    }
}

internal class Function(val nameIndex: Int, val returnType: Type, val signature: JvmMethodSignature) {

    fun getName(nameResolver: JvmNameResolver): String {
        return signature.getName(nameResolver) ?: nameResolver.getString(nameIndex)
    }

    companion object {
        private const val ID_NAME = 2
        private const val ID_RETURN_TYPE = 3
        private const val ID_SIGNATURE = 100

        fun parse(reader: ProtobufReader): Function {
            lateinit var returnType: Type
            var nameIndex = -1
            lateinit var signature: JvmMethodSignature

            while (reader.readTag() != -1) {
                when (reader.currentId) {
                    ID_NAME -> {
                        nameIndex = reader.readInt(ProtoIntegerType.DEFAULT)
                    }
                    ID_RETURN_TYPE -> {
                        returnType = Type.parse(makeDelimited(reader))
                    }
                    ID_SIGNATURE -> {
                        signature = JvmMethodSignature.parse(makeDelimited(reader))
                    }
                    else -> {
                        reader.skipElement()
                    }
                }
            }

            return Function(nameIndex, returnType, signature)
        }
    }
}

internal class Type(val isNullable: Boolean, private val nameIndex: Int) {

    fun isUnit(nameResolver: JvmNameResolver): Boolean = nameResolver.getString(nameIndex) == "kotlin/Unit"

    companion object {
        private const val ID_NULLABLE = 3
        private const val ID_CLASS_NAME = 6

        fun parse(reader: ProtobufReader): Type {
            var nullable = false
            var nameIndex = -1

            while (reader.readTag() != -1) {
                when (reader.currentId) {
                    ID_NULLABLE -> {
                        nullable = reader.readInt(ProtoIntegerType.DEFAULT) != 0
                    }
                    ID_CLASS_NAME -> {
                        nameIndex = reader.readInt(ProtoIntegerType.DEFAULT)
                    }
                    else -> {
                        reader.skipElement()
                    }
                }
            }

            return Type(nullable, nameIndex)
        }
    }
}

internal class JvmMethodSignature(val nameIndex: Int, val descIndex: Int) {

    fun getName(nameResolver: JvmNameResolver): String? {
        return if (nameIndex != -1) nameResolver.getString(nameIndex) else null
    }

    fun getDesc(nameResolver: JvmNameResolver): String = nameResolver.getString(descIndex)


    companion object {
        private const val ID_NAME = 1
        private const val ID_DESC = 2

        fun parse(reader: ProtobufReader): JvmMethodSignature {
            var nameIndex = -1
            var descIndex = -1
            while (reader.readTag() != -1) {
                when (reader.currentId) {
                    ID_NAME -> {
                        nameIndex = reader.readInt(ProtoIntegerType.DEFAULT)
                    }
                    ID_DESC -> {
                        descIndex = reader.readInt(ProtoIntegerType.DEFAULT)
                    }
                    else -> reader.skipElement()
                }
            }

            return JvmMethodSignature(nameIndex, descIndex)
        }
    }
}

internal fun makeDelimited(decoder: ProtobufReader, tagless: Boolean = false): ProtobufReader {
    val input = if (tagless) decoder.objectTaglessInput() else decoder.objectInput()
    return ProtobufReader(input)
}

private fun readIntoList(
    reader: ProtobufReader,
    mutableList: MutableList<Int>
) {
    if (reader.currentType == VARINT) {
        mutableList += reader.readInt(ProtoIntegerType.DEFAULT)
    } else {
        val arrayReader = ProtobufReader(reader.objectInput())
        while (0 < arrayReader.availableBytes) {
            mutableList += arrayReader.readInt(ProtoIntegerType.DEFAULT)
        }
    }
}
