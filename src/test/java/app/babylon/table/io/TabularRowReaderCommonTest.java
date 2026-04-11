package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class TabularRowReaderCommonTest
{
    @Test
    void shouldRejectRenamingSameSourceColumnTwice()
    {
        final ColumnName SOURCE = ColumnName.of("source");
        final ColumnName TARGET_1 = ColumnName.of("target1");
        final ColumnName TARGET_2 = ColumnName.of("target2");
        TabularRowReaderCsv reader = new TabularRowReaderCsv();

        reader.withColumnRename(SOURCE, TARGET_1);

        assertThrows(IllegalArgumentException.class, () -> reader.withColumnRename(SOURCE, TARGET_2));
    }

    @Test
    void shouldRejectBulkRenamesThatReuseTargetColumn()
    {
        final ColumnName SOURCE_1 = ColumnName.of("source1");
        final ColumnName SOURCE_2 = ColumnName.of("source2");
        final ColumnName TARGET = ColumnName.of("target");
        Map<ColumnName, ColumnName> renames = new LinkedHashMap<>();
        renames.put(SOURCE_1, TARGET);
        renames.put(SOURCE_2, TARGET);

        TabularRowReaderCsv reader = new TabularRowReaderCsv();

        assertThrows(IllegalArgumentException.class, () -> reader.withColumnRenames(renames));
    }
}
