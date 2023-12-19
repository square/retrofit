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
 * This file was adapted from https://github.com/Kotlin/kotlinx.serialization/blob/1814a92b871dac128db67c765c9df2b6be8405c7/formats/protobuf/commonMain/src/kotlinx/serialization/protobuf/internal/Streams.kt
 * and https://github.com/Kotlin/kotlinx.serialization/blob/1814a92b871dac128db67c765c9df2b6be8405c7/formats/protobuf/commonMain/src/kotlinx/serialization/protobuf/internal/ProtobufReader.kt
 * by removing the unused parts.
 */

internal open class SerializationException(message: String?) : IllegalArgumentException(message)

internal class ProtobufDecodingException(message: String) : SerializationException(message)

class ByteArrayInput(private var array: ByteArray, private val endIndex: Int = array.size) {
    private var position: Int = 0
    val availableBytes: Int get() = endIndex - position

    fun slice(size: Int): ByteArrayInput {
        ensureEnoughBytes(size)
        val result = ByteArrayInput(array, position + size)
        result.position = position
        position += size
        return result
    }

    fun read(): Int {
        return if (position < endIndex) array[position++].toInt() and 0xFF else -1
    }

    fun readExactNBytes(bytesCount: Int): ByteArray {
        ensureEnoughBytes(bytesCount)
        val b = ByteArray(bytesCount)
        val length = b.size
        // Are there any bytes available?
        val copied = if (endIndex - position < length) endIndex - position else length
        array.copyInto(destination = b, destinationOffset = 0, startIndex = position, endIndex = position + copied)
        position += copied
        return b
    }

    private fun ensureEnoughBytes(bytesCount: Int) {
        if (bytesCount > availableBytes) {
            throw SerializationException("Unexpected EOF, available $availableBytes bytes, requested: $bytesCount")
        }
    }

    fun readString(length: Int): String {
        val result = array.decodeToString(position, position + length)
        position += length
        return result
    }

    fun readVarint32(): Int {
        if (position == endIndex) {
            eof()
        }

        // Fast-path: unrolled loop for single and two byte values
        var currentPosition = position
        var result = array[currentPosition++].toInt()
        if (result >= 0) {
            position  = currentPosition
            return result
        } else if (endIndex - position > 1) {
            result = result xor (array[currentPosition++].toInt() shl 7)
            if (result < 0) {
                position = currentPosition
                return result xor (0.inv() shl 7)
            }
        }

        return readVarint32SlowPath()
    }

    fun readVarint64(eofAllowed: Boolean): Long {
        if (position == endIndex) {
            if (eofAllowed) return -1
            else eof()
        }

        // Fast-path: single and two byte values
        var currentPosition = position
        var result = array[currentPosition++].toLong()
        if (result >= 0) {
            position  = currentPosition
            return result
        } else if (endIndex - position > 1) {
            result = result xor (array[currentPosition++].toLong() shl 7)
            if (result < 0) {
                position = currentPosition
                return result xor (0L.inv() shl 7)
            }
        }

        return readVarint64SlowPath()
    }

    private fun eof() {
        throw SerializationException("Unexpected EOF")
    }

    private fun readVarint64SlowPath(): Long {
        var result = 0L
        var shift = 0
        while (shift < 64) {
            val byte = read()
            result = result or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) {
                return result
            }
            shift += 7
        }
        throw SerializationException("Input stream is malformed: Varint too long (exceeded 64 bits)")
    }

    private fun readVarint32SlowPath(): Int {
        var result = 0
        var shift = 0
        while (shift < 32) {
            val byte = read()
            result = result or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) {
                return result
            }
            shift += 7
        }
        throw SerializationException("Input stream is malformed: Varint too long (exceeded 32 bits)")
    }
}

internal enum class ProtoIntegerType {
    DEFAULT,
    SIGNED,
    FIXED;
}

internal const val VARINT = 0
internal const val i64 = 1
internal const val SIZE_DELIMITED = 2
internal const val i32 = 5

internal class ProtobufReader(private val input: ByteArrayInput) {
    @JvmField
    var currentId = -1
    @JvmField
    var currentType = -1

    val availableBytes: Int
        get() = input.availableBytes

