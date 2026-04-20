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

class TransformStringReplaceAllTest
{
    @Test
    void applyShouldReplaceRegexMatchesAndBlankOutEmptyResults()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName OUT = ColumnName.of("Out");
        ColumnObject.Builder<String> codes = ColumnObject.builder(CODE, ColumnTypes.STRING);
        codes.add("AA-01-AA");
        codes.add("AA");
        codes.addNull();

        TableColumnar transformed = Tables.newTable(TableName.of("t"), codes.build())
                .apply(TransformStringReplaceAll.of(CODE, OUT, "AA", ""));

        assertEquals("-01-", transformed.getString(OUT).get(0));
        assertFalse(transformed.getString(OUT).isSet(1));
        assertFalse(transformed.getString(OUT).isSet(2));
    }

    @Test
    void factoriesShouldCreateWorkingTransforms()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName OUT = ColumnName.of("Out");

        TransformStringReplaceAll sameName = TransformStringReplaceAll.of(CODE, "AA", "");
        TransformStringReplaceAll renamed = TransformStringReplaceAll.of(CODE, OUT, "AA", "");
        TransformStringReplaceAll fromParams = TransformStringReplaceAll.of("Code", "Out", "AA", "");

        assertNotNull(sameName);
        assertNotNull(renamed);
        assertNotNull(fromParams);

        assertNull(TransformStringReplaceAll.of((ColumnName) null, "AA", ""));
        assertNull(TransformStringReplaceAll.of(CODE, (String) null, ""));
        assertNull(TransformStringReplaceAll.of(CODE, "", ""));
        assertNull(TransformStringReplaceAll.of(CODE, OUT, "AA", null));
        assertNull(TransformStringReplaceAll.of(new String[0]));
    }
}
