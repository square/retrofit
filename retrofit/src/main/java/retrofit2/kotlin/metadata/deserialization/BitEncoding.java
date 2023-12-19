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
package retrofit2.kotlin.metadata.deserialization;

import static retrofit2.kotlin.metadata.deserialization.UtfEncodingKt.MAX_UTF8_INFO_LENGTH;

import org.jetbrains.annotations.NotNull;

/**
 * This file was adapted from
 * https://github.com/JetBrains/kotlin/blob/af18b10da9d1e20b1b35831a3fb5e508048a2576/core/metadata.jvm/src/org/jetbrains/kotlin/metadata/jvm/deserialization/BitEncoding.java
 * by removing the unused parts.
 */
public class BitEncoding {

  private static final char _8TO7_MODE_MARKER = (char) -1;

  private BitEncoding() {}

  private static void addModuloByte(@NotNull byte[] data, int increment) {
    for (int i = 0, n = data.length; i < n; i++) {
      data[i] = (byte) ((data[i] + increment) & 0x7f);
    }
  }

  /** Converts encoded array of {@code String} back to a byte array. */
  @NotNull
  public static byte[] decodeBytes(@NotNull String[] data) {
    if (data.length > 0 && !data[0].isEmpty()) {
      char possibleMarker = data[0].charAt(0);
      if (possibleMarker == UtfEncodingKt.UTF8_MODE_MARKER) {
        return UtfEncodingKt.stringsToBytes(dropMarker(data));
      }
      if (possibleMarker == _8TO7_MODE_MARKER) {
        data = dropMarker(data);
      }
    }

    byte[] bytes = combineStringArrayIntoBytes(data);
    // Adding 0x7f modulo max byte value is equivalent to subtracting 1 the same modulo, which is
    // inverse to what happens in encodeBytes
    addModuloByte(bytes, 0x7f);
    return decode7to8(bytes);
  }

  @NotNull
  private static String[] dropMarker(@NotNull String[] data) {
    // Clone because the clients should be able to use the passed array for their own purposes.
    // This is cheap because the size of the array is 1 or 2 almost always.
    String[] result = data.clone();
    result[0] = result[0].substring(1);
    return result;
  }

  /** Combines the array of strings resulted from encodeBytes() into one long byte array */
  @NotNull
  private static byte[] combineStringArrayIntoBytes(@NotNull String[] data) {
    int resultLength = 0;
    for (String s : data) {
      assert s.length() <= MAX_UTF8_INFO_LENGTH : "String is too long: " + s.length();
      resultLength += s.length();
    }

    byte[] result = new byte[resultLength];
    int p = 0;
    for (String s : data) {
      for (int i = 0, n = s.length(); i < n; i++) {
        result[p++] = (byte) s.charAt(i);
      }
    }

    return result;
  }

  /**
   * Decodes the byte array resulted from encode8to7().
   *
   * <p>Each byte of the input array has at most 7 valuable bits of information. So the decoding is
   * equivalent to the following: least significant 7 bits of all input bytes are combined into one
   * long bit string. This bit string is then split into groups of 8 bits, each of which forms a
   * byte in the output. If there are any leftovers, they are ignored, since they were added just as
   * a padding and do not comprise a full byte.
   *
   * <p>Suppose the following encoded byte array is given (bits are numbered the same way as in
   * encode8to7() doc):
   *
   * <p>01234567 01234567 01234567 01234567
   *
   * <p>The output of the following form would be produced:
   *
   * <p>01234560 12345601 23456012
   *
   * <p>Note how all most significant bits and leftovers are dropped, since they don't contain any
   * useful information
   */
  @NotNull
  private static byte[] decode7to8(@NotNull byte[] data) {
    // floor(7 * data.length / 8)
    int resultLength = 7 * data.length / 8;

    byte[] result = new byte[resultLength];

    // We maintain a pointer to an input bit in the same fashion as in encode8to7(): it's
    // represented as two numbers: index of the
    // current byte in the input and index of the bit in the byte
    int byteIndex = 0;
    int bit = 0;

    // A resulting byte is comprised of 8 bits, starting from the current bit. Since each input byte
    // only "contains 7 bytes", a
    // resulting byte always consists of two parts: several most significant bits of the current
    // byte and several least significant bits
    // of the next byte
    for (int i = 0; i < resultLength; i++) {
      int firstPart = (data[byteIndex] & 0xff) >>> bit;
      byteIndex++;
      int secondPart = (data[byteIndex] & ((1 << (bit + 1)) - 1)) << 7 - bit;
      result[i] = (byte) (firstPart + secondPart);

      if (bit == 6) {
        byteIndex++;
        bit = 0;
      } else {
        bit++;
      }
    }

    return result;
  }
}
