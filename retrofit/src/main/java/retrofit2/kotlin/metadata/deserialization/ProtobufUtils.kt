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

import java.io.ByteArrayInputStream

fun ByteArrayInputStream.skipProto(wire: Int) {
    when (wire) {
        0 -> readRawVarint32()
        1 -> skip(8L)
        2 -> skip(readRawVarint32().toLong())
        5 -> skip(4L)
        else -> throw IllegalStateException("Invalid wire type: $wire")
    }
}

inline fun ByteArrayInputStream.readMessage(block: (Int, Int) -> Unit) {
    val size = readRawVarint32()

    val start = available()
    while (true) {
        if (start - available() >= size) break

        val tag = readTag()
        if (tag == 0) break

        val wire = tag and 7
        val field = tag ushr 3
        block(field, wire)
    }
}

fun ByteArrayInputStream.readTag(): Int {
    if (available() == 0) {
        return 0
    }

    val result = readRawVarint32()
    if (result ushr 3 == 0) {
        // If we actually read zero (or any tag number corresponding to field
        // number zero), that's not a valid tag.
        throw IllegalStateException("Invalid tag")
    }
    return result
}

fun ByteArrayInputStream.readRawVarint32(): Int {
    // See implementation notes for readRawVarint64
    fastpath@while (true) {
        if (available() == 0) {
            reset()
            break@fastpath
        }
        mark(0)
        var x: Int
        if (readByte().also { x = it.toInt() } >= 0) {
            return x
        } else if (available() < 9) {
            reset()
            break@fastpath
        } else if ((readByte().toInt() shl 7).let { x = x xor it; x } < 0L) {
            x = x xor (0L.inv() shl 7).toInt()
        } else if ((readByte().toInt() shl 14).let { x = x xor it; x } >= 0L) {
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14)).toInt()
        } else if ((readByte().toInt() shl 21).let { x = x xor it; x } < 0L) {
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14) xor (0L.inv() shl 21)).toInt()
        } else {
            val y = readByte().toInt()
            x = x xor (y shl 28)
            x = x xor (0L.inv() shl 7 xor (0L.inv() shl 14) xor (0L.inv() shl 21) xor (0L.inv() shl 28)).toInt()
            if (y < 0 && readByte() < 0 && readByte() < 0 && readByte() < 0 && readByte() < 0 && readByte() < 0) {
                reset()
                break@fastpath  // Will throw malformedVarint()
            }
        }
        return x
    }

    return readRawVarint64SlowPath().toInt()
}


fun ByteArrayInputStream.readRawVarint64SlowPath(): Long {
    var result: Long = 0
    var shift = 0
    while (shift < 64) {
        val b = read().toLong()
        result = result or ((b and 0x7F) shl shift)
        if ((b and 0x80) == 0L) {
            return result
        }
        shift += 7
    }
    throw IllegalStateException("Malformed varint")
}

fun ByteArrayInputStream.readByte(): Byte = read().toByte()