package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class HeaderStrategyExplicitRowTest
{
    @Test
    void shouldExposeConfiguredRowAndRejectNegativeIndex()
    {
        HeaderStrategyExplicitRow strategy = new HeaderStrategyExplicitRow(1);

        assertEquals(1, strategy.getHeaderRowIndex());
        assertThrows(IllegalArgumentException.class, () -> new HeaderStrategyExplicitRow(-1));
    }

    @Test
    void detectFoundHeadersShouldUseRequestedRow() throws IOException
    {
        HeaderStrategyExplicitRow strategy = new HeaderStrategyExplicitRow(1);
        HeaderDetection detection = strategy.detectFoundHeaders(HeaderStrategyTestSupport.stream(
                HeaderStrategyTestSupport.row("preamble", "value"), HeaderStrategyTestSupport.row("Date", "Price"),
                HeaderStrategyTestSupport.row("2026-01-02", "10.25")), null);

        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Date"), ColumnName.of("Price")}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldThrowWhenRowDoesNotExist()
    {
        HeaderStrategyExplicitRow strategy = new HeaderStrategyExplicitRow(3);

        assertThrows(RuntimeException.class, () -> strategy.detectFoundHeaders(
                HeaderStrategyTestSupport.stream(HeaderStrategyTestSupport.row("Date", "Price")), null));
    }
}
