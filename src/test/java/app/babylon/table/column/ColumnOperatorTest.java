package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ColumnOperatorTest
{
    @Test
    void shouldParseOperatorAliases()
    {
        assertEquals(Column.Operator.EQUAL, Column.Operator.parse("="));
        assertEquals(Column.Operator.EQUAL, Column.Operator.parse("=="));
        assertEquals(Column.Operator.EQUAL, Column.Operator.parse("is"));
        assertEquals(Column.Operator.NOT_EQUAL, Column.Operator.parse("<>"));
        assertEquals(Column.Operator.NOT_EQUAL, Column.Operator.parse("!="));
    }

    @Test
    void shouldNormaliseOperatorOnlyAfterExactAliasesFail()
    {
        assertEquals(Column.Operator.IN, Column.Operator.parse(" IN "));
        assertEquals(Column.Operator.NOT_IN, Column.Operator.parse(" NIN "));
    }

    @Test
    void shouldPreferBusinessReadableOperatorText()
    {
        assertEquals("=", Column.Operator.EQUAL.text());
        assertEquals("<>", Column.Operator.NOT_EQUAL.text());
        assertEquals("in", Column.Operator.IN.text());
        assertEquals("nin", Column.Operator.NOT_IN.text());
    }
}
