/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.text;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

public final class ByteString implements ByteSequence, Comparable<ByteString>
{
    private static final HexFormat HEX = HexFormat.ofDelimiter(" ").withUpperCase();
    private static final int INVALID_INDEX = -2;

    private final byte[] bytes;
    private final int length;
    private final Charset charset;
    private int hash;

    private ByteString(ByteString source, int start, int end)
    {
        Objects.requireNonNull(source, "source");
        Objects.checkFromToIndex(start, end, source.length);
        this.bytes = Arrays.copyOfRange(source.bytes, start, end);
        this.length = end - start;
        this.charset = source.charset;
    }

    private ByteString(Builder builder)
    {
        Objects.requireNonNull(builder, "builder");
        builder.checkValid();
        this.bytes = builder.bytes;
        this.length = builder.length;
        this.charset = builder.charset;
        builder.invalidate();
    }

    public ByteString(String value)
    {
        this(value, StandardCharsets.UTF_8);
    }

    public ByteString(String value, Charset charset)
    {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(charset, "charset");
        this.bytes = value.getBytes(charset);
        this.length = this.bytes.length;
        this.charset = charset;
    }

    public static ByteString of(ByteSequence bytes)
    {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes instanceof ByteString byteString)
        {
            return byteString;
        }
        Builder builder = new Builder(bytes.length(), bytes.charset());
        builder.append(bytes, 0, bytes.length());
        return builder.build();
    }

    public static ByteString of(CharSequence value)
    {
        Objects.requireNonNull(value, "value");
        return value instanceof String string ? new ByteString(string) : encode(value, StandardCharsets.UTF_8);
    }

    public static ByteString of(CharSequence value, Charset charset)
    {
        Objects.requireNonNull(value, "value");
        return value instanceof String string ? new ByteString(string, charset) : encode(value, charset);
    }

    public static ByteString encode(CharSequence value, Charset charset)
    {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(charset, "charset");
        if (value instanceof String string)
        {
            return new ByteString(string, charset);
        }
        return builder(value, charset).build();
    }

    private static Builder builder(CharSequence value, Charset charset)
    {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(charset, "charset");
        byte[] bytes = value.toString().getBytes(charset);
        Builder builder = new Builder(bytes.length, charset);
        for (byte b : bytes)
        {
            builder.append(b);
        }
        return builder;
    }

    @Override
    public int length()
    {
        return this.length;
    }

    public boolean isEmpty()
    {
        return this.length == 0;
    }

    @Override
    public byte byteAt(int index)
    {
        return this.bytes[index];
    }

    public byte[] toByteArray()
    {
        return Arrays.copyOf(this.bytes, this.length);
    }

    @Override
    public ByteString subSequence(int start, int end)
    {
        return new ByteString(this, start, end);
    }

    @Override
    public Charset charset()
    {
        return this.charset;
    }

    public String decode(Charset charset)
    {
        return decode(0, this.length, charset);
    }

    public String decode(int start, int end, Charset charset)
    {
        Objects.requireNonNull(charset, "charset");
        Objects.checkFromToIndex(start, end, this.length);
        return new String(this.bytes, start, end - start, charset);
    }

    @Override
    public String decode(int start, int end)
    {
        Objects.checkFromToIndex(start, end, this.length);
        return new String(this.bytes, start, end - start, this.charset);
    }

    public String parseString(int start, int end)
    {
        return decode(start, end);
    }

    public int parseInt(int start, int end)
    {
        Objects.checkFromToIndex(start, end, this.length);
        return parseInt(this.bytes, start, end);
    }

    public long parseLong(int start, int end)
    {
        Objects.checkFromToIndex(start, end, this.length);
        return parseLong(this.bytes, start, end);
    }

    public BigDecimal parseDecimal(int start, int end)
    {
        if (start < 0 || end > this.length || start >= end)
        {
            return null;
        }
        byte first = this.bytes[start];
        byte last = this.bytes[end - 1];
        if ((isDigit(first) || first == '-') && isDigit(last))
        {
            return parsePlainDecimalFast(start, end, false, 0);
        }
        boolean negative = false;
        boolean percent = false;
        if (last == '%')
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            percent = true;
            first = this.bytes[start];
            last = this.bytes[end - 1];
        }
        if (isCurrencySymbol(first))
        {
            ++start;
            if (start >= end)
            {
                return null;
            }
            first = this.bytes[start];
            last = this.bytes[end - 1];
        }
        else if (isCurrencySymbol(last))
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            first = this.bytes[start];
            last = this.bytes[end - 1];
        }
        if (first == '(' && last == ')')
        {
            ++start;
            --end;
            if (start >= end)
            {
                return null;
            }
            negative = true;
            first = this.bytes[start];
            last = this.bytes[end - 1];
        }
        if (isCurrencySymbol(first))
        {
            ++start;
            if (start >= end)
            {
                return null;
            }
            first = this.bytes[start];
        }
        else if (isCurrencySymbol(last))
        {
            --end;
            if (start >= end)
            {
                return null;
            }
            last = this.bytes[end - 1];
        }
        byte coreLast = this.bytes[end - 1];
        if ((isDigit(first) || first == '-' || first == '.') && (isDigit(coreLast) || coreLast == '.'))
        {
            return parsePlainDecimalFast(start, end, negative, percent ? 2 : 0);
        }
        return null;
    }

    @Override
    public int compareTo(ByteString other)
    {
        Objects.requireNonNull(other, "other");
        int length = Math.min(this.length, other.length);
        for (int i = 0; i < length; ++i)
        {
            int value1 = Byte.toUnsignedInt(this.bytes[i]);
            int value2 = Byte.toUnsignedInt(other.bytes[i]);
            if (value1 != value2)
            {
                return Integer.compare(value1, value2);
            }
        }
        return Integer.compare(this.length, other.length);
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o || o instanceof ByteString other
                && Arrays.equals(this.bytes, 0, this.length, other.bytes, 0, other.length);
    }

    @Override
    public int hashCode()
    {
        int result = this.hash;
        if (result == 0)
        {
            result = hash(this.bytes, this.length);
            this.hash = result;
        }
        return result;
    }

    @Override
    public String toString()
    {
        return decode();
    }

    public String toHexString()
    {
        return HEX.formatHex(this.bytes, 0, this.length);
    }

    private static int hash(byte[] bytes, int length)
    {
        int result = 1;
        for (int i = 0; i < length; ++i)
        {
            result = 31 * result + bytes[i];
        }
        return result;
    }

    private static int parseInt(byte[] bytes, int start, int end)
    {
        boolean negative = false;
        int i = start;
        int limit = -Integer.MAX_VALUE;
        if (i < end)
        {
            byte firstByte = bytes[i];
            if (firstByte < '0')
            {
                if (firstByte == '-')
                {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                }
                else if (firstByte != '+')
                {
                    throw new NumberFormatException();
                }
                ++i;
                if (i == end)
                {
                    throw new NumberFormatException();
                }
            }

            int multmin = limit / 10;
            int result = 0;
            while (i < end)
            {
                int digit = bytes[i] - '0';
                if (digit < 0 || digit > 9 || result < multmin)
                {
                    throw new NumberFormatException();
                }
                result *= 10;
                if (result < limit + digit)
                {
                    throw new NumberFormatException();
                }
                ++i;
                result -= digit;
            }
            return negative ? result : -result;
        }
        throw new NumberFormatException();
    }

    private static long parseLong(byte[] bytes, int start, int end)
    {
        boolean negative = false;
        int i = start;
        long limit = -Long.MAX_VALUE;
        if (i < end)
        {
            byte firstByte = bytes[i];
            if (firstByte < '0')
            {
                if (firstByte == '-')
                {
                    negative = true;
                    limit = Long.MIN_VALUE;
                }
                else if (firstByte != '+')
                {
                    throw new NumberFormatException();
                }
                ++i;
            }
            if (i >= end)
            {
                throw new NumberFormatException();
            }

            long multmin = limit / 10;
            long result = 0L;
            while (i < end)
            {
                int digit = bytes[i] - '0';
                if (digit < 0 || digit > 9 || result < multmin)
                {
                    throw new NumberFormatException();
                }
                result *= 10L;
                if (result < limit + digit)
                {
                    throw new NumberFormatException();
                }
                ++i;
                result -= digit;
            }
            return negative ? result : -result;
        }
        throw new NumberFormatException();
    }

    private BigDecimal parsePlainDecimalFast(int start, int end, boolean negative, int scaleAdjustment)
    {
        int i = start;
        if (this.bytes[i] == '-')
        {
            negative = true;
            ++i;
            if (i == end)
            {
                return null;
            }
        }

        long unscaled = 0L;
        boolean hasDigit = false;
        int commaCount = 0;
        int dotCount = 0;
        int lastCommaIndex = -1;
        int lastDotIndex = -1;
        int lastGroupingSpaceIndex = -1;
        for (; i < end; ++i)
        {
            byte c = this.bytes[i];
            int digit = digitValue(c);
            if (digit >= 0)
            {
                unscaled = unscaled * 10L + digit;
                hasDigit = true;
                continue;
            }

            switch (c)
            {
                case '.' ->
                {
                    ++dotCount;
                    lastDotIndex = i;
                }
                case ',' ->
                {
                    ++commaCount;
                    lastCommaIndex = i;
                }
                default ->
                {
                    int consumed = groupingSeparatorLength(i, end);
                    if (consumed > 0)
                    {
                        lastGroupingSpaceIndex = i;
                        i += consumed - 1;
                        continue;
                    }
                    return null;
                }
            }
        }
        if (!hasDigit)
        {
            return null;
        }
        int decimalIndex = decimalSeparatorIndex(end, commaCount, lastCommaIndex, dotCount, lastDotIndex,
                lastGroupingSpaceIndex >= 0);
        if (decimalIndex == INVALID_INDEX || lastGroupingSpaceIndex > decimalIndex && decimalIndex >= 0
                || !validGroupingSeparators(start, end, decimalIndex))
        {
            return null;
        }
        int scale = decimalIndex >= 0 ? digitCount(decimalIndex + 1, end) : 0;
        if (decimalIndex >= 0 && !validDecimalDigits(decimalIndex + 1, end))
        {
            return null;
        }
        if (negative)
        {
            unscaled = -unscaled;
        }
        return BigDecimal.valueOf(unscaled, scale + scaleAdjustment);
    }

    private static boolean isDigit(byte c)
    {
        return c >= '0' && c <= '9';
    }

    private static int digitValue(byte c)
    {
        int digit = c - '0';
        return digit >= 0 && digit <= 9 ? digit : -1;
    }

    private int decimalSeparatorIndex(int end, int commaCount, int lastCommaIndex, int dotCount, int lastDotIndex,
            boolean hasGroupingSpace)
    {
        if (commaCount > 0 && dotCount > 0)
        {
            if (lastCommaIndex > lastDotIndex && commaCount == 1)
            {
                return lastCommaIndex;
            }
            if (lastDotIndex > lastCommaIndex && dotCount == 1)
            {
                return lastDotIndex;
            }
            return INVALID_INDEX;
        }
        if (commaCount == 1)
        {
            int digitsAfterComma = digitCount(lastCommaIndex + 1, end);
            return !hasGroupingSpace && digitsAfterComma == 3 ? -1 : lastCommaIndex;
        }
        if (dotCount == 1)
        {
            return lastDotIndex;
        }
        return commaCount == 0 || dotCount == 0 ? -1 : INVALID_INDEX;
    }

    private boolean validGroupingSeparators(int start, int end, int decimalIndex)
    {
        int integerEnd = decimalIndex >= 0 ? decimalIndex : end;
        int digitsInGroup = 0;
        boolean seenGrouping = false;
        for (int i = start; i < integerEnd; ++i)
        {
            byte c = this.bytes[i];
            if (isDigit(c))
            {
                ++digitsInGroup;
                continue;
            }
            int consumed = c == ',' || c == '.' ? 1 : groupingSeparatorLength(i, integerEnd);
            if (consumed == 0)
            {
                return false;
            }
            if (digitsInGroup == 0 || seenGrouping && digitsInGroup != 3 || !seenGrouping && digitsInGroup > 3)
            {
                return false;
            }
            seenGrouping = true;
            digitsInGroup = 0;
            i += consumed - 1;
        }
        return !seenGrouping || digitsInGroup == 3;
    }

    private boolean validDecimalDigits(int start, int end)
    {
        for (int i = start; i < end; ++i)
        {
            if (!isDigit(this.bytes[i]))
            {
                return false;
            }
        }
        return true;
    }

    private int digitCount(int start, int end)
    {
        int count = 0;
        for (int i = start; i < end; ++i)
        {
            if (isDigit(this.bytes[i]))
            {
                ++count;
            }
        }
        return count;
    }

    private int groupingSeparatorLength(int index, int end)
    {
        if (this.bytes[index] == ' ')
        {
            return 1;
        }
        if (this.bytes[index] == (byte) 0xA0)
        {
            return 1;
        }
        if (index + 2 <= end && this.bytes[index] == (byte) 0xC2 && this.bytes[index + 1] == (byte) 0xA0)
        {
            return 2;
        }
        if (index + 3 <= end && this.bytes[index] == (byte) 0xE2 && this.bytes[index + 1] == (byte) 0x80
                && this.bytes[index + 2] == (byte) 0xAF)
        {
            return 3;
        }
        return 0;
    }

    private static boolean isCurrencySymbol(byte c)
    {
        return c == '$' || c == 'R';
    }

    public static final class Builder implements ByteSequence
    {
        private byte[] bytes;
        private int length;
        private final Charset charset;

        public Builder()
        {
            this(16, StandardCharsets.UTF_8);
        }

        public Builder(int capacity)
        {
            this(capacity, StandardCharsets.UTF_8);
        }

        public Builder(int capacity, Charset charset)
        {
            this.bytes = new byte[Math.max(0, capacity)];
            this.charset = Objects.requireNonNull(charset, "charset");
        }

        public Builder(ByteString source, int start, int end)
        {
            Objects.requireNonNull(source, "source");
            Objects.checkFromToIndex(start, end, source.length);
            this.bytes = Arrays.copyOfRange(source.bytes, start, end);
            this.length = end - start;
            this.charset = source.charset;
        }

        public ByteString build()
        {
            return new ByteString(this);
        }

        public Builder clear()
        {
            checkValid();
            this.length = 0;
            return this;
        }

        public Builder append(byte value)
        {
            checkValid();
            ensureCapacity(this.length + 1);
            this.bytes[this.length++] = value;
            return this;
        }

        public Builder append(byte[] source, int offset, int length)
        {
            Objects.requireNonNull(source, "source");
            Objects.checkFromIndexSize(offset, length, source.length);
            checkValid();
            ensureCapacity(this.length + length);
            System.arraycopy(source, offset, this.bytes, this.length, length);
            this.length += length;
            return this;
        }

        public Builder append(ByteSequence source, int start, int end)
        {
            Objects.requireNonNull(source, "source");
            Objects.checkFromToIndex(start, end, source.length());
            checkValid();
            ensureCapacity(this.length + end - start);
            for (int i = start; i < end; ++i)
            {
                this.bytes[this.length++] = source.byteAt(i);
            }
            return this;
        }

        public int length()
        {
            checkValid();
            return this.length;
        }

        @Override
        public Charset charset()
        {
            return this.charset;
        }

        @Override
        public byte byteAt(int index)
        {
            checkValid();
            return this.bytes[index];
        }

        private void ensureCapacity(int requiredCapacity)
        {
            if (requiredCapacity <= this.bytes.length)
            {
                return;
            }
            int capacity = Math.max(1, this.bytes.length);
            while (capacity < requiredCapacity)
            {
                capacity = Math.max(capacity * 2, requiredCapacity);
            }
            this.bytes = Arrays.copyOf(this.bytes, capacity);
        }

        private void invalidate()
        {
            this.bytes = null;
            this.length = 0;
        }

        private void checkValid()
        {
            if (this.bytes == null)
            {
                throw new IllegalStateException("builder has already been used");
            }
        }
    }
}
