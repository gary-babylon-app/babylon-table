package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformAddTest
{
    @Test
    void applyShouldAddDecimalColumns()
    {
        final ColumnName LEFT = ColumnName.of("Left");
        final ColumnName RIGHT = ColumnName.of("Right");
        final ColumnName TOTAL = ColumnName.of("Total");

        ColumnObject.Builder<BigDecimal> left = ColumnObject.builderDecimal(LEFT);
        left.add(new BigDecimal("1.25"));
        ColumnObject.Builder<BigDecimal> right = ColumnObject.builderDecimal(RIGHT);
        right.add(new BigDecimal("2.75"));

        TableColumnar transformed = Tables.newTable(TableName.of("t"), left.build(), right.build())
                .apply(new TransformAdd(LEFT, RIGHT, TOTAL));

        assertEquals(0, new BigDecimal("4.00").compareTo(transformed.getDecimal(TOTAL).get(0)));
    }
}
