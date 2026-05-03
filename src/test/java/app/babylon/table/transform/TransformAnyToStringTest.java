package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformAnyToStringTest
{
    @Test
    void applyShouldConvertSelectedColumnsToStrings()
    {
        final ColumnName QUANTITY = ColumnName.of("Quantity");
        final ColumnName TEXT = ColumnName.of("Text");
        ColumnObject.Builder<Integer> quantity = ColumnObject.builder(QUANTITY, ColumnTypes.INT_OBJECT);
        quantity.add(12);
        quantity.addNull();

        TableColumnar transformed = Tables.newTable(TableName.of("t"), quantity.build())
                .apply(new TransformAnyToString(QUANTITY, TEXT));

        assertEquals("12", transformed.getString(TEXT).get(0));
        assertEquals("", transformed.getString(TEXT).get(1));
    }

    @Test
    void ofShouldCreateTransformFromParams()
    {
        assertTrue(TransformAnyToString.of("quantity", "text") instanceof TransformAnyToString);
        assertEquals(null, TransformAnyToString.of("quantity"));
    }
}
