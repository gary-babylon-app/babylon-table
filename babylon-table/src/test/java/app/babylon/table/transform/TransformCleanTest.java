package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformCleanTest
{
    @Test
    void applyShouldRewriteValuesUsingFormatterFunction()
    {
        final ColumnName CODE = ColumnName.of("Code");
        ColumnObject.Builder<String> codes = ColumnObject.builder(CODE, ColumnTypes.STRING);
        codes.add(" ab ");
        codes.addNull();

        TableColumnar transformed = Tables.newTable(TableName.of("t"), codes.build())
                .apply(new TransformClean(CODE, s -> s == null ? null : s.strip().toUpperCase(Locale.UK)));

        assertEquals("AB", transformed.getString(CODE).get(0));
        assertEquals("", transformed.getString(CODE).get(1));
    }
}
