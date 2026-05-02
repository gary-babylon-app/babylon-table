package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformMetadataConstantTest
{
    @Test
    void shouldCreateConstantColumnFromTableName()
    {
        ColumnName symbol = ColumnName.of("Symbol");
        ColumnName sourceFileName = ColumnName.of("SourceFileName");

        ColumnObject.Builder<String> symbols = ColumnObject.builder(symbol, ColumnTypes.STRING);
        symbols.add("AAPL");
        symbols.add("MSFT");

        TableColumnar table = Tables.newTable(TableName.of("broker_USD_20260501.csv"), symbols.build());

        TableColumnar transformed = table
                .apply(new TransformMetadataConstant(TransformMetadataConstant.Key.TABLE_NAME, sourceFileName));

        assertTrue(transformed.get(sourceFileName) instanceof ColumnCategorical<?>);
        ColumnObject<String> sourceFiles = transformed.getString(sourceFileName);
        assertEquals("broker_USD_20260501.csv", sourceFiles.get(0));
        assertEquals("broker_USD_20260501.csv", sourceFiles.get(1));
    }

    @Test
    void shouldCreateConstantColumnFromDescription()
    {
        ColumnName symbol = ColumnName.of("Symbol");
        ColumnName sourceDescription = ColumnName.of("SourceDescription");

        ColumnObject.Builder<String> symbols = ColumnObject.builder(symbol, ColumnTypes.STRING);
        symbols.add("AAPL");

        TableColumnar table = Tables.newTable(TableName.of("Trades"), new TableDescription("May trades"),
                symbols.build());

        TableColumnar transformed = table
                .apply(new TransformMetadataConstant(TransformMetadataConstant.Key.DESCRIPTION, sourceDescription));

        assertEquals("May trades", transformed.getString(sourceDescription).get(0));
    }
}
