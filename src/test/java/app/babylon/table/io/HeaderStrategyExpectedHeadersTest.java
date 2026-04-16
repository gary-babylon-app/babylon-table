package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class HeaderStrategyExpectedHeadersTest
{
    @Test
    void shouldExposeConfiguredHeadersAndRejectInvalidLimit()
    {
        HeaderStrategyExpectedHeaders strategy = new HeaderStrategyExpectedHeaders(3, ColumnName.of("Date"),
                ColumnName.of("Price"));

        Collection<ColumnName> actual = strategy.getExpectedHeaders(new ArrayList<>());

        assertEquals(3, strategy.getScanLimit());
        assertEquals(2, actual.size());
        assertThrows(IllegalArgumentException.class, () -> new HeaderStrategyExpectedHeaders(0));
    }

    @Test
    void detectFoundHeadersShouldMatchExpectedNames() throws IOException
    {
        HeaderStrategyExpectedHeaders strategy = new HeaderStrategyExpectedHeaders(4, ColumnName.of("Date"),
                ColumnName.of("Symbol"), ColumnName.of("Price"));
        HeaderDetection detection = strategy
                .detectFoundHeaders(HeaderStrategyTestSupport.stream(HeaderStrategyTestSupport.row("metadata", "value"),
                        HeaderStrategyTestSupport.row("Date", "Symbol", "Price"),
                        HeaderStrategyTestSupport.row("2026-01-02", "AAA", "10.25")), null);

        assertArrayEquals(new String[]
        {"Date", "Symbol", "Price"}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldThrowForMissingConfigOrNoMatch()
    {
        HeaderStrategyExpectedHeaders unconfigured = new HeaderStrategyExpectedHeaders(3);
        HeaderStrategyExpectedHeaders strategy = new HeaderStrategyExpectedHeaders(2, ColumnName.of("Date"),
                ColumnName.of("Price"));

        assertThrows(RuntimeException.class, () -> unconfigured.detectFoundHeaders(
                HeaderStrategyTestSupport.stream(HeaderStrategyTestSupport.row("Date", "Price")), null));
        assertThrows(RuntimeException.class, () -> strategy.detectFoundHeaders(
                HeaderStrategyTestSupport.stream(HeaderStrategyTestSupport.row("Code", "Amount")), null));
    }
}
