package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.io.DataSources;
import app.babylon.table.column.ColumnDefinition;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

class RowSupplierCsvTest
{
    @Test
    void shouldDetectHeadersAndIterateDataRows()
    {
        String csv = "" + "Meta,Value\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n"
                + "2026-01-02,Salary,1000.00\n";
        RowSourceCsv source = RowSourceCsv.builder().withDataSource(DataSources.fromString(csv, "rows.csv")).build();

        try (RowSupplier supplier = source.openRows())
        {
            ColumnDefinition[] columns = supplier.columns();

            assertEquals(3, columns.length);
            assertEquals(ColumnName.of("Date"), columns[0].name());
            assertTrue(columns[0].type().isEmpty());

            assertTrue(supplier.next());
            assertArrayEquals(new String[]
            {"2026-01-01", "Coffee", "3.50"}, values(supplier.current()));

            assertTrue(supplier.next());
            assertArrayEquals(new String[]
            {"2026-01-02", "Salary", "1000.00"}, values(supplier.current()));

            assertFalse(supplier.next());
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
        RowSourceCsv source = RowSourceCsv.builder().withDataSource(DataSources.fromString(csv, "rows.csv"))
                .withColumnType(ColumnName.of("Temp"), ColumnTypes.INT_OBJECT).build();

        try (RowSupplier supplier = source.openRows())
        {
            ColumnDefinition[] columns = supplier.columns();

            assertEquals(ColumnName.of("City"), columns[0].name());
            assertTrue(columns[0].type().isEmpty());
            assertEquals(ColumnTypes.INT_OBJECT, columns[1].type().orElseThrow());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String[] values(Row row)
    {
        String[] values = new String[row.fieldCount()];
        char[] chars = row.chars();
        for (int i = 0; i < row.fieldCount(); ++i)
        {
            values[i] = new String(chars, row.start(i), row.length(i));
        }
        return values;
    }
}
