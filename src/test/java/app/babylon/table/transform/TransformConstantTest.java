package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformConstantTest
{
    @Test
    void shouldCreateStringConstantColumnWithExistingRowCount()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName COUNTRY = ColumnName.of("Country");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("A");
        names.add("B");

        TableColumnar table = Tables.newTable(TableName.of("t"), names.build());

        TableColumnar transformed = table.apply(new TransformConstant(COUNTRY, "UK"));

        ColumnObject<String> country = transformed.getString(COUNTRY);
        assertTrue(country instanceof ColumnCategorical<?>);
        assertEquals("UK", country.get(0));
        assertEquals("UK", country.get(1));
    }

    @Test
    void shouldCreatePrimitiveConstantColumnFromString()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName RANK = ColumnName.of("Rank");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("A");
        names.add("B");

        TableColumnar table = Tables.newTable(TableName.of("t"), names.build());
        TableColumnar transformed = table.apply(new TransformConstant(ColumnTypes.INT, RANK, "42"));

        ColumnInt rank = transformed.getInt(RANK);
        assertEquals(42, rank.get(0));
        assertEquals(42, rank.get(1));
    }

    @Test
    void shouldCreateTypedConstantColumn()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName AMOUNT = ColumnName.of("Amount");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("A");
        names.add("B");
        names.add("C");

        TableColumnar table = Tables.newTable(TableName.of("t"), names.build());

        TableColumnar transformed = table.apply(new TransformConstant(ColumnTypes.DECIMAL, AMOUNT, "1.25"));

        ColumnObject<BigDecimal> amount = transformed.getDecimal(AMOUNT);
        assertEquals(0, new BigDecimal("1.25").compareTo(amount.get(0)));
        assertEquals(0, new BigDecimal("1.25").compareTo(amount.get(2)));
    }

    @Test
    void shouldExposeFactoryAndToString()
    {
        TransformConstant transform = TransformConstant.of(new String[]
        {"Country", "UK"});

        assertNotNull(transform);
        assertEquals("Constant(Country, UK)", transform.toString());
        assertNull(TransformConstant.of(new String[]
        {"Country"}));
    }
}
