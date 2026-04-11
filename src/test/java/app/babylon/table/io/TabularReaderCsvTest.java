package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.io.DataSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.column.ColumnName;

class TabularReaderCsvTest
{
    @Test
    void shouldReadCsvIntoTableResult()
    {
        ColumnName city = ColumnName.of("City");
        ColumnName temp = ColumnName.of("Temp");
        TabularReaderCsv reader = new TabularReaderCsv();

        TabularReader.Result result = reader
                .read(DataSources.fromString("City,Temp\nLondon,12\nParis,15\n", "weather.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertTrue(result.hasTable());
        assertEquals(2, result.getTable().getRowCount());
        assertEquals("London", result.getTable().getString(city).get(0));
        assertEquals("15", result.getTable().getString(temp).get(1));
    }

    @Test
    void shouldKeepExplicitCsvPropertiesOnReader()
    {
        HeaderStrategy expected = new HeaderStrategyNoHeaders(10);
        TabularReaderCsv reader = new TabularReaderCsv();

        reader.withHeaderStrategy(expected).withSeparator(';').withStripping(false).withFixedWidths(new int[]
        {1, 2}).withCharset(StandardCharsets.UTF_8).withAutoDetectEncoding(false);

        assertTrue(!reader.isStripping());
        assertEquals(';', reader.getSeparator());
        assertEquals(2, reader.getFixedWidths().length);
        assertEquals(StandardCharsets.UTF_8, reader.getCharset());
        assertTrue(!reader.isAutoDetectEncoding());
        assertEquals(expected, reader.getHeaderStrategy());
    }

    @Test
    void shouldExposeConfiguredTableMetadata()
    {
        TabularReaderCsv reader = new TabularReaderCsv();
        TableName tableName = TableName.of("Weather");
        ColumnName resourceName = ColumnName.of("Source");

        reader.withTableName(tableName).withIncludeResourceName(resourceName);

        assertEquals(tableName, reader.getTableName());
        assertEquals(resourceName, reader.getResourceName());
    }

    @Test
    void shouldRespectExplicitHeaderRowStrategy()
    {
        String csv = "" + "A,1\n" + "B,2\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";
        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(new HeaderStrategyExplicitRow(2));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "explicit-header-row.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getTable().getColumnCount());
        assertEquals(1, result.getTable().getRowCount());
        assertEquals("Coffee", result.getTable().getString(ColumnName.of("Description")).get(0));
    }

    @Test
    void shouldReadExpectedHeadersWithinScanLimit()
    {
        String csv = "" + "Meta,Value\n" + "Account,123\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";
        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(new HeaderStrategyExpectedHeaders(10,
                ColumnName.of("Date"), ColumnName.of("Description"), ColumnName.of("Amount")));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "expected-header.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getTable().getColumnCount());
        assertEquals(1, result.getTable().getRowCount());
    }

    @Test
    void shouldGenerateSyntheticColumnsForNoHeadersStrategy()
    {
        String csv = "" + "john smith,london,uk\n" + "mary jones,paris\n";
        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(new HeaderStrategyNoHeaders(10));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "no-headers.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getTable().getColumnCount());
        assertEquals(2, result.getTable().getRowCount());
        assertEquals("john smith", result.getTable().getString(ColumnName.of("Column1")).get(0));
        assertEquals("uk", result.getTable().getString(ColumnName.of("Column3")).get(0));
        assertTrue(result.getTable().getString(ColumnName.of("Column3")).isSet(0));
        assertTrue(!result.getTable().getString(ColumnName.of("Column3")).isSet(1));
    }

    @Test
    void shouldIgnoreSelectedHeadersWhenUsingNoHeadersStrategy()
    {
        String csv = "" + "john smith,london,uk\n" + "mary jones,paris,fr\n";
        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(new HeaderStrategyNoHeaders(100))
                .withSelectedColumn(ColumnName.of("Date")).withSelectedColumn(ColumnName.of("Amount"));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "no-headers-selected.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getTable().getColumnCount());
        assertEquals(2, result.getTable().getRowCount());
        assertTrue(result.getTable().getString(ColumnName.of("Column1")) != null);
        assertTrue(result.getTable().getString(ColumnName.of("Column2")) != null);
        assertTrue(result.getTable().getString(ColumnName.of("Column3")) != null);
    }

    @Test
    void shouldDetectHeaderUsingWidestNonEmptyRow()
    {
        String csv = "" + "Account,12345678\n" + "SortCode,12-34-56\n" + "Date,Description,Amount\n"
                + "2026-01-01,Coffee,3.50\n" + "2026-01-02,Salary,1000.00\n";
        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(10));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "widest-row.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getTable().getColumnCount());
        assertEquals(2, result.getTable().getRowCount());
        assertEquals("Coffee", result.getTable().getString(ColumnName.of("Description")).get(0));
    }

    @Test
    void shouldRespectWidestNonEmptyRowScanLimit()
    {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < 50; ++i)
        {
            csv.append("meta").append(i).append('\n');
        }
        csv.append("Date,Description,Amount\n");
        csv.append("2026-01-01,Coffee,3.50\n");

        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(50));

