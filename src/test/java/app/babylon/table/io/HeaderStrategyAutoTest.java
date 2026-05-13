package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        String csv = """
                25570,25571,25572
                Date,Symbol,Close
                2026-01-02,AAA,10.25
                """;

        HeaderDetection detection = strategy.detectFoundHeaders(stream(csv), null);

        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Date"), ColumnName.of("Symbol"), ColumnName.of("Close")}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldFallbackToSyntheticHeadersWhenNoHeaderEvidence() throws IOException
    {
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        String csv = """
                AAPL,US,Tech
                MSFT,US,Tech
                GOOG,US,Media
                """;
        RowStreamMarkable rows = stream(csv);

        HeaderDetection detection = strategy.detectFoundHeaders(rows, null);

        assertTrue(detection.isSyntheticHeaders());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Column1"), ColumnName.of("Column2"), ColumnName.of("Column3")}, detection.getHeadersFound());
        rows.reset();
        assertTrue(rows.next());
        assertEquals("AAPL", ((RowBuffer) rows.current()).getString(0));
    }

    @Test
    void detectFoundHeadersShouldFallbackWhenFirstRowContinuesNumericAndDateDomains() throws IOException
    {
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        String csv = """
                2026-01-01,100,USD
                2026-01-02,125,USD
                2026-01-03,150,USD
                """;

        HeaderDetection detection = strategy.detectFoundHeaders(stream(csv), null);

        assertTrue(detection.isSyntheticHeaders());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Column1"), ColumnName.of("Column2"), ColumnName.of("Column3")}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldKeepTextHeavyTransactionHeaders() throws IOException
    {
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        String csv = """
                TransactionId,Description
                T1,buy 100 VEVE for 2000 gbp
                """;

        HeaderDetection detection = strategy.detectFoundHeaders(stream(csv), null);

        assertFalse(detection.isSyntheticHeaders());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("TransactionId"), ColumnName.of("Description")}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldKeepTextHeavyTransformSetHeaders() throws IOException
    {
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        String csv = """
                Type,SetName,StepOrder,Transform,Param1,Param2,Param3,Param4,Param5
                SegmentLedger,Babylon,10,ToDate,SettleDate,SettleDate,,,
                SegmentLedger,Babylon,15,ToDate,TradeDate,TradeDate,,,
                """;

        HeaderDetection detection = strategy.detectFoundHeaders(stream(csv), null);

        assertFalse(detection.isSyntheticHeaders());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Type"), ColumnName.of("SetName"), ColumnName.of("StepOrder"), ColumnName.of("Transform"),
                ColumnName.of("Param1"), ColumnName.of("Param2"), ColumnName.of("Param3"), ColumnName.of("Param4"),
                ColumnName.of("Param5")}, detection.getHeadersFound());
    }

    @Test
    void detectFoundHeadersShouldKeepTextHeavyListingsHeaders() throws IOException
    {
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        String csv = """
                BourseSymbol,Bourse,Symbol,Isin,Issuer,Description,PriceCurrency,SecurityType
                JSE:APACXJ,JSE,APACXJ,ZAE000322483,10X FUND MANAGERS,10XAActively Managed ETF,ZAC,ETF
                JSE:GLODIV,JSE,GLODIV,ZAE000254249,10X FUND MANAGERS,10X GlobalDivTrax,ZAC,ETF
                """;

        HeaderDetection detection = strategy.detectFoundHeaders(stream(csv), null);

        assertFalse(detection.isSyntheticHeaders());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("BourseSymbol"), ColumnName.of("Bourse"), ColumnName.of("Symbol"), ColumnName.of("Isin"),
                ColumnName.of("Issuer"), ColumnName.of("Description"), ColumnName.of("PriceCurrency"),
                ColumnName.of("SecurityType")}, detection.getHeadersFound());
    }

    @Test
    void detectShouldFilterSelectedColumns() throws IOException
    {
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName PRICE = ColumnName.of("Price");
        HeaderStrategyAuto strategy = new HeaderStrategyAuto(5);
        String csv = """
                Trade Date,,Price
                2026-01-02,,10.25
                """;

        HeaderDetection detection = strategy.detect(stream(csv), Set.of(TRADE_DATE, PRICE));

        assertFalse(detection.isSyntheticHeaders());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Trade Date"), ColumnName.of("Price")}, detection.getHeadersFound());
        assertArrayEquals(new ColumnName[]
        {ColumnName.of("Trade Date"), ColumnName.of("Price")}, detection.getSelectedHeaders());
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

    private static RowStreamMarkable stream(String csv)
    {
        String[] lines = csv.strip().split("\\R");
        RowBuffer[] rows = new RowBuffer[lines.length];
        for (int i = 0; i < lines.length; ++i)
        {
            rows[i] = HeaderStrategyTestSupport.row(lines[i].split(",", -1));
        }
        return HeaderStrategyTestSupport.stream(rows);
    }
}
