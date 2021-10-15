
package retrofit2.kotlin.metadata.deserialization

import java.io.ByteArrayInputStream

class JvmNameResolver(private val input: ByteArrayInputStream, private val strings: Array<String>) {

    class Record(
        val range: Int,
        val predefinedIndex: Int,
        val operation: Int,
        val string: String?,
        val substringIndexList: List<Int>,
        val replaceCharList: List<Int>
    ) {
        fun hasString() = string != null
        fun hasPredefinedIndex() = predefinedIndex != -1
    }

    private val records = parseStringTableTypes()

    fun getString(index: Int): String {
        val record = records[index]

        var string = when {
            record.hasString() -> record.string
            record.hasPredefinedIndex() && record.predefinedIndex in PREDEFINED_STRINGS.indices ->
                PREDEFINED_STRINGS[record.predefinedIndex]
            else -> strings[index]
        }
        requireNotNull(string)

        if (record.substringIndexList.size >= 2) {
            val (begin, end) = record.substringIndexList
            if (begin in 0..end && end <= string.length) {
                string = string.substring(begin, end)
            }
        }

        if (record.replaceCharList.size >= 2) {
            val (from, to) = record.replaceCharList
            string = string.replace(from.toChar(), to.toChar())
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
        var string: String? = null
        val substringIndexList = mutableListOf<Int>()
        val replaceCharList = mutableListOf<Int>()

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
                4 -> {
                    val length = input.readRawVarint32()
                    repeat(length) {
                        substringIndexList += input.readRawVarint32()
                    }
                }
                5 -> {
                    val length = input.readRawVarint32()
                    repeat(length) {
                        replaceCharList += input.readRawVarint32()
                    }
                }
                6 -> {
                    val length = input.readRawVarint32()
                    val array = ByteArray(length)
                    input.read(array)
                    string = String(array)
                }
                else -> { input.skipProto(wireType) }
            }
        }

        return Record(range, predefinedIndex, operation, string, substringIndexList, replaceCharList)
    }

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
}