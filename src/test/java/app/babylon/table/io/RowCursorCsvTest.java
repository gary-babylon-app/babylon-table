package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
