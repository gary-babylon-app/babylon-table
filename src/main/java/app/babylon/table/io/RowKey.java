/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

public abstract class RowKey
{
    public static RowKey copyOf(Row row, int[] positions)
    {
        if (row == null)
        {
            throw new IllegalArgumentException("row must not be null");
        }
        if (positions == null || positions.length == 0)
        {
            throw new IllegalArgumentException("positions must not be empty");
        }

        return switch (positions.length)
        {
            case 1 -> RowKey1.copyOf(row, positions[0]);
            case 2 -> RowKey2.copyOf(row, positions[0], positions[1]);
            case 3 -> RowKey3.copyOf(row, positions[0], positions[1], positions[2]);
            default -> RowKeyN.copyOfN(row, positions);
        };
    }

    public static RowKey copyOf(CharSequence[] values, int[] positions)
    {
        if (values == null)
        {
            throw new IllegalArgumentException("values must not be null");
        }
        if (positions == null || positions.length == 0)
        {
            throw new IllegalArgumentException("positions must not be empty");
        }
        return switch (positions.length)
        {
            case 1 -> RowKey1.copyOf(values[positions[0]]);
            case 2 -> RowKey2.copyOf(values[positions[0]], values[positions[1]]);
            case 3 -> RowKey3.copyOf(values[positions[0]], values[positions[1]], values[positions[2]]);
            default -> RowKeyN.copyOfN(values, positions);
        };
    }

    public abstract int fieldCount();

    protected abstract int fieldStart(int fieldIndex);

    protected abstract int fieldLength(int fieldIndex);

    protected abstract char charAt(int fieldIndex, int charIndex);

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    public String getString(int fieldIndex)
    {
        char[] chars = new char[fieldLength(fieldIndex)];
        for (int i = 0; i < chars.length; ++i)
        {
            chars[i] = charAt(fieldIndex, i);
        }
        return new String(chars);
    }

