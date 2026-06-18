package app.babylon.table.plans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.io.DataResources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.aggregation.Aggregate;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.io.ReadOptionsCsv;
import app.babylon.table.io.RowSource;
import app.babylon.table.io.RowSources;

class TablePlanAggregateTest
{
    @Test
    void shouldCaptureOutputNameGroupBysAndAggregatesInOrder()
    {
        final ColumnName STATION = ColumnName.of("station");
        final ColumnName OBSERVATION = ColumnName.of("observation");
        final ColumnName MEAN_OBSERVATION = ColumnName.of("mean_observation");

        TablePlanAggregate plan = new TablePlanAggregate().withTableName(TableName.of("summary")).withGroupBy(STATION)
                .withAggregate(OBSERVATION, MEAN_OBSERVATION, Aggregate.MEAN);

        assertEquals(TableName.of("summary"), plan.getTableName());
        assertEquals(1, plan.getGroupByColumns().size());
        assertEquals(STATION, plan.getGroupByColumns().get(0));
        assertEquals(1, plan.getAggregateSpecs().size());
        assertEquals(OBSERVATION, plan.getAggregateSpecs().get(0).sourceColumnName());
        assertEquals(MEAN_OBSERVATION, plan.getAggregateSpecs().get(0).outputColumnName());
        assertEquals(Aggregate.MEAN, plan.getAggregateSpecs().get(0).aggregate());
    }

    @Test
    void shouldCapturePlanLevelColumnTypes()
    {
        final ColumnName STATION = ColumnName.of("station");
        final ColumnName OBSERVATION = ColumnName.of("observation");

        TablePlanAggregate plan = new TablePlanAggregate().withColumnType(STATION, ColumnTypes.STRING)
                .withColumnType(OBSERVATION, ColumnTypes.DOUBLE);

        assertEquals(ColumnTypes.STRING, plan.getColumnType(STATION));
        assertEquals(ColumnTypes.DOUBLE, plan.getColumnType(OBSERVATION));
        assertEquals(2, plan.getColumnTypes().size());
    }

    @Test
    void executeShouldGroupAndAggregateSingleDoubleColumn()
    {
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName COUNT = ColumnName.of("Count");
        final ColumnName SUM = ColumnName.of("Sum");
        final ColumnName MIN = ColumnName.of("Min");
        final ColumnName MEAN = ColumnName.of("Mean");
        final ColumnName MAX = ColumnName.of("Max");

        TablePlanAggregate plan = new TablePlanAggregate().withColumnType(STATION, ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, ColumnTypes.DOUBLE).withTableName(TableName.of("StationSummary"))
                .withGroupBy(STATION).withAggregate(TEMPERATURE, COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, SUM, Aggregate.SUM).withAggregate(TEMPERATURE, MIN, Aggregate.MIN)
                .withAggregate(TEMPERATURE, MEAN, Aggregate.MEAN).withAggregate(TEMPERATURE, MAX, Aggregate.MAX);

        String csv = """
                Station;Temperature
                Amsterdam;10.0
                Amsterdam;14.0
                London;7.0
                """;
        TableColumnar table = plan.execute(semiColonRowSource(csv));

        assertEquals(TableName.of("StationSummary"), table.getName());
        assertEquals(2, table.getRowCount());
        assertEquals("Amsterdam", table.getString(STATION).get(0));
        assertEquals(2L, table.getLong(COUNT).get(0));
        assertEquals(24.0d, table.getDouble(SUM).get(0));
        assertEquals(10.0d, table.getDouble(MIN).get(0));
        assertEquals(12.0d, table.getDouble(MEAN).get(0));
        assertEquals(14.0d, table.getDouble(MAX).get(0));
        assertEquals("London", table.getString(STATION).get(1));
        assertEquals(1L, table.getLong(COUNT).get(1));
        assertEquals(7.0d, table.getDouble(SUM).get(1));
        assertEquals(7.0d, table.getDouble(MIN).get(1));
        assertEquals(7.0d, table.getDouble(MEAN).get(1));
        assertEquals(7.0d, table.getDouble(MAX).get(1));
    }