    fun readTag(): Int {
        val header = input.readVarint64(true).toInt()
        return if (header == -1) {
            currentId = -1
            currentType = -1
            -1
        } else {
            currentId = header ushr 3
            currentType = header and 0b111
            currentId
        }
    }

    fun skipElement() {
        when (currentType) {
            VARINT -> readInt(ProtoIntegerType.DEFAULT)
            i64 -> readLong(ProtoIntegerType.FIXED)
            SIZE_DELIMITED -> readByteArray()
            i32 -> readInt(ProtoIntegerType.FIXED)
            else -> throw ProtobufDecodingException("Unsupported start group or end group wire type: $currentType")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun assertWireType(expected: Int) {
        if (currentType != expected) throw ProtobufDecodingException("Expected wire type $expected, but found $currentType")
    }

    private fun readByteArray(): ByteArray {
        assertWireType(SIZE_DELIMITED)
        return readByteArrayNoTag()
    }

    private fun readByteArrayNoTag(): ByteArray {
        val length = decode32()
        checkLength(length)
        return input.readExactNBytes(length)
    }

    fun objectInput(): ByteArrayInput {
        assertWireType(SIZE_DELIMITED)
        return objectTaglessInput()
    }

    fun objectTaglessInput(): ByteArrayInput {
        val length = decode32()
        checkLength(length)
        return input.slice(length)
    }

    fun readInt(format: ProtoIntegerType): Int {
        val wireType = if (format == ProtoIntegerType.FIXED) i32 else VARINT
        assertWireType(wireType)
        return decode32(format)
    }

    private fun readLong(format: ProtoIntegerType): Long {
        val wireType = if (format == ProtoIntegerType.FIXED) i64 else VARINT
        assertWireType(wireType)
        return decode64(format)
    }

    private fun readIntLittleEndian(): Int {
        // TODO this could be optimized by extracting method to the IS
        var result = 0
        for (i in 0..3) {
            val byte = input.read() and 0x000000FF
            result = result or (byte shl (i * 8))
        }
        return result
    }

    private fun readLongLittleEndian(): Long {
        // TODO this could be optimized by extracting method to the IS
        var result = 0L
        for (i in 0..7) {
            val byte = (input.read() and 0x000000FF).toLong()
            result = result or (byte shl (i * 8))
        }
        return result
    }

    fun readString(): String {
        assertWireType(SIZE_DELIMITED)
        val length = decode32()
        checkLength(length)
        return input.readString(length)
    }

    private fun checkLength(length: Int) {
        if (length < 0) {
            throw ProtobufDecodingException("Unexpected negative length: $length")
        }
    }

    private fun decode32(format: ProtoIntegerType = ProtoIntegerType.DEFAULT): Int = when (format) {
        ProtoIntegerType.DEFAULT -> input.readVarint64(false).toInt()
        ProtoIntegerType.SIGNED -> decodeSignedVarintInt(input)
        ProtoIntegerType.FIXED -> readIntLittleEndian()
    }

    private fun decode64(format: ProtoIntegerType = ProtoIntegerType.DEFAULT): Long = when (format) {
        ProtoIntegerType.DEFAULT -> input.readVarint64(false)
        ProtoIntegerType.SIGNED -> decodeSignedVarintLong(input)
        ProtoIntegerType.FIXED -> readLongLittleEndian()
    }

    /**
     *  Source for all varint operations:
     *  https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/util/Varint.java
     */
    private fun decodeSignedVarintInt(input: ByteArrayInput): Int {
        val raw = input.readVarint32()
        val temp = raw shl 31 shr 31 xor raw shr 1
        // This extra step lets us deal with the largest signed values by treating
        // negative results from read unsigned methods as like unsigned values.
        // Must re-flip the top bit if the original read value had it set.
        return temp xor (raw and (1 shl 31))
    }

    private fun decodeSignedVarintLong(input: ByteArrayInput): Long {
        val raw = input.readVarint64(false)
        val temp = raw shl 63 shr 63 xor raw shr 1
        // This extra step lets us deal with the largest signed values by treating
        // negative results from read unsigned methods as like unsigned values
        // Must re-flip the top bit if the original read value had it set.
        return temp xor (raw and (1L shl 63))

    }
}
