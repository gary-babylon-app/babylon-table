package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.ColumnName;

class HeaderStrategyAutoTest
{
    @Test
    void shouldExposeConfiguredScanLimitAndRejectInvalidLimit()
    {
        assertEquals(HeaderStrategy.DEFAULT_SCAN_LIMIT, new HeaderStrategyAuto().getScanLimit());
        assertEquals(7, new HeaderStrategyAuto(7).getScanLimit());
        assertThrows(IllegalArgumentException.class, () -> new HeaderStrategyAuto(0));
    }

    @Test
    void detectFoundHeadersShouldFindBestHeaderRow() throws IOException
    {
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        HeaderDetection detection = strategy.detectFoundHeaders(
                HeaderStrategyTestSupport.stream(HeaderStrategyTestSupport.row("25570", "25571", "25572"),
                        HeaderStrategyTestSupport.row("Date", "Symbol", "Close"),
                        HeaderStrategyTestSupport.row("2026-01-02", "AAA", "10.25")),
                null);

        assertArrayEquals(new String[]
        {"Date", "Symbol", "Close"}, detection.getHeadersFound());
    }

    @Test
    void detectShouldFilterSelectedColumns() throws IOException
    {
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        HeaderDetection detection = strategy.detect(
                HeaderStrategyTestSupport.stream(HeaderStrategyTestSupport.row("Trade Date", "", "Price"),
                        HeaderStrategyTestSupport.row("2026-01-02", "", "10.25")),
                Set.of(ColumnName.of("TradeDate"), ColumnName.of("Price")));

        assertArrayEquals(new String[]
        {"Trade Date", "Column2", "Price"}, detection.getHeadersFound());
        assertArrayEquals(new String[]
        {"Trade Date", "Price"}, detection.getSelectedHeaders());
        assertArrayEquals(new int[]
        {0, 2}, detection.getSelectedPositions());
    }

    @Test
    void headerScoreShouldPreferTextHeaderOverDataRow()
    {
        double headerScore = HeaderStrategyAuto
                .headerScore(HeaderStrategyTestSupport.row("Date", "Symbol", "Price", "Volume"));
        double dataScore = HeaderStrategyAuto
                .headerScore(HeaderStrategyTestSupport.row("2026-03-21", "AAPL", "123.45", "1000"));

        assertTrue(headerScore > dataScore);
    }

    @Test
    void headerScoreShouldTreatNumericEdgeCasesAsNumeric()
    {
        double numericLikeScore = HeaderStrategyAuto
                .headerScore(HeaderStrategyTestSupport.row("+1", "-1", "1.0", "100000", "42"));
        double textualScore = HeaderStrategyAuto
                .headerScore(HeaderStrategyTestSupport.row("+1x", "1e3", ".1", "abc", "x42"));

        assertTrue(textualScore > numericLikeScore);
    }

    @Test
    void detectHeaderRowIndexShouldHandleTypicalLayouts()
    {
        assertEquals(0,
                HeaderStrategyAuto.detectHeaderRowIndex(HeaderStrategyTestSupport.rows(
                        HeaderStrategyTestSupport.row("Date", "Symbol", "Price", "Volume"),
                        HeaderStrategyTestSupport.row("2026-01-02", "AAA", "10.25", "1000"))));

        assertEquals(0,
                HeaderStrategyAuto.detectHeaderRowIndex(
                        HeaderStrategyTestSupport.rows(HeaderStrategyTestSupport.row("Date", "Symbol"),
                                HeaderStrategyTestSupport.row("2026-01-02", "AAA", "10.25", "1000", "2000"))));

        assertEquals(2,
                HeaderStrategyAuto.detectHeaderRowIndex(
                        HeaderStrategyTestSupport.rows(HeaderStrategyTestSupport.row("25569", "25570"),
                                HeaderStrategyTestSupport.row("2026-03-23", "2026-03-24"),
                                HeaderStrategyTestSupport.row("Date", "Symbol", "Price"),
                                HeaderStrategyTestSupport.row("2026-01-02", "AAA", "10.25"))));

        assertEquals(4,
                HeaderStrategyAuto.detectHeaderRowIndex(
                        HeaderStrategyTestSupport.rows(HeaderStrategyTestSupport.row("Account", "12345678"),
                                HeaderStrategyTestSupport.row("SortCode", "12-34-56"),
                                HeaderStrategyTestSupport.row("Period", "2026-01-01 to 2026-01-31"),
                                HeaderStrategyTestSupport.row("Currency", "GBP"),
                                HeaderStrategyTestSupport.row("Date", "Description", "Debit", "Credit", "Balance"),
                                HeaderStrategyTestSupport.row("2026-01-02", "Card Purchase", "10.00", "", "990.00"))));
    }
}
