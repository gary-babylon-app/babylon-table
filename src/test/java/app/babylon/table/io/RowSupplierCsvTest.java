package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

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
        RowSupplierCsv supplier = RowSupplierCsv.builder()
                .build(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

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

    @Test
    void shouldExposeExplicitCsvColumnTypes()
    {
        String csv = "City,Temp\nLondon,12\n";
        RowSupplierCsv supplier = RowSupplierCsv.builder().withColumnType(ColumnName.of("Temp"), ColumnTypes.INT_OBJECT)
                .build(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        ColumnDefinition[] columns = supplier.columns();

        assertEquals(ColumnName.of("City"), columns[0].name());
        assertTrue(columns[0].type().isEmpty());
        assertEquals(ColumnTypes.INT_OBJECT, columns[1].type().orElseThrow());
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
