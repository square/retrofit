package retrofit2.kotlin.metadata.deserialization

import retrofit2.KotlinMetadata
import java.io.ByteArrayInputStream

class MetadataParser(val strings: Array<String>, val input: ByteArrayInputStream) {

    class Record(val range: Int, val predefinedIndex: Int, val operation: Int)

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

    private val records = parseStringTableTypes()

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
                    name = getString(nameIndex)
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

        val actualName = if (signature.nameIndex != -1) getString(signature.nameIndex) else name
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
                    name = getString(nameIndex)
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
                    desc = getString(input.readRawVarint32())
                }
                else -> input.skipProto(wireType)
            }
        }

        return Signature(nameIndex, desc)
    }

    private fun parseStringTableTypes(): List<Record> {
        val records = mutableListOf<Record>()
        input.readMessage { field, wireType ->
            when (field) {
                1 -> {
                    val record = parseRecord()
                    repeat(record.range) { records += record }
                }
                else -> { input.skipProto(wireType) }
            }
        }

        return records
    }

    private fun parseRecord(): Record {
        var range = 1
        var operation = 0
        var predefinedIndex = -1

        input.readMessage { field, wireType ->
            when (field) {
                1 -> {
                    range = input.readRawVarint32()
                }
                2 -> {
                    predefinedIndex = input.readRawVarint32()
                }
                3 -> {
                    operation = input.readRawVarint32()
                }
                else -> { input.skipProto(wireType) }
            }
        }

        return Record(range, predefinedIndex, operation)
    }

    private fun getString(index: Int): String {
        val record = records[index]

        var string = when {
            record.predefinedIndex != -1 && record.predefinedIndex in PREDEFINED_STRINGS.indices ->
                PREDEFINED_STRINGS[record.predefinedIndex]
            else -> strings[index]
        }

        when (record.operation) {
            0 -> {
                // Do nothing
            }
            1 -> {
                string = string.replace('$', '.')
            }
            2 -> {
                if (string.length >= 2) {
                    string = string.substring(1, string.length - 1)
                }
                string = string.replace('$', '.')
            }
        }

        return string
    }
}