package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.ColumnCategorical;
import app.babylon.table.ColumnName;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.Transform;

public class TransformCopyTest
{
    @Test
    public void shouldCopyColumnToNewName()
    {
        ColumnName from = ColumnName.of("Symbol");
        ColumnName to = ColumnName.of("SymbolCopy");

        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(from, String.class);
        builder.add("VEVE");
        builder.add("SGLN");
        builder.add("VEVE");

        TableColumnar table = Tables.newTable(TableName.of("t"), builder.build());

        TableColumnar transformed = table.apply(new TransformCopy(from, to));

        assertTrue(transformed.get(to) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> copied = transformed.getCategorical(to);
        assertEquals("VEVE", copied.get(0));
        assertEquals("SGLN", copied.get(1));
        assertEquals("VEVE", copied.get(2));
        assertEquals(copied.getCategoryCode(0), copied.getCategoryCode(2));
        assertNotSame(transformed.get(from), copied);
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Copy", "Symbol", "SymbolCopy");

        assertTrue(transform instanceof TransformCopy);
    }
}
