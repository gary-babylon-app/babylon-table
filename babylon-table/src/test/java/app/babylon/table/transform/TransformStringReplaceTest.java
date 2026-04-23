package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformStringReplaceTest
{
    @Test
    void applyShouldReplaceLiteralValuesAndBlankOutEmptyResults()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName OUT = ColumnName.of("Out");
        ColumnObject.Builder<String> codes = ColumnObject.builder(CODE, ColumnTypes.STRING);
        codes.add("AA-01");
        codes.add("XX");
        codes.addNull();

        TableColumnar transformed = Tables.newTable(TableName.of("t"), codes.build())
                .apply(TransformStringReplace.of(CODE, OUT, "AA-", ""));

        assertEquals("01", transformed.getString(OUT).get(0));
        assertEquals("XX", transformed.getString(OUT).get(1));
        assertFalse(transformed.getString(OUT).isSet(2));
    }

    @Test
    void factoriesShouldCreateWorkingTransforms()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName OUT = ColumnName.of("Out");

        TransformStringReplace sameName = TransformStringReplace.of(CODE, "AA-", "");
        TransformStringReplace renamed = TransformStringReplace.of(CODE, OUT, "AA-", "");
        TransformStringReplace fromParams = TransformStringReplace.of("Code", "Out", "AA-", "");

        assertNotNull(sameName);
        assertNotNull(renamed);
        assertNotNull(fromParams);

        assertNull(TransformStringReplace.of((ColumnName) null, "AA-", ""));
        assertNull(TransformStringReplace.of(CODE, (String) null, ""));
        assertNull(TransformStringReplace.of(CODE, "", ""));
        assertNull(TransformStringReplace.of(CODE, OUT, "AA-", null));
        assertNull(TransformStringReplace.of(new String[0]));
    }
}
