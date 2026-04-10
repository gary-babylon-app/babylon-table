package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class TabularReaderCommonTest
{
    @Test
    void shouldRejectRenamingSameSourceColumnTwice()
    {
        ColumnName source = ColumnName.of("source");
        TabularReaderCsv<Object> reader = new TabularReaderCsv<>();

        reader.withColumnRename(source, ColumnName.of("target1"));

        assertThrows(IllegalArgumentException.class, () -> reader.withColumnRename(source, ColumnName.of("target2")));
    }

    @Test
    void shouldRejectBulkRenamesThatReuseTargetColumn()
    {
        Map<ColumnName, ColumnName> renames = new LinkedHashMap<>();
        renames.put(ColumnName.of("source1"), ColumnName.of("target"));
        renames.put(ColumnName.of("source2"), ColumnName.of("target"));

        TabularReaderCsv<Object> reader = new TabularReaderCsv<>();

        assertThrows(IllegalArgumentException.class, () -> reader.withColumnRenames(renames));
    }
}
