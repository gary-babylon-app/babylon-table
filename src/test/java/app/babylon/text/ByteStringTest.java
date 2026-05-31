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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

public class ByteStringTest
{
    @Test
    public void constructorShouldTakeOwnershipFromBuilderNotSourceBytes()
    {
        byte[] bytes =
        {1, 2, 3};
        ByteString.Builder builder = new ByteString.Builder(bytes.length);
        append(builder, bytes, 0, bytes.length);

        bytes[1] = 9;
        ByteString byteString = builder.build();

        assertEquals(3, byteString.length());
        assertEquals((byte) 2, byteString.byteAt(1));
    }

    @Test
    public void builderShouldAppendRequestedByteRange()
    {
        byte[] bytes =
        {0, 1, 2, 3};
        ByteString.Builder builder = new ByteString.Builder(2);
        append(builder, bytes, 1, 3);

        bytes[1] = 9;
        ByteString byteString = builder.build();

        assertEquals(2, byteString.length());
        assertEquals((byte) 1, byteString.byteAt(0));
        assertEquals((byte) 2, byteString.byteAt(1));
    }

    @Test
    public void toByteArrayShouldDefensivelyCopyBytes()
    {
        ByteString byteString = bytes(1, 2, 3);
        byte[] copy = byteString.toByteArray();

        copy[1] = 9;

        assertArrayEquals(new byte[]
        {1, 2, 3}, byteString.toByteArray());
    }

    @Test
    public void encodeShouldCreateByteStringUsingCharset()
    {
        ByteString byteString = ByteString.encode("R\u00A0100", StandardCharsets.UTF_8);

        assertArrayEquals(new byte[]
        {'R', (byte) 0xC2, (byte) 0xA0, '1', '0', '0'}, byteString.toByteArray());
    }

    @Test
    public void stringConstructorShouldEncodeUsingUtf8()
    {
        ByteString byteString = new ByteString("R\u00A0100");

        assertArrayEquals(new byte[]
        {'R', (byte) 0xC2, (byte) 0xA0, '1', '0', '0'}, byteString.toByteArray());
        assertEquals(StandardCharsets.UTF_8, byteString.charset());
    }

    @Test
    public void stringConstructorShouldEncodeUsingCharset()
    {
        ByteString byteString = new ByteString("R\u00A0100", StandardCharsets.ISO_8859_1);

        assertArrayEquals(new byte[]
        {'R', (byte) 0xA0, '1', '0', '0'}, byteString.toByteArray());
        assertEquals(StandardCharsets.ISO_8859_1, byteString.charset());
    }

    @Test
    public void ofShouldEncodeCharSequenceAsUtf8()
    {
        ByteString byteString = ByteString.of(new StringBuilder("R\u00A0100"));

        assertArrayEquals(new byte[]
        {'R', (byte) 0xC2, (byte) 0xA0, '1', '0', '0'}, byteString.toByteArray());
        assertEquals(StandardCharsets.UTF_8, byteString.charset());
    }

    @Test
    public void decodeShouldCreateStringUsingCharset()
    {
        ByteString byteString = bytes('R', 0xC2, 0xA0, '1', '0', '0');

        assertEquals("R\u00A0100", byteString.decode(StandardCharsets.UTF_8));
    }

    @Test
    public void decodeShouldUseStoredCharset()
    {
        ByteString.Builder builder = new ByteString.Builder(5, StandardCharsets.ISO_8859_1);
        append(builder, new byte[]
        {'R', (byte) 0xA0, '1', '0', '0'}, 0, 5);
        ByteString byteString = builder.build();

        assertEquals(StandardCharsets.ISO_8859_1, byteString.charset());
        assertEquals("R\u00A0100", byteString.decode());
    }

    @Test
    public void decodeShouldCreateStringFromRequestedRange()
    {
        ByteString byteString = ByteString.encode("abcR\u00A0100xyz", StandardCharsets.UTF_8);

        assertEquals("R\u00A0100", byteString.decode(3, byteString.length() - 3, StandardCharsets.UTF_8));
    }

