package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class HeaderStrategyNoHeadersTest
{
    @Test
    void shouldExposeConfigurationAndRejectInvalidArguments()
    {
        HeaderStrategyNoHeaders strategy = new HeaderStrategyNoHeaders(3, "Field");

        assertEquals(3, strategy.getScanLimit());
        assertEquals("Field", strategy.getColumnPrefix());
        assertThrows(IllegalArgumentException.class, () -> new HeaderStrategyNoHeaders(0));
        assertThrows(IllegalArgumentException.class, () -> new HeaderStrategyNoHeaders(1, ""));
    }

    @Test
    void detectFoundHeadersShouldCreateSyntheticHeadersFromWidestRow() throws IOException
    {
        HeaderStrategyNoHeaders strategy = new HeaderStrategyNoHeaders(3, "Field");
        HeaderDetection detection = strategy.detectFoundHeaders(HeaderStrategyTestSupport
                .stream(HeaderStrategyTestSupport.row("a", "b"), HeaderStrategyTestSupport.row("c", "d", "e")), null);

        assertTrue(detection.isSyntheticHeaders());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Field1"), ColumnName.of("Field2"), ColumnName.of("Field3")}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldReturnEmptySyntheticHeadersForEmptyInput() throws IOException
    {
        HeaderStrategyNoHeaders strategy = new HeaderStrategyNoHeaders(2);
        HeaderDetection detection = strategy.detectFoundHeaders(HeaderStrategyTestSupport.stream(), null);

        assertTrue(detection.isSyntheticHeaders());
        assertEquals(0, detection.getHeadersFound().length);
    }
}