    @Test
    void executeShouldSupportMultipleAggregateSourceColumns()
    {
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName HUMIDITY = ColumnName.of("Humidity");
        final ColumnName MIN_TEMPERATURE = ColumnName.of("MinTemperature");
        final ColumnName MAX_HUMIDITY = ColumnName.of("MaxHumidity");

        TablePlanAggregate plan = new TablePlanAggregate().withColumnType(STATION, ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, ColumnTypes.DOUBLE).withColumnType(HUMIDITY, ColumnTypes.DOUBLE)
                .withGroupBy(STATION).withAggregate(TEMPERATURE, MIN_TEMPERATURE, Aggregate.MIN)
                .withAggregate(HUMIDITY, MAX_HUMIDITY, Aggregate.MAX);

        String csv = """
                Station;Temperature;Humidity
                Amsterdam;10.0;85.0
                Amsterdam;12.0;82.0
                London;7.0;91.0
                """;
        TableColumnar table = plan.execute(semiColonRowSource(csv));

        assertEquals(2, table.getRowCount());
        assertEquals("Amsterdam", table.getString(STATION).get(0));
        assertEquals(10.0d, table.getDouble(MIN_TEMPERATURE).get(0));
        assertEquals(85.0d, table.getDouble(MAX_HUMIDITY).get(0));
        assertEquals("London", table.getString(STATION).get(1));
        assertEquals(7.0d, table.getDouble(MIN_TEMPERATURE).get(1));
        assertEquals(91.0d, table.getDouble(MAX_HUMIDITY).get(1));
    }

    @Test
    void executeShouldGroupByMultipleColumns()
    {
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName COUNTRY = ColumnName.of("Country");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName COUNT = ColumnName.of("Count");
        final ColumnName MEAN = ColumnName.of("Mean");

        TablePlanAggregate plan = new TablePlanAggregate().withColumnType(STATION, ColumnTypes.STRING)
                .withColumnType(COUNTRY, ColumnTypes.STRING).withColumnType(TEMPERATURE, ColumnTypes.DOUBLE)
                .withTableName(TableName.of("StationCountrySummary")).withGroupBy(STATION, COUNTRY)
                .withAggregate(TEMPERATURE, COUNT, Aggregate.COUNT).withAggregate(TEMPERATURE, MEAN, Aggregate.MEAN);

        String csv = """
                Station;Country;Temperature
                Amsterdam;NL;10.0
                Amsterdam;NL;14.0
                Amsterdam;US;30.0
                London;UK;7.0
                """;
        TableColumnar table = plan.execute(semiColonRowSource(csv));

        assertEquals(3, table.getRowCount());
        assertEquals("Amsterdam", table.getString(STATION).get(0));
        assertEquals("NL", table.getString(COUNTRY).get(0));
        assertEquals(2L, table.getLong(COUNT).get(0));
        assertEquals(12.0d, table.getDouble(MEAN).get(0));
        assertEquals("Amsterdam", table.getString(STATION).get(1));
        assertEquals("US", table.getString(COUNTRY).get(1));
        assertEquals(1L, table.getLong(COUNT).get(1));
        assertEquals(30.0d, table.getDouble(MEAN).get(1));
        assertEquals("London", table.getString(STATION).get(2));
        assertEquals("UK", table.getString(COUNTRY).get(2));
        assertEquals(1L, table.getLong(COUNT).get(2));
        assertEquals(7.0d, table.getDouble(MEAN).get(2));
    }

