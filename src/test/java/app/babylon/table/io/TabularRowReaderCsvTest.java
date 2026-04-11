package app.babylon.table.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.io.DataSource;
import app.babylon.io.DataSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.column.ColumnName;

class TabularRowReaderCsvTest
{
    @Test
    void shouldReadCsvIntoTableResult()
    {
        final ColumnName CITY = ColumnName.of("City");
        final ColumnName TEMP = ColumnName.of("Temp");
        TabularRowReaderCsv reader = new TabularRowReaderCsv();

        TableRead read = readTable(reader, "City,Temp\nLondon,12\nParis,15\n", "weather.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(2, table.getRowCount());
        assertEquals("London", table.getString(CITY).get(0));
        assertEquals("15", table.getString(TEMP).get(1));
    }

    @Test
    void shouldKeepExplicitCsvPropertiesOnReader()
    {
        HeaderStrategy expected = new HeaderStrategyNoHeaders(10);
        TabularRowReaderCsv reader = new TabularRowReaderCsv();

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
    void shouldExposeConfiguredResourceMetadata()
    {
        final ColumnName SOURCE = ColumnName.of("Source");
        TableRead read = readTable(new TabularRowReaderCsv(), "City,Temp\nLondon,12\n", "source.csv", SOURCE);

        assertEquals(TabularRowReader.Status.SUCCESS, read.result.getStatus());
        assertEquals("source.csv", read.table.getString(SOURCE).get(0));
    }

    @Test
    void shouldRespectExplicitHeaderRowStrategy()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        String csv = "" + "A,1\n" + "B,2\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withHeaderStrategy(new HeaderStrategyExplicitRow(2));

        TableRead read = readTable(reader, csv, "explicit-header-row.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, table.getColumnCount());
        assertEquals(1, table.getRowCount());
        assertEquals("Coffee", table.getString(DESCRIPTION).get(0));
    }

    @Test
    void shouldReadExpectedHeadersWithinScanLimit()
    {
        final ColumnName DATE = ColumnName.of("Date");
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = "" + "Meta,Value\n" + "Account,123\n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv()
                .withHeaderStrategy(new HeaderStrategyExpectedHeaders(10, DATE, DESCRIPTION, AMOUNT));

        TableRead read = readTable(reader, csv, "expected-header.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, table.getColumnCount());
        assertEquals(1, table.getRowCount());
    }

    @Test
    void shouldGenerateSyntheticColumnsForNoHeadersStrategy()
    {
        final ColumnName COLUMN_1 = ColumnName.of("Column1");
        final ColumnName COLUMN_3 = ColumnName.of("Column3");
        String csv = "" + "john smith,london,uk\n" + "mary jones,paris\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withHeaderStrategy(new HeaderStrategyNoHeaders(10));

        TableRead read = readTable(reader, csv, "no-headers.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());
        assertEquals("john smith", table.getString(COLUMN_1).get(0));
        assertEquals("uk", table.getString(COLUMN_3).get(0));
        assertTrue(table.getString(COLUMN_3).isSet(0));
        assertTrue(!table.getString(COLUMN_3).isSet(1));
    }

    @Test
    void shouldIgnoreSelectedHeadersWhenUsingNoHeadersStrategy()
    {
        final ColumnName DATE = ColumnName.of("Date");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName COLUMN_1 = ColumnName.of("Column1");
        final ColumnName COLUMN_2 = ColumnName.of("Column2");
        final ColumnName COLUMN_3 = ColumnName.of("Column3");
        String csv = "" + "john smith,london,uk\n" + "mary jones,paris,fr\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withHeaderStrategy(new HeaderStrategyNoHeaders(100))
                .withSelectedColumn(DATE).withSelectedColumn(AMOUNT);

        TableRead read = readTable(reader, csv, "no-headers-selected.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());
        assertTrue(table.getString(COLUMN_1) != null);
        assertTrue(table.getString(COLUMN_2) != null);
        assertTrue(table.getString(COLUMN_3) != null);
    }

    @Test
    void shouldDetectHeaderUsingWidestNonEmptyRow()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        String csv = "" + "Account,12345678\n" + "SortCode,12-34-56\n" + "Date,Description,Amount\n"
                + "2026-01-01,Coffee,3.50\n" + "2026-01-02,Salary,1000.00\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv()
                .withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(10));

        TableRead read = readTable(reader, csv, "widest-row.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, table.getColumnCount());
        assertEquals(2, table.getRowCount());
        assertEquals("Coffee", table.getString(DESCRIPTION).get(0));
    }

    @Test
    void shouldRespectWidestNonEmptyRowScanLimit()
    {
        final ColumnName META_0 = ColumnName.of("meta0");
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < 50; ++i)
        {
            csv.append("meta").append(i).append('\n');
        }
        csv.append("Date,Description,Amount\n");
        csv.append("2026-01-01,Coffee,3.50\n");

        TabularRowReaderCsv reader = new TabularRowReaderCsv()
                .withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(50));

        TableRead read = readTable(reader, csv.toString(), "widest-row-limit.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(1, table.getColumnCount());
        assertEquals(51, table.getRowCount());
        assertEquals("meta1", table.getString(META_0).get(0));
    }

    @Test
    void shouldTreatTrimmedNaAsEmptyForHeaderScoring()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        String csv = "" + "  n/a  ,  N/A  \n" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv()
                .withHeaderStrategy(new HeaderStrategyWidestNonEmptyRow(10));

        TableRead read = readTable(reader, csv, "widest-row-na.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, table.getColumnCount());
        assertEquals(1, table.getRowCount());
        assertEquals("Coffee", table.getString(DESCRIPTION).get(0));
    }

    @Test
    void shouldIncludeResourceNameColumnWhenConfigured()
    {
        final ColumnName RESOURCE_NAME = ColumnName.of("ResourceName");
        String csv = "" + "Date,Description,Amount\n" + "2026-01-01,Coffee,3.50\n" + "2026-01-02,Salary,1000.00\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv();

        TableRead read = readTable(reader, csv, "resource-name.csv", RESOURCE_NAME);
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(4, table.getColumnCount());
        assertEquals(2, table.getRowCount());
        assertEquals("resource-name.csv", table.getString(RESOURCE_NAME).get(0));
        assertEquals("resource-name.csv", table.getString(RESOURCE_NAME).get(1));
    }

    @Test
    void shouldPadMalformedDataRowsAfterProjection()
    {
        final ColumnName DATE = ColumnName.of("Date");
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = "" + "Account Name,Everyday Current Account\n" + "Account Number,12345678\n"
                + "Statement Period,2026-01\n" + "Date,Description,Amount\n"
                + "2026-01-01,\"Coffee, corner shop\",-3.50\n"
                + "2026-01-02,\"Salary \"\"Bonus\"\"\r\nAdjustment\",1000.00\n" + "2026-01-03,\"Card Payment\"\n"
                + "2026-01-04,\"Rent\",-500.00\n" + "2026-01-05 Only Date Present\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv();

        TableRead read = readTable(reader, csv, "bank-statement-malformed.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(3, table.getColumnCount());
        assertEquals(5, table.getRowCount());

        assertEquals("2026-01-01", table.getString(DATE).get(0));
        assertEquals("Coffee, corner shop", table.getString(DESCRIPTION).get(0));
        assertEquals("-3.50", table.getString(AMOUNT).get(0));

        assertEquals("2026-01-02", table.getString(DATE).get(1));
        assertEquals("Salary \"Bonus\"\r\nAdjustment", table.getString(DESCRIPTION).get(1));
        assertEquals("1000.00", table.getString(AMOUNT).get(1));

        assertEquals("2026-01-03", table.getString(DATE).get(2));
        assertEquals("Card Payment", table.getString(DESCRIPTION).get(2));
        assertTrue(!table.getString(AMOUNT).isSet(2));

        assertEquals("2026-01-05 Only Date Present", table.getString(DATE).get(4));
        assertTrue(!table.getString(DESCRIPTION).isSet(4));
        assertTrue(!table.getString(AMOUNT).isSet(4));
    }

    @Test
    void shouldPreserveWhitespaceWhenStrippingIsDisabled()
    {
        final ColumnName DATE = ColumnName.of("Date");
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName AMOUNT = ColumnName.of("Amount");
        String csv = "" + "Date,Description,Amount\n" + " 2026-01-01 , Coffee , 3.50 \n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withStripping(false);

        TableRead read = readTable(reader, csv, "no-strip.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(1, table.getRowCount());
        assertEquals(" 2026-01-01 ", table.getString(DATE).get(0));
        assertEquals(" Coffee ", table.getString(DESCRIPTION).get(0));
        assertEquals(" 3.50 ", table.getString(AMOUNT).get(0));
    }

    @Test
    void shouldFilterRowsUsingRowPredicate()
    {
        final ColumnName CITY = ColumnName.of("City");
        String csv = "" + "City,Temp\n" + "London,12\n" + "Paris,15\n" + "Rome,18\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withRowFilter(
                columnNames -> row -> !"Paris".equals(new String(row.chars(), row.start(0), row.length(0))));

        TableRead read = readTable(reader, csv, "row-filter.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(2, table.getRowCount());
        assertEquals("London", table.getString(CITY).get(0));
        assertEquals("Rome", table.getString(CITY).get(1));
    }

    @Test
    void shouldExcludeRowsWhenAnyRequiredColumnIsEmpty()
    {
        final ColumnName CITY = ColumnName.of("City");
        final ColumnName ISIN = ColumnName.of("Isin");
        final ColumnName TYPE = ColumnName.of("Type");
        String csv = "" + "City,Isin,Type\n" + "London,GB0001,Buy\n" + "Paris,,Buy\n" + "Rome,IT0001,\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withRowFilter(RowFilters.excludeEmpty(ISIN, TYPE));

        TableRead read = readTable(reader, csv, "exclude-empty.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(1, table.getRowCount());
        assertEquals("London", table.getString(CITY).get(0));
    }

    @Test
    void shouldIncludeRowsWhenAllColumnPredicatesMatch()
    {
        final ColumnName CITY = ColumnName.of("City");
        final ColumnName TYPE = ColumnName.of("Type");
        final ColumnName ISIN = ColumnName.of("Isin");
        String csv = "" + "City,Type,Isin\n" + "London,Buy,GB0001\n" + "Paris,Buy,\n" + "Rome,Sell,IT0001\n"
                + "Madrid,Buy,ES0001\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withRowFilter(RowFilters
                .include(Map.of(TYPE, value -> "Buy".contentEquals(value), ISIN, value -> value.length() > 0)));

        TableRead read = readTable(reader, csv, "include-map.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(2, table.getRowCount());
        assertEquals("London", table.getString(CITY).get(0));
        assertEquals("Madrid", table.getString(CITY).get(1));
    }

    @Test
    void shouldExcludeRowsWhenAnyColumnPredicateMatches()
    {
        final ColumnName CITY = ColumnName.of("City");
        final ColumnName TYPE = ColumnName.of("Type");
        final ColumnName ISIN = ColumnName.of("Isin");
        String csv = "" + "City,Type,Isin\n" + "London,Buy,GB0001\n" + "Paris,Sell,FR0001\n" + "Rome,Buy,\n"
                + "Madrid,Buy,ES0001\n";
        TabularRowReaderCsv reader = new TabularRowReaderCsv().withRowFilter(RowFilters
                .exclude(Map.of(TYPE, value -> "Sell".contentEquals(value), ISIN, value -> value.length() == 0)));

        TableRead read = readTable(reader, csv, "exclude-map.csv");
        TabularRowReader.Result result = read.result;
        TableColumnar table = read.table;

        assertEquals(TabularRowReader.Status.SUCCESS, result.getStatus());
        assertEquals(2, table.getRowCount());
        assertEquals("London", table.getString(CITY).get(0));
        assertEquals("Madrid", table.getString(CITY).get(1));
    }

    private static TableRead readTable(TabularRowReaderCsv reader, String csv, String resourceName)
    {
        return readTable(reader, csv, resourceName, null);
    }

    private static TableRead readTable(TabularRowReaderCsv reader, String csv, String resourceName,
            ColumnName resourceColumnName)
    {
        DataSource dataSource = DataSources.fromString(csv, resourceName);
        RowConsumerCreateTable rowConsumer = RowConsumerCreateTable.create(null, null, resourceColumnName,
                dataSource.getName(), java.util.Collections.emptyMap());
        TabularRowReader.Result result = reader.read(dataSource, rowConsumer);
        TableColumnar table = result.isSuccessLike() ? rowConsumer.build() : null;
        return new TableRead(result, table);
    }

    private record TableRead(TabularRowReader.Result result, TableColumnar table)
    {
    }
}
