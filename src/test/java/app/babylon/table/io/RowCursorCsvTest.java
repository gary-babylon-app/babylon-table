package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.io.TestStreamSources;
import app.babylon.io.StreamSources;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

class RowCursorCsvTest
{
    @Test
    void shouldDetectHeadersAndIterateDataRows()
    {
        String csv = "" + "Meta,Value\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n"
                + "2026-01-02,Salary,1000.00\n";
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "rows.csv"))
                .build();

        try (RowCursor rowCursort = source.openRows())
        {
            ColumnDefinition[] columns = rowCursort.columns();

            assertEquals(3, columns.length);
            assertEquals(ColumnName.of("Date"), columns[0].name());
            assertEquals(null, columns[0].type());

            assertTrue(rowCursort.next());
            assertArrayEquals(new String[]
            {"2026-01-01", "Coffee", "3.50"}, values(rowCursort.current()));

            assertTrue(rowCursort.next());
            assertArrayEquals(new String[]
            {"2026-01-02", "Salary", "1000.00"}, values(rowCursort.current()));

            assertFalse(rowCursort.next());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldExposeExplicitCsvColumnTypes()
    {
        String csv = "City,Temp\nLondon,12\n";
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "rows.csv"))
                .withColumnType(ColumnName.of("Temp"), ColumnTypes.INT_OBJECT).build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(null, columns[0].type());
            assertEquals(ColumnTypes.INT_OBJECT, columns[1].type());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldExposeExplicitCsvColumnTypesFromMap()
    {
        String csv = "City,Temp\nLondon,12.5\n";
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "rows.csv"))
                .withColumnTypes(Map.of(ColumnName.of("Temp"), ColumnTypes.DOUBLE_OBJECT)).build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(null, columns[0].type());
            assertEquals(ColumnTypes.DOUBLE_OBJECT, columns[1].type());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldDetectUtf16LeWithoutBom()
    {
        byte[] bytes = "City,Temp\nLondon,12\n".getBytes(StandardCharsets.UTF_16LE);
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(TestStreamSources.fromBytes(bytes, "rows.csv"))
                .build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(ColumnName.of("Temp"), columns[1].name());

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
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "rows.csv"))
                .build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(ColumnName.of("Note"), columns[1].name());

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
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "rows.csv"))
                .withSeparator(';').withQuote('\'').build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(ColumnName.of("Note"), columns[1].name());

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
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(TestStreamSources.fromBytes(bytes, "rows.csv"))
                .build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(ColumnName.of("Note"), columns[1].name());

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
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(text, "rows.txt"))
                .withFixedWidths(new int[]
                {3, 2, 3}).withHeaderStrategy(new HeaderStrategyNoHeaders(1)).withAutoDetectEncoding(false).build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("Column1"), columns[0].name());
            assertEquals(ColumnName.of("Column2"), columns[1].name());
            assertEquals(ColumnName.of("Column3"), columns[2].name());

            assertTrue(rowCursor.next());
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
    void shouldTreatNullFixedWidthsAsRegularDelimitedCsv()
    {
        String csv = "City,Note\nParis,\"Price,12\"\n";
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "rows.csv"))
                .withFixedWidths(null).build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(ColumnName.of("Note"), columns[1].name());

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
        RowSourceCsv source = RowSourceCsv.builder().withStreamSource(TestStreamSources.fromBytes(bytes, "rows.csv"))
                .withCharset(java.nio.charset.Charset.forName("windows-1252")).withAutoDetectEncoding(false).build();

        try (RowCursor rowCursor = source.openRows())
        {
            ColumnDefinition[] columns = rowCursor.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertEquals(ColumnName.of("Note"), columns[1].name());

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

    private static String[] values(Row row)
    {
        String[] values = new String[row.size()];
        char[] chars = row.chars();
        for (int i = 0; i < row.size(); ++i)
        {
            values[i] = new String(chars, row.start(i), row.length(i));
        }
        return values;
    }
}
