package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;

class TransformRetainRemoveTest
{
    @Test
    void retainShouldKeepAndOrderRequestedColumns()
    {
        final ColumnName A = ColumnName.of("A");
        final ColumnName B = ColumnName.of("B");
        final ColumnName C = ColumnName.of("C");

        TableColumnar table = Tables.newTable(TableName.of("t"), column(A).build(), column(B).build(),
                column(C).build());

        TableColumnar transformed = table.apply(new TransformRetain(C, A));

        assertArrayEquals(new ColumnName[]
        {C, A}, transformed.getColumnNames());
    }

    @Test
    void removeShouldDropRequestedColumns()
    {
        final ColumnName A = ColumnName.of("A");
        final ColumnName B = ColumnName.of("B");
        final ColumnName C = ColumnName.of("C");

        TableColumnar table = Tables.newTable(TableName.of("t"), column(A).build(), column(B).build(),
                column(C).build());

        TableColumnar transformed = table.apply(new TransformRemove(B));

        assertArrayEquals(new ColumnName[]
        {A, C}, transformed.getColumnNames());
    }

    private static ColumnInt.Builder column(ColumnName name)
    {
        return ColumnInt.builder(name).add(1);
    }
}