    @Test
    public void constructorShouldTakeOwnershipFromBuilder()
    {
        ByteString.Builder builder = new ByteString.Builder(16, StandardCharsets.ISO_8859_1);
        builder.append((byte) 1).append((byte) 2).append((byte) 3);

        ByteString byteString = builder.build();

        assertEquals(StandardCharsets.ISO_8859_1, byteString.charset());
        assertEquals(3, byteString.length());
        assertArrayEquals(new byte[]
        {1, 2, 3}, byteString.toByteArray());
        assertThrows(IllegalStateException.class, () -> builder.append((byte) 4));
        assertThrows(IllegalStateException.class, builder::length);
    }

    @Test
    public void builderShouldAppendRanges()
    {
        ByteString source = bytes(0, 1, 2, 3, 4);
        ByteString.Builder builder = new ByteString.Builder(1);
        append(builder, new byte[]
        {9, 8, 7}, 1, 3);
        builder.append(source, 2, 5);

        ByteString byteString = builder.build();

        assertArrayEquals(new byte[]
        {8, 7, 2, 3, 4}, byteString.toByteArray());
    }

    @Test
    public void builderShouldImplementByteSequence()
    {
        ByteString.Builder builder = new ByteString.Builder(2);
        builder.append((byte) 4).append((byte) 5);

        assertEquals(2, builder.length());
        assertEquals((byte) 4, builder.byteAt(0));
        assertEquals((byte) 5, builder.byteAt(1));
    }

    @Test
    public void ofShouldMaterialiseByteSequence()
    {
        ByteString.Builder builder = new ByteString.Builder(8, StandardCharsets.ISO_8859_1);
        builder.append((byte) '1').append((byte) '2').append((byte) '3');

        ByteString byteString = ByteString.of(builder);

        assertEquals(StandardCharsets.ISO_8859_1, byteString.charset());
        assertArrayEquals(new byte[]
        {'1', '2', '3'}, byteString.toByteArray());
    }

    @Test
    public void ofShouldReturnByteStringInput()
    {
        ByteString byteString = bytes(1, 2, 3);

        assertTrue(byteString == ByteString.of(byteString));
    }

    @Test
    public void subSequenceShouldCopyByteStringSlice()
    {
        ByteString byteString = bytes(0, 1, 2, 3, 4);
        ByteString slice = byteString.subSequence(1, 4);

        assertArrayEquals(new byte[]
        {1, 2, 3}, slice.toByteArray());
        assertEquals(byteString.charset(), slice.charset());
        assertFalse(slice == byteString);
    }

    @Test
    public void builderShouldCopyByteStringSlice()
    {
        ByteString source = new ByteString("xR\u00A0100y");
        ByteString.Builder builder = new ByteString.Builder(source, 1, source.length() - 1);
        ByteString copy = builder.build();

        assertEquals("R\u00A0100", copy.decode());
        assertEquals(source.charset(), copy.charset());
    }

    @Test
    public void subSequenceShouldMaterialiseGenericByteSequenceSlice()
    {
        ByteString.Builder builder = new ByteString.Builder(5, StandardCharsets.ISO_8859_1);
        builder.append((byte) 0).append((byte) 1).append((byte) 2).append((byte) 3).append((byte) 4);

        ByteString slice = builder.subSequence(2, 5);

        assertArrayEquals(new byte[]
        {2, 3, 4}, slice.toByteArray());
        assertEquals(StandardCharsets.ISO_8859_1, slice.charset());
    }

    @Test
    public void parseStringShouldDecodeStoredBytes()
    {
        ByteString byteString = ByteString.encode("xxAlpha", StandardCharsets.UTF_8);

        assertEquals("Alpha", byteString.parseString(2, byteString.length()));
    }

    @Test
    public void parsePrimitiveNumbersShouldThrowOnInvalidInput()
    {
        ByteString byteString = ByteString.encode("xx-12|6789012345|bad", StandardCharsets.UTF_8);

        assertEquals(-12, byteString.parseInt(2, 5));
        assertEquals(6789012345L, byteString.parseLong(6, 16));
        assertThrows(NumberFormatException.class, () -> byteString.parseInt(17, 20));
        assertThrows(NumberFormatException.class, () -> byteString.parseLong(17, 20));
    }

