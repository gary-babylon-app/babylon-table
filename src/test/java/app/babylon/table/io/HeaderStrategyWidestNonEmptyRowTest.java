package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class HeaderStrategyWidestNonEmptyRowTest
{
    @Test
    void shouldExposeConfiguredScanLimitAndRejectInvalidLimit()
    {
        assertEquals(50, new HeaderStrategyWidestNonEmptyRow().getScanLimit());
        assertEquals(4, new HeaderStrategyWidestNonEmptyRow(4).getScanLimit());
        assertThrows(IllegalArgumentException.class, () -> new HeaderStrategyWidestNonEmptyRow(0));
    }

    @Test
    void detectFoundHeadersShouldChooseWidestNonEmptyRow() throws IOException
    {
        HeaderStrategyWidestNonEmptyRow strategy = new HeaderStrategyWidestNonEmptyRow(5);
        HeaderDetection detection = strategy.detectFoundHeaders(HeaderStrategyTestSupport.stream(
                HeaderStrategyTestSupport.row("n/a", "", ""), HeaderStrategyTestSupport.row("Date", "Symbol", "Price"),
                HeaderStrategyTestSupport.row("2026-01-02", "AAA")), null);

        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Date"), ColumnName.of("Symbol"), ColumnName.of("Price")}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldReturnEmptyHeadersForEmptyInput() throws IOException
    {
        HeaderStrategyWidestNonEmptyRow strategy = new HeaderStrategyWidestNonEmptyRow(2);
        HeaderDetection detection = strategy.detectFoundHeaders(HeaderStrategyTestSupport.stream(), null);

        assertEquals(0, detection.getHeadersFound().length);
    }
}
