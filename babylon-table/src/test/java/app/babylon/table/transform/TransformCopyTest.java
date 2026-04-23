package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

public class TransformCopyTest
{
    @Test
    public void shouldCopyColumnToNewName()
    {
        final ColumnName SYMBOL = ColumnName.of("Symbol");
        final ColumnName SYMBOL_COPY = ColumnName.of("SymbolCopy");

        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(SYMBOL, ColumnTypes.STRING);
        builder.add("VEVE");
        builder.add("SGLN");
        builder.add("VEVE");

        TableColumnar table = Tables.newTable(TableName.of("t"), builder.build());

        TableColumnar transformed = table.apply(new TransformCopy(SYMBOL, SYMBOL_COPY));

        assertTrue(transformed.get(SYMBOL_COPY) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> copied = transformed.getCategorical(SYMBOL_COPY);
        assertEquals("VEVE", copied.get(0));
        assertEquals("SGLN", copied.get(1));
        assertEquals("VEVE", copied.get(2));
        assertEquals(copied.getCategoryCode(0), copied.getCategoryCode(2));
        assertNotSame(transformed.get(SYMBOL), copied);
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Copy", "Symbol", "SymbolCopy");

        assertTrue(transform instanceof TransformCopy);
    }
}
