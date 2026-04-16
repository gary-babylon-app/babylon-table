package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformAbsTest
{
    private static void assertDecimalEquals(String expected, BigDecimal actual)
    {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void applyColumnShouldTakeAbsoluteValues()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("-2.5"));
        amounts.addNull();
        amounts.add(new BigDecimal("4.25"));

        ColumnObject<BigDecimal> transformed = TransformAbs.of(amount).apply((Column) amounts.build());

        assertNotNull(transformed);
        assertEquals(amount, transformed.getName());
        assertDecimalEquals("2.5", transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertDecimalEquals("4.25", transformed.get(2));
    }

    @Test
    void applyMapShouldUseColumnApply()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName absAmount = ColumnName.of("AbsAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("-2.5"));
        amounts.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());
        TableColumnar transformed = table.apply(TransformAbs.of(amount, absAmount));

        assertEquals(absAmount, transformed.getDecimal(absAmount).getName());
        assertDecimalEquals("2.5", transformed.getDecimal(absAmount).get(0));
        assertFalse(transformed.getDecimal(absAmount).isSet(1));
    }

    @Test
    void factoriesShouldCreateWorkingTransforms()
    {
        ColumnName amount = ColumnName.of("Amount");
        ColumnName absAmount = ColumnName.of("AbsAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("-7.50"));
        ColumnObject<BigDecimal> source = amounts.build();

        TransformAbs sameName = TransformAbs.of(amount);
        TransformAbs renamed = TransformAbs.of(amount, absAmount);
        TransformAbs fromExpression = TransformAbs.of("Abs(Amount)");
        TransformAbs fromParams = TransformAbs.of("Amount", "AbsAmount");

        assertNotNull(sameName);
        assertNotNull(renamed);
        assertNotNull(fromExpression);
        assertNotNull(fromParams);

        assertEquals("Abs(Amount)", sameName.toString());
        assertDecimalEquals("7.50", sameName.apply((Column) source).get(0));
        assertEquals(absAmount, renamed.apply((Column) source).getName());
        assertDecimalEquals("7.50", fromExpression.apply((Column) source).get(0));
        assertEquals(absAmount, fromParams.apply((Column) source).getName());

        assertNull(TransformAbs.of((ColumnName) null));
        assertNull(TransformAbs.of((ColumnName) null, absAmount));
        assertNull(TransformAbs.of(amount, (ColumnName) null));
        assertNull(TransformAbs.of(new String[0]));
        assertNull(TransformAbs.of("Amount"));
    }
}