    @Override
    public final String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("RowKey[");
        for (int i = 0; i < fieldCount(); ++i)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(getString(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private static char[] copyChars(Row row, int... positions)
    {
        int totalLength = 0;
        for (int position : positions)
        {
            totalLength += row.length(position);
        }
        char[] chars = new char[totalLength];
        char[] source = row.chars();
        int writeIndex = 0;
        for (int position : positions)
        {
            int length = row.length(position);
            System.arraycopy(source, row.start(position), chars, writeIndex, length);
            writeIndex += length;
        }
        return chars;
    }

    private static char[] copyChars(CharSequence... values)
    {
        int totalLength = 0;
        for (CharSequence value : values)
        {
            if (value != null)
            {
                totalLength += value.length();
            }
        }
        char[] chars = new char[totalLength];
        int writeIndex = 0;
        for (CharSequence value : values)
        {
            if (value == null)
            {
                continue;
            }
            for (int i = 0; i < value.length(); ++i)
            {
                chars[writeIndex++] = value.charAt(i);
            }
        }
        return chars;
    }

    private static final class RowKey1 extends RowKey
    {
        private int hash;
        private final char[] chars;

        private RowKey1(Row row, int position)
        {
            int length = row.length(position);
            this.chars = new char[length];
            System.arraycopy(row.chars(), row.start(position), this.chars, 0, length);
        }

        private static RowKey1 copyOf(Row row, int position)
        {
            return new RowKey1(row, position);
        }

        private RowKey1(CharSequence value)
        {
            this.chars = copyChars(value);
        }

        private static RowKey1 copyOf(CharSequence value)
        {
            return new RowKey1(value);
        }

        @Override
        public int fieldCount()
        {
            return 1;
        }

        @Override
        protected int fieldStart(int fieldIndex)
        {
            return 0;
        }

        @Override
        protected int fieldLength(int fieldIndex)
        {
            return this.chars.length;
        }

        @Override
        protected char charAt(int fieldIndex, int charIndex)
        {
            return this.chars[charIndex];
        }

        @Override
        public int hashCode()
        {
            int h = this.hash;
            if (h == 0)
            {
                h = 1;
                h = 31 * h + this.chars.length;
                for (char c : this.chars)
                {
                    h = 31 * h + c;
                }
                this.hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof RowKey other))
            {
                return false;
            }
            if (other.fieldCount() != 1 || other.fieldLength(0) != this.chars.length)
            {
                return false;
            }
            if (other instanceof RowKey1 otherChar)
            {
                for (int i = 0; i < this.chars.length; ++i)
                {
                    if (this.chars[i] != otherChar.chars[i])
                    {
                        return false;
                    }
                }
                return true;
            }
            for (int i = 0; i < this.chars.length; ++i)
            {
                if (this.chars[i] != other.charAt(0, i))
                {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class RowKey2 extends RowKey
    {
        private int hash;
        private final char[] chars;
        private final int length0;

        private RowKey2(Row row, int position0, int position1)
        {
            this.length0 = row.length(position0);
            int length1 = row.length(position1);
            this.chars = new char[this.length0 + length1];
            char[] source = row.chars();
            System.arraycopy(source, row.start(position0), this.chars, 0, this.length0);
            System.arraycopy(source, row.start(position1), this.chars, this.length0, length1);
        }

        private static RowKey2 copyOf(Row row, int position1, int position2)
        {
            return new RowKey2(row, position1, position2);
        }

        private RowKey2(CharSequence value0, CharSequence value1)
        {
            this.length0 = value0 == null ? 0 : value0.length();
            this.chars = copyChars(value0, value1);
        }

        private static RowKey2 copyOf(CharSequence value0, CharSequence value1)
        {
            return new RowKey2(value0, value1);
        }

        @Override
        public int fieldCount()
        {
            return 2;
        }

        @Override
        protected int fieldStart(int fieldIndex)
        {
            return fieldIndex == 0 ? 0 : this.length0;
        }

        @Override
        protected int fieldLength(int fieldIndex)
        {
            return fieldIndex == 0 ? this.length0 : this.chars.length - this.length0;
        }

        @Override
        protected char charAt(int fieldIndex, int charIndex)
        {
            return this.chars[fieldStart(fieldIndex) + charIndex];
        }

        @Override
        public int hashCode()
        {
            int h = this.hash;
            if (h == 0)
            {
                h = 2;
                h = 31 * h + this.length0;
                for (char c : this.chars)
                {
                    h = 31 * h + c;
                }
                this.hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj instanceof RowKey2 otherKey)
            {
                if (this.length0 != otherKey.length0 || this.chars.length != otherKey.chars.length)
                {
                    return false;
                }
                for (int i = 0; i < this.chars.length; ++i)
                {
                    if (this.chars[i] != otherKey.chars[i])
                    {
                        return false;
                    }
                }
                return true;
            }
            if (!(obj instanceof RowKey other))
            {
                return false;
            }

            int length1 = this.chars.length - this.length0;
            if (other.fieldCount() != 2 || other.fieldLength(0) != this.length0 || other.fieldLength(1) != length1)
            {
                return false;
            }
            for (int i = 0; i < this.length0; ++i)
            {
                if (this.chars[i] != other.charAt(0, i))
                {
                    return false;
                }
            }
            for (int i = 0; i < length1; ++i)
            {
                if (this.chars[this.length0 + i] != other.charAt(1, i))
                {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class RowKey3 extends RowKey
    {
        private int hash;
        private final char[] chars;
        private final int length1;
        private final int length2;
        private final int length3;

        private RowKey3(Row row, int position0, int position1, int position2)
        {
            this.length1 = row.length(position0);
            this.length2 = row.length(position1);
            this.length3 = row.length(position2);
            this.chars = new char[this.length1 + this.length2 + this.length3];
            char[] source = row.chars();
            System.arraycopy(source, row.start(position0), this.chars, 0, this.length1);
            System.arraycopy(source, row.start(position1), this.chars, this.length1, this.length2);
            System.arraycopy(source, row.start(position2), this.chars, this.length1 + this.length2, this.length3);
        }

        private static RowKey3 copyOf(Row row, int position1, int position2, int position3)
        {
            return new RowKey3(row, position1, position2, position3);
        }

        private RowKey3(CharSequence value0, CharSequence value1, CharSequence value2)
        {
            this.length1 = value0 == null ? 0 : value0.length();
            this.length2 = value1 == null ? 0 : value1.length();
            this.length3 = value2 == null ? 0 : value2.length();
            this.chars = copyChars(value0, value1, value2);
        }

        private static RowKey3 copyOf(CharSequence value0, CharSequence value1, CharSequence value2)
        {
            return new RowKey3(value0, value1, value2);
        }

        @Override
        public int fieldCount()
        {
            return 3;
        }

        @Override
        protected int fieldStart(int fieldIndex)
        {
            return switch (fieldIndex)
            {
                case 0 -> 0;
                case 1 -> this.length1;
                default -> this.length1 + this.length2;
            };
        }

        @Override
        protected int fieldLength(int fieldIndex)
        {
            return switch (fieldIndex)
            {
                case 0 -> this.length1;
                case 1 -> this.length2;
                default -> this.length3;
            };
        }

        @Override
        protected char charAt(int fieldIndex, int charIndex)
        {
            return this.chars[fieldStart(fieldIndex) + charIndex];
        }

        @Override
        public int hashCode()
        {
            int h = this.hash;
            if (h == 0)
            {
                h = 3;
                int index = 0;

                h = 31 * h + this.length1;
                for (int i = 0; i < this.length1; ++i)
                {
                    h = 31 * h + this.chars[index++];
                }

                h = 31 * h + this.length2;
                for (int i = 0; i < this.length2; ++i)
                {
                    h = 31 * h + this.chars[index++];
                }

                h = 31 * h + this.length3;
                for (int i = 0; i < this.length3; ++i)
                {
                    h = 31 * h + this.chars[index++];
                }
                this.hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof RowKey other))
            {
                return false;
            }
            if (other.fieldCount() != 3 || other.fieldLength(0) != this.length1 || other.fieldLength(1) != this.length2
                    || other.fieldLength(2) != this.length3)
            {
                return false;
            }
            if (other instanceof RowKey3 otherKey)
            {
                for (int i = 0; i < this.length1 + this.length2 + this.length3; ++i)
                {
                    if (this.chars[i] != otherKey.chars[i])
                    {
                        return false;
                    }
                }
                return true;
            }
            int index = 0;
            for (int i = 0; i < this.length1; ++i)
            {
                if (this.chars[index++] != other.charAt(0, i))
                {
                    return false;
                }
            }
            for (int i = 0; i < this.length2; ++i)
            {
                if (this.chars[index++] != other.charAt(1, i))
                {
                    return false;
                }
            }
            for (int i = 0; i < this.length3; ++i)
            {
                if (this.chars[index++] != other.charAt(2, i))
                {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class RowKeyN extends RowKey
    {
        private int hash;
        private final char[] chars;
        private final int[] starts;
        private final int[] lengths;

        private RowKeyN(Row row, int[] positions)
        {
            this.chars = copyChars(row, positions);
            this.starts = new int[positions.length];
            this.lengths = new int[positions.length];
            int writeIndex = 0;
            for (int i = 0; i < positions.length; ++i)
            {
                int length = row.length(positions[i]);
                this.starts[i] = writeIndex;
                this.lengths[i] = length;
                writeIndex += length;
            }
        }

        private static RowKeyN copyOfN(Row row, int[] positions)
        {
            return new RowKeyN(row, positions);
        }

        private RowKeyN(CharSequence[] values, int[] positions)
        {
            CharSequence[] selected = new CharSequence[positions.length];
            for (int i = 0; i < positions.length; ++i)
            {
                selected[i] = values[positions[i]];
            }
            this.chars = copyChars(selected);
            this.starts = new int[positions.length];
            this.lengths = new int[positions.length];
            int writeIndex = 0;
            for (int i = 0; i < positions.length; ++i)
            {
                int length = selected[i] == null ? 0 : selected[i].length();
                this.starts[i] = writeIndex;
                this.lengths[i] = length;
                writeIndex += length;
            }
        }

        private static RowKeyN copyOfN(CharSequence[] values, int[] positions)
        {
            return new RowKeyN(values, positions);
        }

        @Override
        public int fieldCount()
        {
            return this.starts.length;
        }

        @Override
        protected int fieldStart(int fieldIndex)
        {
            return this.starts[fieldIndex];
        }

        @Override
        protected int fieldLength(int fieldIndex)
        {
            return this.lengths[fieldIndex];
        }

        @Override
        protected char charAt(int fieldIndex, int charIndex)
        {
            return this.chars[this.starts[fieldIndex] + charIndex];
        }

        @Override
        public int hashCode()
        {
            int h = this.hash;
            if (h == 0)
            {
                h = this.starts.length;
                for (int i = 0; i < this.starts.length; ++i)
                {
                    h = 31 * h + this.lengths[i];
                    int start = this.starts[i];
                    int end = start + this.lengths[i];
                    for (int j = start; j < end; ++j)
                    {
                        h = 31 * h + this.chars[j];
                    }
                }
                this.hash = h;
            }
            return h;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!(obj instanceof RowKey other))
            {
                return false;
            }
            if (other.fieldCount() != this.starts.length)
            {
                return false;
            }
            for (int i = 0; i < this.starts.length; ++i)
            {
                if (other.fieldLength(i) != this.lengths[i])
                {
                    return false;
                }
            }
            if (other instanceof RowKeyN otherKey)
            {
                for (int i = 0; i < this.starts.length; ++i)
                {
                    int start = this.starts[i];
                    int otherStart = otherKey.starts[i];
                    int end = start + this.lengths[i];
                    for (int a = start, b = otherStart; a < end; ++a, ++b)
                    {
                        if (this.chars[a] != otherKey.chars[b])
                        {
                            return false;
                        }
                    }
                }
                return true;
            }
            for (int i = 0; i < this.starts.length; ++i)
            {
                int start = this.starts[i];
                int end = start + this.lengths[i];
                for (int a = start, b = 0; a < end; ++a, ++b)
                {
                    if (this.chars[a] != other.charAt(i, b))
                    {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