    @Test
    public void parseDecimalShouldParseFromByteStringBacking()
    {
        ByteString byteString = ByteString.encode("xxR1 234,56yy", StandardCharsets.UTF_8);

        assertEquals(0, new BigDecimal("1234.56").compareTo(byteString.parseDecimal(2, 11)));
        assertEquals(0, new BigDecimal("1234").compareTo(byteString.parseDecimal(3, 8)));
        assertEquals(null, byteString.parseDecimal(0, 2));
    }

    @Test
    public void encodeAndDecodeShouldSupportNonLatinNoBreakSpace()
    {
        String decimal = "R12\u202F345.67";
        ByteString byteString = ByteString.encode(decimal, StandardCharsets.UTF_8);

        assertArrayEquals(new byte[]
        {'R', '1', '2', (byte) 0xE2, (byte) 0x80, (byte) 0xAF, '3', '4', '5', '.', '6', '7'}, byteString.toByteArray());
        assertEquals(decimal, byteString.decode(StandardCharsets.UTF_8));
    }

    @Test
    public void equalsAndHashCodeShouldUseByteContent()
    {
        ByteString byteString1 = bytes(1, 2, 3);
        ByteString byteString2 = bytes(1, 2, 3);
        ByteString byteString3 = bytes(1, 2, 4);

        assertEquals(byteString1, byteString2);
        assertEquals(byteString1.hashCode(), byteString2.hashCode());
        assertNotEquals(byteString1, byteString3);
        assertNotEquals(byteString1, new Object());
    }

    @Test
    public void compareToShouldUseUnsignedLexicographicOrder()
    {
        assertTrue(bytes(1, 2).compareTo(bytes(1, 3)) < 0);
        assertTrue(bytes(1, 0xFF).compareTo(bytes(1, 0x7F)) > 0);
        assertTrue(bytes(1, 2).compareTo(bytes(1, 2, 0)) < 0);
        assertEquals(0, bytes(1, 2).compareTo(bytes(1, 2)));
    }

    @Test
    public void isEmptyShouldRecogniseEmptyByteString()
    {
        assertTrue(bytes().isEmpty());
        assertFalse(bytes(1).isEmpty());
    }

    @Test
    public void toStringShouldDecodeUsingCharset()
    {
        ByteString byteString = new ByteString("R\u00A0100", StandardCharsets.UTF_8);

        assertEquals("R\u00A0100", byteString.toString());
        assertEquals("", bytes().toString());
    }

    @Test
    public void toHexStringShouldRenderHexBytesSeparatedBySpaces()
    {
        ByteString byteString = bytes(0x00, 0x0F, 0x10, 0xA0, 0xFF);

        assertEquals("00 0F 10 A0 FF", byteString.toHexString());
        assertEquals("", bytes().toHexString());
    }

    @Test
    public void ofShouldRejectInvalidInput()
    {
        assertThrows(NullPointerException.class, () -> ByteString.of((ByteSequence) null));
        assertThrows(NullPointerException.class, () -> ByteString.of((CharSequence) null));
        assertThrows(NullPointerException.class, () -> ByteString.encode(null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> ByteString.encode("abc", null));
        assertThrows(NullPointerException.class, () -> new ByteString((String) null));
        assertThrows(NullPointerException.class, () -> new ByteString("abc", null));
        assertThrows(IndexOutOfBoundsException.class, () -> bytes(1).decode(-1, 1, StandardCharsets.UTF_8));
    }

    private static ByteString bytes(int... values)
    {
        ByteString.Builder builder = new ByteString.Builder(values.length);
        for (int value : values)
        {
            builder.append((byte) value);
        }
        return builder.build();
    }

    private static void append(ByteString.Builder builder, byte[] bytes, int start, int end)
    {
        for (int i = start; i < end; ++i)
        {
            builder.append(bytes[i]);
        }
    }
}
