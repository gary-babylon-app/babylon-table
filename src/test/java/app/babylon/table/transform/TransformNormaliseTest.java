package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformNormaliseTest
{
    @Test
    void shouldNormaliseDecimalValues()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("0.0100"));
        amounts.add(new BigDecimal("1000.000"));
        amounts.addNull();
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());

        TableColumnar transformed = table.apply(TransformNormalise.of(amount));

        assertEquals("0.01", transformed.getDecimal(amount).get(0).toPlainString());
        assertEquals("1000", transformed.getDecimal(amount).get(1).toPlainString());
        assertEquals(0, transformed.getDecimal(amount).get(1).scale());
        assertFalse(transformed.getDecimal(amount).isSet(2));
    }

    @Test
    void shouldNormaliseIntoNewColumn()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName normalisedAmount = ColumnName.of("NormalisedAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("12.3400"));
        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());

        TableColumnar transformed = table.apply(TransformNormalise.of(amount, normalisedAmount));

        assertEquals("12.34", transformed.getDecimal(normalisedAmount).get(0).toPlainString());
        assertEquals("12.3400", transformed.getDecimal(amount).get(0).toPlainString());
    }
}
