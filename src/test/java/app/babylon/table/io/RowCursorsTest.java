package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import app.babylon.io.StreamSources;
import app.babylon.io.TestStreamSources;
import app.babylon.table.TableException;

class RowCursorsTest
{
    @Test
    void shouldIterateRawCsvRows()
    {
        String csv = "Date,Description,Amount\n2026-01-01,Coffee,3.50\n";
        RowSource source = RowSources.create(ReadOptionsCsv.standard(), StreamSources.fromString(csv, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertInstanceOf(ByteStringSlices.class, rowCursor.current());
            assertArrayEquals(new String[]
            {"Date", "Description", "Amount"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"2026-01-01", "Coffee", "3.50"}, values(rowCursor.current()));

            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldCreateStandardCsvRowSource()
    {
        String csv = "Date,Description,Amount\n2026-01-01,Coffee,3.50\n";
        RowSource source = RowSources.create(StreamSources.fromString(csv, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"Date", "Description", "Amount"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"2026-01-01", "Coffee", "3.50"}, values(rowCursor.current()));

            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReadUtf16LeDelimitedCsvWithCharacterStateMachine()
    {
        byte[] bytes = "City,Temp\nLondon,12\n".getBytes(StandardCharsets.UTF_16LE);
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withCharset(StandardCharsets.UTF_16LE)
                .withAutoDetectOptions(false).build();
        RowSource source = RowSources.create(csvFormat, TestStreamSources.fromBytes(bytes, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertInstanceOf(StringSlices.class, rowCursor.current());
            assertArrayEquals(new String[]
            {"City", "Temp"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"London", "12"}, values(rowCursor.current()));

            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldAutoDetectCommaAndDoubleQuote()
    {
        String csv = "City,Note\nParis,\"Price,12\"\n";
        RowSource source = RowSources.create(ReadOptionsCsv.standard(), StreamSources.fromString(csv, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"Paris", "Price,12"}, values(rowCursor.current()));
            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReadSemicolonAndSingleQuoteWhenConfigured()
    {
        String csv = "City;Note\nParis;'Price;12'\n";
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withSeparator(';').withQuote('\'').build();
        RowSource source = RowSources.create(csvFormat, StreamSources.fromString(csv, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"Paris", "Price;12"}, values(rowCursor.current()));
            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldFallbackToWindows1252WhenUtf8IsInvalid()
    {
        byte[] bytes = "City,Note\nParis,Price €12\n".getBytes(java.nio.charset.Charset.forName("windows-1252"));
        RowSource source = RowSources.create(ReadOptionsCsv.standard(), TestStreamSources.fromBytes(bytes, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"Paris", "Price €12"}, values(rowCursor.current()));
            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReadFixedWidthRowsWhenConfigured()
    {
        String text = "ABC12XYZ\nDEF34UVW\n";
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withFixedWidths(new int[]
        {3, 2, 3}).withAutoDetectOptions(false).build();
        RowSource source = RowSources.create(csvFormat, StreamSources.fromString(text, "rows.txt"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertInstanceOf(StringSlices.class, rowCursor.current());
            assertArrayEquals(new String[]
            {"ABC", "12", "XYZ"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"DEF", "34", "UVW"}, values(rowCursor.current()));

            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReadFixedWidthRowsAcrossBufferBoundary()
    {
        String row = "ABC12XYZ\n";
        String text = row.repeat(912);
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withFixedWidths(new int[]
        {3, 2, 3}).withAutoDetectOptions(false).build();
        RowSource source = RowSources.create(csvFormat, StreamSources.fromString(text, "rows.txt"));

        try (RowCursor rowCursor = source.openRows())
        {
            int rowCount = 0;
            while (rowCursor.next())
            {
                assertArrayEquals(new String[]
                {"ABC", "12", "XYZ"}, values(rowCursor.current()));
                ++rowCount;
            }
            assertEquals(912, rowCount);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldTreatNullFixedWidthsAsRegularDelimitedCsv()
    {
        String csv = "City,Note\nParis,\"Price,12\"\n";
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withFixedWidths(null).build();
        RowSource source = RowSources.create(csvFormat, StreamSources.fromString(csv, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"Paris", "Price,12"}, values(rowCursor.current()));
            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldUseConfiguredCharsetWhenAutoDetectEncodingIsDisabled()
    {
        byte[] bytes = "City,Note\nParis,Price €12\n".getBytes(java.nio.charset.Charset.forName("windows-1252"));
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder()
                .withCharset(java.nio.charset.Charset.forName("windows-1252")).withAutoDetectOptions(false).build();
        RowSource source = RowSources.create(csvFormat, TestStreamSources.fromBytes(bytes, "rows.csv"));

        try (RowCursor rowCursor = source.openRows())
        {
            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"City", "Note"}, values(rowCursor.current()));

            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"Paris", "Price €12"}, values(rowCursor.current()));
            assertFalse(rowCursor.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldExposeConfiguredPhysicalReaderOptions()
    {
        int[] fixedWidths = new int[]
        {3, 2, 3};
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withSeparator(';').withQuote('\'')
                .withFixedWidths(fixedWidths).withCharset(StandardCharsets.UTF_16LE).withAutoDetectOptions(false)
                .build();
        byte[] bytes = "ABC12XYZ\n".getBytes(StandardCharsets.UTF_16LE);
        RowCursor rowCursor = RowCursors.create(csvFormat, TestStreamSources.fromBytes(bytes, "rows.txt").openStream());
        try
        {
            assertInstanceOf(RowCursorCharFixedWidth.class, rowCursor);
            assertTrue(rowCursor.next());
            assertArrayEquals(new String[]
            {"ABC", "12", "XYZ"}, values(rowCursor.current()));
            assertFalse(rowCursor.next());
        } finally
        {
            try
            {
                rowCursor.close();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void shouldWrapClosedInputStreamFailureInTableException()
    {
        InputStream inputStream = InputStream.nullInputStream();
        try
        {
            inputStream.close();
            RowCursors.create(ReadOptionsCsv.standard(), inputStream);
            fail("Expected TableException");
        }
        catch (TableException e)
        {
            assertNotNull(e.getCause());
            assertTrue(e.getMessage() != null && !e.getMessage().isBlank());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String[] values(RowValues row)
    {
        String[] values = new String[row.size()];
        for (int i = 0; i < row.size(); ++i)
        {
            values[i] = row.getString(i);
        }
        return values;
    }
}