    @Test
    void streamingAndInMemoryExecutionShouldProduceMatchingSummaries()
    {
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName COUNTRY = ColumnName.of("Country");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName HUMIDITY = ColumnName.of("Humidity");
        final ColumnName COUNT = ColumnName.of("Count");
        final ColumnName SUM = ColumnName.of("Sum");
        final ColumnName MIN = ColumnName.of("Min");
        final ColumnName MEAN = ColumnName.of("Mean");
        final ColumnName MAX = ColumnName.of("Max");
        final ColumnName HUMIDITY_MAX = ColumnName.of("HumidityMax");
        final String csv = """
                Station,Country,Temperature,Humidity
                Amsterdam,NL,10.0,85.0
                Amsterdam,NL,14.0,82.0
                Amsterdam,US,30.0,65.0
                London,UK,7.0,91.0
                London,UK,9.0,87.0
                Paris,FR,12.5,80.0
                Paris,FR,11.5,83.0
                """;

        TablePlanAggregate plan = new TablePlanAggregate().withTableName(TableName.of("StationCountrySummary"))
                .withColumnType(STATION, ColumnTypes.STRING).withColumnType(COUNTRY, ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, ColumnTypes.DOUBLE).withColumnType(HUMIDITY, ColumnTypes.DOUBLE)
                .withGroupBy(STATION, COUNTRY).withAggregate(TEMPERATURE, COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, SUM, Aggregate.SUM).withAggregate(TEMPERATURE, MIN, Aggregate.MIN)
                .withAggregate(TEMPERATURE, MEAN, Aggregate.MEAN).withAggregate(TEMPERATURE, MAX, Aggregate.MAX)
                .withAggregate(HUMIDITY, HUMIDITY_MAX, Aggregate.MAX);

        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withSeparator(',').build();
        TableColumnar streamingResult = plan
                .execute(RowSources.create(csvFormat, DataResources.fromString(csv, "summary.csv")));
        TableColumnar parsedTable = new TablePlanRead().withTableName(TableName.of("ParsedSummary"))
                .withColumnType(STATION, ColumnTypes.STRING).withColumnType(COUNTRY, ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, ColumnTypes.DOUBLE).withColumnType(HUMIDITY, ColumnTypes.DOUBLE)
                .execute(RowSources.create(csvFormat, DataResources.fromString(csv, "summary.csv")));
        TableColumnar inMemoryResult = plan.execute(parsedTable);

        assertEquals(streamingResult.getName(), inMemoryResult.getName());
        assertEquals(streamingResult.getRowCount(), inMemoryResult.getRowCount());

        Map<String, SummaryRow> streamingRows = toSummaryRows(streamingResult, STATION, COUNTRY, COUNT, SUM, MIN, MEAN,
                MAX, HUMIDITY_MAX);
        Map<String, SummaryRow> inMemoryRows = toSummaryRows(inMemoryResult, STATION, COUNTRY, COUNT, SUM, MIN, MEAN,
                MAX, HUMIDITY_MAX);

        assertEquals(streamingRows.keySet(), inMemoryRows.keySet());
        for (Map.Entry<String, SummaryRow> entry : streamingRows.entrySet())
        {
            SummaryRow expected = entry.getValue();
            SummaryRow actual = inMemoryRows.get(entry.getKey());
            assertEquals(expected.count, actual.count);
            assertEquals(expected.sum, actual.sum, 1.0e-9);
            assertEquals(expected.min, actual.min, 1.0e-9);
            assertEquals(expected.mean, actual.mean, 1.0e-9);
            assertEquals(expected.max, actual.max, 1.0e-9);
            assertEquals(expected.humidityMax, actual.humidityMax, 1.0e-9);
        }
    }

    private static RowSource semiColonRowSource(String csv)
    {
        ReadOptionsCsv csvFormat = ReadOptionsCsv.builder().withSeparator(';').build();
        return RowSources.create(csvFormat, DataResources.fromString(csv.stripIndent(), "1brc.csv"));
    }

    private static Map<String, SummaryRow> toSummaryRows(TableColumnar table, ColumnName station, ColumnName country,
            ColumnName count, ColumnName sum, ColumnName min, ColumnName mean, ColumnName max, ColumnName humidityMax)
    {
        Map<String, SummaryRow> rows = new LinkedHashMap<>();
        for (int i = 0; i < table.getRowCount(); ++i)
        {
            String key = table.getString(station).get(i) + "|" + table.getString(country).get(i);
            rows.put(key,
                    new SummaryRow(table.getLong(count).get(i), table.getDouble(sum).get(i),
                            table.getDouble(min).get(i), table.getDouble(mean).get(i), table.getDouble(max).get(i),
                            table.getDouble(humidityMax).get(i)));
        }
        return rows;
    }

