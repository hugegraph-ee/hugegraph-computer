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

package com.baidu.hugegraph.computer.core.io;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.lang.reflect.Field;

import com.baidu.hugegraph.computer.core.common.Constants;
import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.graph.id.Id;
import com.baidu.hugegraph.computer.core.graph.id.IdFactory;
import com.baidu.hugegraph.computer.core.graph.value.Value;
import com.baidu.hugegraph.computer.core.graph.value.ValueFactory;
import com.baidu.hugegraph.util.E;

import sun.misc.Unsafe;

public class UnsafeByteArrayGraphInput implements GraphInput {

    private static final sun.misc.Unsafe UNSAFE;

    private byte[] buffer;
    private int limit;
    private int position;

    static {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) field.get(null);
        } catch (Exception e) {
            throw new ComputerException("Failed to get unsafe", e);
        }
    }

    public UnsafeByteArrayGraphInput(byte[] buffer) {
        this(buffer, buffer.length);
    }

    public UnsafeByteArrayGraphInput(byte[] buffer, int limit) {
        E.checkArgumentNotNull(buffer, "The buffer can't be null");
        this.buffer = buffer;
        this.limit = limit;
        this.position = 0;
    }

    @Override
    public Id readId() throws IOException {
        byte type = this.readByte();
        Id id = IdFactory.createID(type);
        id.read(this);
        return id;
    }

    @Override
    public Value readValue() throws IOException {
        byte typeCode = this.readByte();
        Value value = ValueFactory.createValue(typeCode);
        value.read(this);
        return value;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        this.readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        this.require(len);
        System.arraycopy(this.buffer, this.position, b, off, len);
        this.position += len;
    }

    @Override
    public int skipBytes(int n) throws IOException {
        int remaining = remaining();
        if (remaining >= n) {
            this.position += n;
            return n;
        } else {
            this.position += remaining;
            return remaining;
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        this.require(Constants.BOOLEAN_LEN);
        boolean value = UNSAFE.getBoolean(this.buffer,
                                          Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                          this.position);
        this.position += Constants.BOOLEAN_LEN;
        return value;
    }

    @Override
    public byte readByte() throws IOException {
        this.require(Constants.BYTE_LEN);
        byte value = this.buffer[position];
        this.position += Constants.BYTE_LEN;
        return value;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        this.require(Constants.BYTE_LEN);
        int value = this.buffer[position] & 0xFF;
        this.position += Constants.BYTE_LEN;
        return value;
    }

    @Override
    public short readShort() throws IOException {
        this.require(Constants.SHORT_LEN);
        short value = UNSAFE.getShort(this.buffer,
                                      Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                      this.position);
        this.position += Constants.SHORT_LEN;
        return value;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        this.require(Constants.SHORT_LEN);
        int value = UNSAFE.getShort(this.buffer,
                                      Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                      this.position) & 0xFFFF;
        this.position += Constants.SHORT_LEN;
        return value;
    }

    @Override
    public char readChar() throws IOException {
        this.require(Constants.CHAR_LEN);
        char value = UNSAFE.getChar(this.buffer,
                                    Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                    this.position);
        this.position += Constants.CHAR_LEN;
        return value;
    }

    @Override
    public int readInt() throws IOException {
        this.require(Constants.INT_LEN);
        int value = UNSAFE.getInt(this.buffer,
                                  Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                  this.position);
        this.position += Constants.INT_LEN;
        return value;
    }

    @Override
    public long readLong() throws IOException {
        this.require(Constants.LONG_LEN);
        long value = UNSAFE.getLong(this.buffer,
                                  Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                  this.position);
        this.position += Constants.LONG_LEN;
        return value;
    }

    @Override
    public float readFloat() throws IOException {
        this.require(Constants.FLOAT_LEN);
        float value = UNSAFE.getFloat(this.buffer,
                                      Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                      this.position);
        this.position += Constants.FLOAT_LEN;
        return value;
    }

    @Override
    public double readDouble() throws IOException {
        this.require(Constants.DOUBLE_LEN);
        double value = UNSAFE.getDouble(this.buffer,
                                        Unsafe.ARRAY_BYTE_BASE_OFFSET +
                                        this.position);
        this.position += Constants.DOUBLE_LEN;
        return value;
    }

    @Override
    public String readLine() throws IOException {
        throw new ComputerException("Not implemented yet");
    }

    @Override
    public String readUTF() throws IOException {
        // Note that this code is mostly copied from DataInputStream
        int len = readUnsignedShort();

        byte[] bytes = new byte[len];
        char[] chars = new char[len];
        int c;
        int char2;
        int char3;
        int count = 0;
        int charIndex = 0;

        this.readFully(bytes, 0, len);

        while (count < len) {
            c = (int) bytes[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            chars[charIndex++] = (char) c;
        }

        while (count < len) {
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
                    /* 0xxxxxxx */
                    count++;
                    chars[charIndex++] = (char) c;
                    break;
                case 12:
                case 13:
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > len) {
                        throw new UTFDataFormatException(
                                  "Malformed input: partial character at end");
                    }
                    char2 = (int) bytes[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException(
                                  "Malformed input around byte " + count);
                    }
                    chars[charIndex++] = (char) (((c & 0x1F) << 6) |
                                                (char2 & 0x3F));
                    break;
                case 14:
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > len) {
                        throw new UTFDataFormatException(
                                  "Malformed input: partial character at end");
                    }
                    char2 = bytes[count - 2];
                    char3 = bytes[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new UTFDataFormatException(
                                  "Malformed input around byte " + (count - 1));
                    }
                    chars[charIndex++] = (char) (((c & 0x0F) << 12) |
                                                ((char2 & 0x3F) << 6) |
                                                ((char3 & 0x3F) << 0));
                    break;
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new UTFDataFormatException(
                              "Malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than len
        return new String(chars, 0, charIndex);
    }

    public int position() {
        return this.position;
    }

    private void require(int size) {
        if (this.position + size > this.limit) {
            throw new ComputerException(
                      "Only %s bytes available, trying to read %s bytes",
                      this.limit - this.position, size);
        }
    }

    public int remaining() {
        return this.limit - this.position;
    }
}
