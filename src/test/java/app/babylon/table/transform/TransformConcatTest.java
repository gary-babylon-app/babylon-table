package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
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

        TableColumnar transformed = table.apply(new TransformConcat(FULL, " ", FIRST, LAST));

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

        TableColumnar transformed = table.apply(TransformConcat.of(ACCOUNT_KEY, "|", new ColumnName[]
        {ACCOUNT_TYPE, COUNTRY, null, ACCOUNT_NUMBER}, new String[]
        {null, null, "ACCT", null}));

        ColumnObject<String> accountKey = transformed.getString(ACCOUNT_KEY);
        assertEquals("CASH|GB|ACCT|12345", accountKey.get(0));
        assertEquals("MARGIN|US|ACCT|67890", accountKey.get(1));
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

        assertThrows(IllegalArgumentException.class, () -> table.apply(new TransformConcat(FULL, " ", FIRST, LAST)));
    }

    @Test
    void shouldBeAvailableFromFactory()
    {
        Transform transform = TransformConcat.of("Full", " ", "First", "Last");

        assertTrue(transform instanceof TransformConcat);
    }
}