        TabularReader.Result result = reader.read(DataSources.fromString(csv.toString(), "widest-row-limit.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getTable().getColumnCount());
        assertEquals(51, result.getTable().getRowCount());
        assertEquals("meta1", result.getTable().getString(ColumnName.of("meta0")).get(0));
    }

    @Test
    void shouldTreatTrimmedNaAsEmptyForHeaderScoring()
    {
        String csv = "" + "  n/a  ,  N/A  \n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";
        TabularReaderCsv reader = new TabularReaderCsv().withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(10));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "widest-row-na.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getTable().getColumnCount());
        assertEquals(1, result.getTable().getRowCount());
        assertEquals("Coffee", result.getTable().getString(ColumnName.of("Description")).get(0));
    }

    @Test
    void shouldIncludeResourceNameColumnWhenConfigured()
    {
        String csv = "" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n" + "2026-01-02,Salary,1000.00\n";
        TabularReaderCsv reader = new TabularReaderCsv().withIncludeResourceName(ColumnName.of("ResourceName"));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "resource-name.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(4, result.getTable().getColumnCount());
        assertEquals(2, result.getTable().getRowCount());
        assertEquals("resource-name.csv", result.getTable().getString(ColumnName.of("ResourceName")).get(0));
        assertEquals("resource-name.csv", result.getTable().getString(ColumnName.of("ResourceName")).get(1));
    }

    @Test
    void shouldPadMalformedDataRowsAfterProjection()
    {
        String csv = "" + "Account Name,Everyday Current Account\n" + "Account Number,12345678\n"
                + "Statement Period,2026-01\n" + "Date,Description,Amount\n"
                + "2026-01-01,\"Coffee, corner shop\",-3.50\n"
                + "2026-01-02,\"Salary \"\"Bonus\"\"\r\nAdjustment\",1000.00\n" + "2026-01-03,\"Card Payment\"\n"
                + "2026-01-04,\"Rent\",-500.00\n" + "2026-01-05 Only Date Present\n";
        TabularReaderCsv reader = new TabularReaderCsv();

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "bank-statement-malformed.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, result.getTable().getColumnCount());
        assertEquals(5, result.getTable().getRowCount());

        assertEquals("2026-01-01", result.getTable().getString(ColumnName.of("Date")).get(0));
        assertEquals("Coffee, corner shop", result.getTable().getString(ColumnName.of("Description")).get(0));
        assertEquals("-3.50", result.getTable().getString(ColumnName.of("Amount")).get(0));

        assertEquals("2026-01-02", result.getTable().getString(ColumnName.of("Date")).get(1));
        assertEquals("Salary \"Bonus\"\r\nAdjustment",
                result.getTable().getString(ColumnName.of("Description")).get(1));
        assertEquals("1000.00", result.getTable().getString(ColumnName.of("Amount")).get(1));

        assertEquals("2026-01-03", result.getTable().getString(ColumnName.of("Date")).get(2));
        assertEquals("Card Payment", result.getTable().getString(ColumnName.of("Description")).get(2));
        assertTrue(!result.getTable().getString(ColumnName.of("Amount")).isSet(2));

        assertEquals("2026-01-05 Only Date Present", result.getTable().getString(ColumnName.of("Date")).get(4));
        assertTrue(!result.getTable().getString(ColumnName.of("Description")).isSet(4));
        assertTrue(!result.getTable().getString(ColumnName.of("Amount")).isSet(4));
    }

