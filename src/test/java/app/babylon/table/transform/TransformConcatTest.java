package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformConcatTest
{
    @Test
    void shouldConcatSourceColumnsUsingSeparator()
    {
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName LAST = ColumnName.of("Last");
        final ColumnName FULL = ColumnName.of("Full");

        ColumnObject.Builder<String> first = ColumnObject.builder(FIRST, ColumnTypes.STRING);
        first.add("Ada");
        first.add("Grace");

        ColumnObject.Builder<String> last = ColumnObject.builder(LAST, ColumnTypes.STRING);
        last.add("Lovelace");
        last.add("Hopper");

        TableColumnar table = Tables.newTable(TableName.of("t"), first.build(), last.build());

        TableColumnar transformed = table.apply(TransformConcat.of(FULL, " ", FIRST, LAST));

        ColumnObject<String> full = transformed.getString(FULL);
        assertEquals("Ada Lovelace", full.get(0));
        assertEquals("Grace Hopper", full.get(1));
    }

    @Test
    void shouldConcatColumnsAndLiteralValues()
    {
        final ColumnName ACCOUNT_TYPE = ColumnName.of("AccountType");
        final ColumnName COUNTRY = ColumnName.of("Country");
        final ColumnName ACCOUNT_NUMBER = ColumnName.of("AccountNumber");
        final ColumnName ACCOUNT_KEY = ColumnName.of("AccountKey");

        ColumnObject.Builder<String> accountType = ColumnObject.builder(ACCOUNT_TYPE, ColumnTypes.STRING);
        accountType.add("CASH");
        accountType.add("MARGIN");

        ColumnObject.Builder<String> country = ColumnObject.builder(COUNTRY, ColumnTypes.STRING);
        country.add("GB");
        country.add("US");

        ColumnObject.Builder<String> accountNumber = ColumnObject.builder(ACCOUNT_NUMBER, ColumnTypes.STRING);
        accountNumber.add("12345");
        accountNumber.add("67890");

        TableColumnar table = Tables.newTable(TableName.of("t"), accountType.build(), country.build(),
                accountNumber.build());

        TableColumnar transformed = table.apply(TransformConcat.of(ACCOUNT_KEY, "|",
                TransformConcat.Part.column(ACCOUNT_TYPE), TransformConcat.Part.column(COUNTRY),
                TransformConcat.Part.literal("ACCT"), TransformConcat.Part.column(ACCOUNT_NUMBER)));

        ColumnObject<String> accountKey = transformed.getString(ACCOUNT_KEY);
        assertEquals("CASH|GB|ACCT|12345", accountKey.get(0));
        assertEquals("MARGIN|US|ACCT|67890", accountKey.get(1));
    }

    @Test
    void shouldConcatIterableParts()
    {
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName LAST = ColumnName.of("Last");
        final ColumnName FULL = ColumnName.of("Full");

        ColumnObject.Builder<String> first = ColumnObject.builder(FIRST, ColumnTypes.STRING);
        first.add("Ada");

        ColumnObject.Builder<String> last = ColumnObject.builder(LAST, ColumnTypes.STRING);
        last.add("Lovelace");

        TableColumnar table = Tables.newTable(TableName.of("t"), first.build(), last.build());

        TableColumnar transformed = table.apply(TransformConcat.of(FULL, "", List.of(TransformConcat.Part.column(FIRST),
                TransformConcat.Part.literal(" "), TransformConcat.Part.column(LAST))));

        assertEquals("Ada Lovelace", transformed.getString(FULL).get(0));
    }

    @Test
    void shouldConcatDateAndTimeTextToLocalDateTime()
    {
        final ColumnName DATE = ColumnName.of("Date");
        final ColumnName TIME = ColumnName.of("Time");
        final ColumnName DATE_TIME = ColumnName.of("DateTime");

        ColumnObject.Builder<String> date = ColumnObject.builder(DATE, ColumnTypes.STRING);
        date.add("2026-05-08");
        date.add("bad");

        ColumnObject.Builder<String> time = ColumnObject.builder(TIME, ColumnTypes.STRING);
        time.add("13:45:30");
        time.add("09:00:00");

        TableColumnar table = Tables.newTable(TableName.of("t"), date.build(), time.build());

        TableColumnar transformed = table.apply(
                TransformConcat.of(DATE_TIME, "T", ColumnTypes.LOCAL_DATE_TIME, ColumnObject.Mode.AUTO, DATE, TIME));

        ColumnObject<LocalDateTime> dateTimes = transformed.getObject(DATE_TIME, ColumnTypes.LOCAL_DATE_TIME);
        assertEquals(LocalDateTime.of(2026, 5, 8, 13, 45, 30), dateTimes.get(0));
        assertFalse(dateTimes.isSet(1));
    }

    @Test
    void shouldBuildColumnFromTable()
    {
        final ColumnName DATE = ColumnName.of("Date");
        final ColumnName TIME = ColumnName.of("Time");
        final ColumnName DATE_TIME = ColumnName.of("DateTime");

        ColumnObject.Builder<String> date = ColumnObject.builder(DATE, ColumnTypes.STRING);
        date.add("2026-05-08");

        ColumnObject.Builder<String> time = ColumnObject.builder(TIME, ColumnTypes.STRING);
        time.add("13:45:30");

        TableColumnar table = Tables.newTable(TableName.of("t"), date.build(), time.build());

        Column column = TransformConcat
                .of(DATE_TIME, "T", ColumnTypes.LOCAL_DATE_TIME, ColumnObject.Mode.AUTO, DATE, TIME).transform(table);

        ColumnObject<LocalDateTime> dateTimes = assertInstanceOf(ColumnObject.class, column);
        assertEquals(DATE_TIME, dateTimes.getName());
        assertEquals(ColumnTypes.LOCAL_DATE_TIME, dateTimes.getType());
        assertEquals(LocalDateTime.of(2026, 5, 8, 13, 45, 30), dateTimes.get(0));
    }

    @Test
    void shouldBuildColumnFromColumnMap()
    {
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName LAST = ColumnName.of("Last");
        final ColumnName FULL = ColumnName.of("Full");

        ColumnObject.Builder<String> first = ColumnObject.builder(FIRST, ColumnTypes.STRING);
        first.add("Ada");

        ColumnObject.Builder<String> last = ColumnObject.builder(LAST, ColumnTypes.STRING);
        last.add("Lovelace");

        Map<ColumnName, Column> columnsByName = new LinkedHashMap<>();
        columnsByName.put(FIRST, first.build());
        columnsByName.put(LAST, last.build());

        ColumnObject<String> full = assertInstanceOf(ColumnObject.class,
                TransformConcat.of(FULL, " ", FIRST, LAST).transform(columnsByName, 1));

        assertEquals(FULL, full.getName());
        assertEquals("Ada Lovelace", full.get(0));
    }

    @Test
    void shouldConcatToCategoricalTargetType()
    {
        final ColumnName DATE = ColumnName.of("Date");
        final ColumnName TIME = ColumnName.of("Time");
        final ColumnName DATE_TIME = ColumnName.of("DateTime");

        ColumnObject.Builder<String> date = ColumnObject.builder(DATE, ColumnTypes.STRING);
        date.add("2026-05-08");
        date.add("2026-05-09");
        date.add("2026-05-08");

        ColumnObject.Builder<String> time = ColumnObject.builder(TIME, ColumnTypes.STRING);
        time.add("13:45:30");
        time.add("09:00:00");
        time.add("13:45:30");

        TableColumnar table = Tables.newTable(TableName.of("t"), date.build(), time.build());

        TableColumnar transformed = table.apply(TransformConcat.of(DATE_TIME, "T", ColumnTypes.LOCAL_DATE_TIME,
                ColumnObject.Mode.CATEGORICAL, DATE, TIME));

        ColumnCategorical<LocalDateTime> dateTimes = assertInstanceOf(ColumnCategorical.class,
                transformed.get(DATE_TIME));
        assertEquals(ColumnTypes.LOCAL_DATE_TIME, dateTimes.getType());
        assertEquals(LocalDateTime.of(2026, 5, 8, 13, 45, 30), dateTimes.get(0));
        assertEquals(LocalDateTime.of(2026, 5, 9, 9, 0), dateTimes.get(1));
        assertEquals(LocalDateTime.of(2026, 5, 8, 13, 45, 30), dateTimes.get(2));
    }

    @Test
    void shouldStillRejectMissingSourceColumn()
    {
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName LAST = ColumnName.of("Last");
        final ColumnName FULL = ColumnName.of("Full");

        ColumnObject.Builder<String> first = ColumnObject.builder(FIRST, ColumnTypes.STRING);
        first.add("Ada");

        TableColumnar table = Tables.newTable(TableName.of("t"), first.build());

        assertThrows(IllegalArgumentException.class, () -> table.apply(TransformConcat.of(FULL, " ", FIRST, LAST)));
    }

    @Test
    void shouldBeAvailableFromLegacyRegistryFactory()
    {
        Transform transform = TransformConcat.of("Full", " ", "First", "Last");

        assertTrue(transform instanceof TransformConcat);
    }
}
