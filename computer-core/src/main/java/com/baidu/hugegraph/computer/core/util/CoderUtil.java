/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.computer.core.util;

import com.baidu.hugegraph.computer.core.common.exception.ComputerException;

public class CoderUtil {

    /**
     * The byte array returned must be decode by {@link #decode(byte[] bytes)}.
     */
    public static byte[] encode(String s) {
        // Note that this code is mostly copied from DataOutputStream
        int strLen = s.length();
        int utfLen = 0;
        char c;
        int count = 0;

        // Use charAt instead of copying String to char array
        for (int i = 0; i < strLen; i++) {
            c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utfLen++;
            } else if (c > 0x07FF) {
                utfLen += 3;
            } else {
                utfLen += 2;
            }
        }

        byte[] bytes = new byte[utfLen];

        int i;
        for (i = 0; i < strLen; i++) {
            c = s.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) {
                break;
            } else {
                bytes[count++] = (byte) c;
            }
        }

        for (; i < strLen; i++) {
            c = s.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                bytes[count++] = (byte) c;
            } else if (c > 0x07FF) {
                bytes[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                bytes[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytes[count++] = (byte) (0x80 | (c & 0x3F));
            } else {
                bytes[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                bytes[count++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return bytes;
    }

    /**
     * Decode the byte array into string. Note that the bytes must be
     * generated by {@link #encode(String s)}
     */
    public static String decode(byte[] bytes) {
        return decode(bytes, 0, bytes.length);
    }

    public static String decode(byte[] bytes, int start, int length) {
        // Note that this code is mostly copied from DataInputStream
        char[] chars = new char[length];
        int c;
        int char2;
        int char3;
        int count = start;
        int charIndex = 0;

        while (count < length) {
            c = (int) bytes[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            chars[charIndex++] = (char) c;
        }

        while (count < length) {
            c = (int) bytes[count] & 0xff;
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    count++;
                    chars[charIndex++] = (char) c;
                    break;
                case 12:
                case 13:
                    // 110x xxxx   10xx xxxx
                    count += 2;
                    if (count > length) {
                        throw new ComputerException(
                                  "Malformed input: partial character at end");
                    }
                    char2 = (int) bytes[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new ComputerException(
                                  "Malformed input around byte " + count);
                    }
                    chars[charIndex++] = (char) (((c & 0x1F) << 6) |
                                                 (char2 & 0x3F));
                    break;
                case 14:
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    count += 3;
                    if (count > length) {
                        throw new ComputerException(
                                  "Malformed input: partial character at end");
                    }
                    char2 = bytes[count - 2];
                    char3 = bytes[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new ComputerException(
                                  "Malformed input around byte " + (count - 1));
                    }
                    chars[charIndex++] = (char) (((c & 0x0F) << 12) |
                                                 ((char2 & 0x3F) << 6) |
                                                 ((char3 & 0x3F) << 0));
                    break;
                default:
                    // 10xx xxxx,  1111 xxxx
                    throw new ComputerException(
                              "Malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than len
        return new String(chars, 0, charIndex);
    }
}
