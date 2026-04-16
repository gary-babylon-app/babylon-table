package app.babylon.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TableNameTest
{
    @Test
    void shouldExposeOriginalAndCleanNames()
    {
        TableName tableName = TableName.of(" Trade-Book 1 ");

        assertEquals(" Trade-Book 1 ", tableName.getOriginal());
        assertEquals("tradebook1", tableName.getClean());
        assertEquals(" Trade-Book 1 ", tableName.toString());
    }

    @Test
    void shouldCompareAndHashByCleanName()
    {
        TableName first = TableName.of("Trade Book");
        TableName second = TableName.of("trade-book");
        TableName third = TableName.of("Trades");

        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(0, first.compareTo(second));
        assertTrue(first.compareTo(third) < 0);
        assertNotEquals(first, third);
    }
}
