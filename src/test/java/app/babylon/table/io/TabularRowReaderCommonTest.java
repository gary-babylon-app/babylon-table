package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class TabularRowReaderCommonTest
{
    @Test
    void resultFactoriesShouldExposeStatusMessageAndCause()
    {
        RuntimeException cause = new RuntimeException("boom");

        TabularRowReader.Result success = TabularRowReader.Result.success();
        TabularRowReader.Result successWithMessage = TabularRowReader.Result.success("ok");
        TabularRowReader.Result warning = TabularRowReader.Result.warning("warn");
        TabularRowReader.Result empty = TabularRowReader.Result.empty("empty");
        TabularRowReader.Result exception = TabularRowReader.Result.exception("bad", cause);

        assertEquals(TabularRowReader.Status.SUCCESS, success.getStatus());
        assertNull(success.getMessage());
        assertNull(success.getCause());
        assertTrue(success.isSuccessLike());

        assertEquals(TabularRowReader.Status.SUCCESS, successWithMessage.getStatus());
        assertEquals("ok", successWithMessage.getMessage());
        assertNull(successWithMessage.getCause());
        assertTrue(successWithMessage.isSuccessLike());

        assertEquals(TabularRowReader.Status.WARNING, warning.getStatus());
        assertEquals("warn", warning.getMessage());
        assertTrue(warning.isSuccessLike());

        assertEquals(TabularRowReader.Status.EMPTY, empty.getStatus());
        assertEquals("empty", empty.getMessage());
        assertTrue(empty.isSuccessLike());

        assertEquals(TabularRowReader.Status.EXCEPTION, exception.getStatus());
        assertEquals("bad", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertFalse(exception.isSuccessLike());
    }

    @Test
    void statusShouldReportSuccessLikeAndFailure()
    {
        assertTrue(TabularRowReader.Status.SUCCESS.isSuccessLike());
        assertTrue(TabularRowReader.Status.WARNING.isSuccessLike());
        assertTrue(TabularRowReader.Status.EMPTY.isSuccessLike());
        assertFalse(TabularRowReader.Status.EXCEPTION.isSuccessLike());

        assertFalse(TabularRowReader.Status.SUCCESS.isFailure());
        assertFalse(TabularRowReader.Status.WARNING.isFailure());
        assertFalse(TabularRowReader.Status.EMPTY.isFailure());
        assertTrue(TabularRowReader.Status.EXCEPTION.isFailure());
    }

    @Test
    void resultShouldRejectNullStatus()
    {
        assertThrows(RuntimeException.class, () -> new TabularRowReader.Result(null, "x", null));
    }

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
