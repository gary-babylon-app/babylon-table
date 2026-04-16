package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformStringReplaceTest
{
    @Test
    void applyShouldReplaceLiteralValuesAndBlankOutEmptyResults()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName OUT = ColumnName.of("Out");
        ColumnObject.Builder<String> codes = ColumnObject.builder(CODE, app.babylon.table.column.ColumnTypes.STRING);
        codes.add("AA-01");
        codes.add("XX");
        codes.addNull();

        TableColumnar transformed = Tables.newTable(TableName.of("t"), codes.build())
                .apply(new TransformStringReplace(CODE, OUT, "AA-", ""));

        assertEquals("01", transformed.getString(OUT).get(0));
        assertEquals("XX", transformed.getString(OUT).get(1));
        assertFalse(transformed.getString(OUT).isSet(2));
    }
}
