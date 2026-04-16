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
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformCreateConstantTest
{
    @Test
    void shouldCreateStringConstantColumnWithExistingRowCount()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName COUNTRY = ColumnName.of("Country");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("A");
        names.add("B");

        TableColumnar table = Tables.newTable(TableName.of("t"), names.build());

        TableColumnar transformed = table.apply(new TransformCreateConstant(COUNTRY, "UK"));

        ColumnObject<String> country = transformed.getString(COUNTRY);
        assertTrue(country instanceof ColumnCategorical<?>);
        assertEquals("UK", country.get(0));
        assertEquals("UK", country.get(1));
    }

    @Test
    void shouldCreateTypedConstantColumn()
    {
        final ColumnName NAME = ColumnName.of("Name");
        final ColumnName AMOUNT = ColumnName.of("Amount");

        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, app.babylon.table.column.ColumnTypes.STRING);
        names.add("A");
        names.add("B");
        names.add("C");

        TableColumnar table = Tables.newTable(TableName.of("t"), names.build());

        TableColumnar transformed = table
                .apply(new TransformCreateConstant(BigDecimal.class, AMOUNT, new BigDecimal("1.25")));

        ColumnObject<BigDecimal> amount = transformed.getDecimal(AMOUNT);
        assertEquals(0, new BigDecimal("1.25").compareTo(amount.get(0)));
        assertEquals(0, new BigDecimal("1.25").compareTo(amount.get(2)));
    }

    @Test
    void shouldExposeFactoryAndToString()
    {
        TransformCreateConstant transform = TransformCreateConstant.of(new String[]
        {"Country", "UK"});

        assertNotNull(transform);
        assertEquals("CreateConstant(Country, UK)", transform.toString());
        assertNull(TransformCreateConstant.of(new String[]
        {"Country"}));
    }
}