    @Test
    void shouldPreserveWhitespaceWhenStrippingIsDisabled()
    {
        String csv = "" + "Date,Description,Amount\n" + " 2026-01-01 , Coffee , 3.50 \n";
        TabularReaderCsv reader = new TabularReaderCsv().withStripping(false);

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "no-strip.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getTable().getRowCount());
        assertEquals(" 2026-01-01 ", result.getTable().getString(ColumnName.of("Date")).get(0));
        assertEquals(" Coffee ", result.getTable().getString(ColumnName.of("Description")).get(0));
        assertEquals(" 3.50 ", result.getTable().getString(ColumnName.of("Amount")).get(0));
    }

    @Test
    void shouldFilterRowsUsingRowPredicate()
    {
        String csv = "" + "City,Temp\n" + "London,12\n" + "Paris,15\n" + "Rome,18\n";
        TabularReaderCsv reader = new TabularReaderCsv().withRowFilter(
                columnNames -> row -> !"Paris".equals(new String(row.chars(), row.start(0), row.length(0))));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "row-filter.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getTable().getRowCount());
        assertEquals("London", result.getTable().getString(ColumnName.of("City")).get(0));
        assertEquals("Rome", result.getTable().getString(ColumnName.of("City")).get(1));
    }

    @Test
    void shouldExcludeRowsWhenAnyRequiredColumnIsEmpty()
    {
        String csv = "" + "City,Isin,Type\n" + "London,GB0001,Buy\n" + "Paris,,Buy\n" + "Rome,IT0001,\n";
        TabularReaderCsv reader = new TabularReaderCsv()
                .withRowFilter(RowFilters.excludeEmpty(ColumnName.of("Isin"), ColumnName.of("Type")));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "exclude-empty.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getTable().getRowCount());
        assertEquals("London", result.getTable().getString(ColumnName.of("City")).get(0));
    }

    @Test
    void shouldIncludeRowsWhenAllColumnPredicatesMatch()
    {
        String csv = "" + "City,Type,Isin\n" + "London,Buy,GB0001\n" + "Paris,Buy,\n" + "Rome,Sell,IT0001\n"
                + "Madrid,Buy,ES0001\n";
        TabularReaderCsv reader = new TabularReaderCsv().withRowFilter(RowFilters.include(Map.of(ColumnName.of("Type"),
                value -> "Buy".contentEquals(value), ColumnName.of("Isin"), value -> value.length() > 0)));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "include-map.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getTable().getRowCount());
        assertEquals("London", result.getTable().getString(ColumnName.of("City")).get(0));
        assertEquals("Madrid", result.getTable().getString(ColumnName.of("City")).get(1));
    }

    @Test
    void shouldExcludeRowsWhenAnyColumnPredicateMatches()
    {
        String csv = "" + "City,Type,Isin\n" + "London,Buy,GB0001\n" + "Paris,Sell,FR0001\n" + "Rome,Buy,\n"
                + "Madrid,Buy,ES0001\n";
        TabularReaderCsv reader = new TabularReaderCsv().withRowFilter(RowFilters.exclude(Map.of(ColumnName.of("Type"),
                value -> "Sell".contentEquals(value), ColumnName.of("Isin"), value -> value.length() == 0)));

        TabularReader.Result result = reader.read(DataSources.fromString(csv, "exclude-map.csv"));

        assertEquals(TabularReader.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getTable().getRowCount());
        assertEquals("London", result.getTable().getString(ColumnName.of("City")).get(0));
        assertEquals("Madrid", result.getTable().getString(ColumnName.of("City")).get(1));
    }
}