    @Test
    void executeTableColumnarShouldAggregateMinAndMaxFromPrimitiveAndObjectColumns()
    {
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName TRADE_DATE = ColumnName.of("TradeDate");
        final ColumnName SCORE = ColumnName.of("Score");
        final ColumnName ACTIVE = ColumnName.of("Active");
        final ColumnName COUNT = ColumnName.of("Count");
        final ColumnName FIRST_TRADE_DATE = ColumnName.of("FirstTradeDate");
        final ColumnName LAST_TRADE_DATE = ColumnName.of("LastTradeDate");
        final ColumnName MIN_SCORE = ColumnName.of("MinScore");
        final ColumnName MAX_SCORE = ColumnName.of("MaxScore");
        final ColumnName MIN_ACTIVE = ColumnName.of("MinActive");
        final ColumnName MAX_ACTIVE = ColumnName.of("MaxActive");

        TableColumnar source = typedMinMaxSource(STATION, TRADE_DATE, SCORE, ACTIVE);
        TablePlanAggregate plan = new TablePlanAggregate().withTableName(TableName.of("Summary")).withGroupBy(STATION)
                .withAggregate(STATION, COUNT, Aggregate.COUNT)
                .withAggregate(TRADE_DATE, FIRST_TRADE_DATE, Aggregate.MIN)
                .withAggregate(TRADE_DATE, LAST_TRADE_DATE, Aggregate.MAX)
                .withAggregate(SCORE, MIN_SCORE, Aggregate.MIN).withAggregate(SCORE, MAX_SCORE, Aggregate.MAX)
                .withAggregate(ACTIVE, MIN_ACTIVE, Aggregate.MIN).withAggregate(ACTIVE, MAX_ACTIVE, Aggregate.MAX);

        TableColumnar summary = plan.execute(source);

        assertEquals(2, summary.getRowCount());
        assertEquals("Amsterdam", summary.getString(STATION).get(0));
        assertEquals(2L, summary.getLong(COUNT).get(0));
        assertEquals(LocalDate.of(2026, 1, 2), summary.getObject(FIRST_TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 1, 5), summary.getObject(LAST_TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(4, summary.getInt(MIN_SCORE).get(0));
        assertEquals(7, summary.getInt(MAX_SCORE).get(0));
        assertFalse(summary.getBoolean(MIN_ACTIVE).get(0));
        assertTrue(summary.getBoolean(MAX_ACTIVE).get(0));

        assertEquals("London", summary.getString(STATION).get(1));
        assertEquals(1L, summary.getLong(COUNT).get(1));
        assertEquals(LocalDate.of(2026, 2, 3), summary.getObject(FIRST_TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 2, 3), summary.getObject(LAST_TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(9, summary.getInt(MIN_SCORE).get(1));
        assertEquals(9, summary.getInt(MAX_SCORE).get(1));
        assertTrue(summary.getBoolean(MIN_ACTIVE).get(1));
        assertTrue(summary.getBoolean(MAX_ACTIVE).get(1));
    }

    @Test
    void executeShouldAggregateFromRowSource()
    {
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName COUNT = ColumnName.of("Count");
        final ColumnName MEAN = ColumnName.of("Mean");
        final String csv = """
                Station,Temperature
                Amsterdam,10.0
                Amsterdam,14.0
                London,7.0
                """;

        RowSource rowSource = RowSources.create(ReadOptionsCsv.standard(),
                DataResources.fromString(csv, "summary.csv"));
        TablePlanAggregate plan = new TablePlanAggregate().withTableName(TableName.of("StationSummary"))
                .withGroupBy(STATION).withAggregate(TEMPERATURE, COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, MEAN, Aggregate.MEAN);

        TableColumnar table = plan.execute(rowSource);

        assertEquals(TableName.of("StationSummary"), table.getName());
        assertEquals(2, table.getRowCount());
        assertEquals("Amsterdam", table.getString(STATION).get(0));
        assertEquals(2L, table.getLong(COUNT).get(0));
        assertEquals(12.0d, table.getDouble(MEAN).get(0));
        assertEquals("London", table.getString(STATION).get(1));
        assertEquals(1L, table.getLong(COUNT).get(1));
        assertEquals(7.0d, table.getDouble(MEAN).get(1));
    }

    private static record SummaryRow(long count, double sum, double min, double mean, double max, double humidityMax)
    {
    }

    private static TableColumnar typedMinMaxSource(ColumnName stationName, ColumnName tradeDateName,
            ColumnName scoreName, ColumnName activeName)
    {
        ColumnObject.Builder<String> stations = ColumnObject.builder(stationName);
        stations.add("Amsterdam");
        stations.add("Amsterdam");
        stations.add("London");

        ColumnObject.Builder<LocalDate> tradeDates = ColumnObject.builder(tradeDateName, ColumnTypes.LOCALDATE);
        tradeDates.add(LocalDate.of(2026, 1, 5));
        tradeDates.add(LocalDate.of(2026, 1, 2));
        tradeDates.add(LocalDate.of(2026, 2, 3));

        ColumnInt.Builder scores = ColumnInt.builder(scoreName);
        scores.add(7);
        scores.add(4);
        scores.add(9);

        ColumnBoolean.Builder active = ColumnBoolean.builder(activeName);
        active.add(true);
        active.add(false);
        active.add(true);

        return Tables.newTable(TableName.of("TypedMinMaxSource"), stations.build(), tradeDates.build(), scores.build(),
                active.build());
    }

}
