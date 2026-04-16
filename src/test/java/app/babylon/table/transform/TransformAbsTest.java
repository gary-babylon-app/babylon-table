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

class TransformAbsTest
{
    @Test
    void applyShouldTakeAbsoluteValues()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName ABS_AMOUNT = ColumnName.of("AbsAmount");
        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(AMOUNT);
        amounts.add(new BigDecimal("-2.5"));
        amounts.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), amounts.build());
        TableColumnar transformed = table.apply(new TransformAbs(AMOUNT, ABS_AMOUNT));

        assertEquals(0, new BigDecimal("2.5").compareTo(transformed.getDecimal(ABS_AMOUNT).get(0)));
        assertFalse(transformed.getDecimal(ABS_AMOUNT).isSet(1));
    }
}
