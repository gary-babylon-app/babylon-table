package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformAbsTest
{
    private static void assertDecimalEquals(String expected, BigDecimal actual)
    {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void applyColumnShouldTakeAbsoluteValues()
    {
        final ColumnName amount = ColumnName.of("Amount");
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
        final ColumnName amount = ColumnName.of("Amount");
        final ColumnName absAmount = ColumnName.of("AbsAmount");
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
    void applyMapShouldTakeAbsoluteValuesOnlyWhenBooleanColumnIsTrue()
    {
        final ColumnName amount = ColumnName.of("Amount");
        final ColumnName shouldAbs = ColumnName.of("ShouldAbs");
        final ColumnName absAmount = ColumnName.of("AbsAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("-2.5"));
        amounts.add(new BigDecimal("-3.5"));
        amounts.add(new BigDecimal("-4.5"));
        amounts.addNull();
        ColumnBoolean.Builder flags = ColumnBoolean.builder(shouldAbs);
        flags.add(true);
        flags.add(false);
        flags.addNull();
        flags.add(true);

        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build(), flags.build());
        TableColumnar transformed = table
                .apply(TransformAbs.builder(amount).when(shouldAbs).withNewColumnName(absAmount).build());

        assertDecimalEquals("2.5", transformed.getDecimal(absAmount).get(0));
        assertDecimalEquals("-3.5", transformed.getDecimal(absAmount).get(1));
        assertDecimalEquals("-4.5", transformed.getDecimal(absAmount).get(2));
        assertFalse(transformed.getDecimal(absAmount).isSet(3));
    }

    @Test
    void applyMapShouldRejectNonBooleanConditionColumn()
    {
        final ColumnName amount = ColumnName.of("Amount");
        final ColumnName condition = ColumnName.of("Condition");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(amount);
        amounts.add(new BigDecimal("-2.5"));
        ColumnObject.Builder<String> conditions = ColumnObject.builder(condition, ColumnTypes.STRING);
        conditions.add("true");

        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build(), conditions.build());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> table.apply(TransformAbs.builder(amount).when(condition).build()));

        assertTrue(exception.getMessage().contains("when requires Boolean column"));
    }

    @Test
    void factoriesShouldCreateWorkingTransforms()
    {
        final ColumnName amount = ColumnName.of("Amount");
        final ColumnName absAmount = ColumnName.of("AbsAmount");
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
