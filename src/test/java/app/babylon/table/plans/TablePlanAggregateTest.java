package app.babylon.table.plans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.io.StreamSource;
import app.babylon.io.StreamSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.aggregation.Aggregate;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.io.RowConsumerCreateTable;
import app.babylon.table.io.RowSourceCsv;
import app.babylon.table.io.TabularRowReader;
import app.babylon.table.io.TabularRowReaderCsv;

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

        TablePlanAggregate plan = new TablePlanAggregate()
                .withColumnType(STATION, app.babylon.table.column.ColumnTypes.STRING)
                .withColumnType(OBSERVATION, double.class);

        assertEquals(ColumnTypes.STRING, plan.getColumnType(STATION));
        assertEquals(ColumnTypes.DOUBLE, plan.getColumnType(OBSERVATION));
        assertEquals(2, plan.getColumnTypes().size());
    }

    @Test
    void executeShouldGroupAndAggregateSingleDoubleColumn()
    {
        final ColumnName COLUMN_1 = ColumnName.of("Column1");
        final ColumnName COLUMN_2 = ColumnName.of("Column2");
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName COUNT = ColumnName.of("Count");
        final ColumnName SUM = ColumnName.of("Sum");
        final ColumnName MIN = ColumnName.of("Min");
        final ColumnName MEAN = ColumnName.of("Mean");
        final ColumnName MAX = ColumnName.of("Max");

        TablePlanAggregate plan = new TablePlanAggregate()
                .withColumnType(STATION, app.babylon.table.column.ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, double.class).withTableName(TableName.of("StationSummary"))
                .withGroupBy(STATION).withAggregate(TEMPERATURE, COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, SUM, Aggregate.SUM).withAggregate(TEMPERATURE, MIN, Aggregate.MIN)
                .withAggregate(TEMPERATURE, MEAN, Aggregate.MEAN).withAggregate(TEMPERATURE, MAX, Aggregate.MAX);

        TabularRowReaderCsv reader = new TabularRowReaderCsv().withSeparator(';')
                .withHeaderStrategy(new app.babylon.table.io.HeaderStrategyNoHeaders(10))
                .withColumnRename(COLUMN_1, STATION).withColumnRename(COLUMN_2, TEMPERATURE);

        TableColumnar table = plan
                .execute(StreamSources.fromString("Amsterdam;10.0\nAmsterdam;14.0\nLondon;7.0\n", "1brc.csv"), reader);

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
        final ColumnName COLUMN_1 = ColumnName.of("Column1");
        final ColumnName COLUMN_2 = ColumnName.of("Column2");
        final ColumnName COLUMN_3 = ColumnName.of("Column3");
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName HUMIDITY = ColumnName.of("Humidity");
        final ColumnName MIN_TEMPERATURE = ColumnName.of("MinTemperature");
        final ColumnName MAX_HUMIDITY = ColumnName.of("MaxHumidity");

        TablePlanAggregate plan = new TablePlanAggregate()
                .withColumnType(STATION, app.babylon.table.column.ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, double.class).withColumnType(HUMIDITY, double.class).withGroupBy(STATION)
                .withAggregate(TEMPERATURE, MIN_TEMPERATURE, Aggregate.MIN)
                .withAggregate(HUMIDITY, MAX_HUMIDITY, Aggregate.MAX);

        TabularRowReaderCsv reader = new TabularRowReaderCsv().withSeparator(';')
                .withHeaderStrategy(new app.babylon.table.io.HeaderStrategyNoHeaders(10))
                .withColumnRename(COLUMN_1, STATION).withColumnRename(COLUMN_2, TEMPERATURE)
                .withColumnRename(COLUMN_3, HUMIDITY);

        TableColumnar table = plan.execute(
                StreamSources.fromString("Amsterdam;10.0;85.0\nAmsterdam;12.0;82.0\nLondon;7.0;91.0\n", "1brc.csv"),
                reader);

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
        final ColumnName COLUMN_1 = ColumnName.of("Column1");
        final ColumnName COLUMN_2 = ColumnName.of("Column2");
        final ColumnName COLUMN_3 = ColumnName.of("Column3");
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName COUNTRY = ColumnName.of("Country");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName COUNT = ColumnName.of("Count");
        final ColumnName MEAN = ColumnName.of("Mean");

        TablePlanAggregate plan = new TablePlanAggregate()
                .withColumnType(STATION, app.babylon.table.column.ColumnTypes.STRING)
                .withColumnType(COUNTRY, app.babylon.table.column.ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, double.class).withTableName(TableName.of("StationCountrySummary"))
                .withGroupBy(STATION, COUNTRY).withAggregate(TEMPERATURE, COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, MEAN, Aggregate.MEAN);

        TabularRowReaderCsv reader = new TabularRowReaderCsv().withSeparator(';')
                .withHeaderStrategy(new app.babylon.table.io.HeaderStrategyNoHeaders(10))
                .withColumnRename(COLUMN_1, STATION).withColumnRename(COLUMN_2, COUNTRY)
                .withColumnRename(COLUMN_3, TEMPERATURE);

        TableColumnar table = plan.execute(StreamSources.fromString(
                "Amsterdam;NL;10.0\nAmsterdam;NL;14.0\nAmsterdam;US;30.0\nLondon;UK;7.0\n", "1brc.csv"), reader);

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
                .withColumnType(STATION, app.babylon.table.column.ColumnTypes.STRING)
                .withColumnType(COUNTRY, app.babylon.table.column.ColumnTypes.STRING)
                .withColumnType(TEMPERATURE, double.class).withColumnType(HUMIDITY, double.class)
                .withGroupBy(STATION, COUNTRY).withAggregate(TEMPERATURE, COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, SUM, Aggregate.SUM).withAggregate(TEMPERATURE, MIN, Aggregate.MIN)
                .withAggregate(TEMPERATURE, MEAN, Aggregate.MEAN).withAggregate(TEMPERATURE, MAX, Aggregate.MAX)
                .withAggregate(HUMIDITY, HUMIDITY_MAX, Aggregate.MAX);

        StreamSource streamingSource = StreamSources.fromString(csv, "summary.csv");
        StreamSource inMemorySource = StreamSources.fromString(csv, "summary.csv");

        TabularRowReaderCsv reader = newReader(STATION, COUNTRY, TEMPERATURE, HUMIDITY);
        TableColumnar streamingResult = plan.execute(streamingSource, reader);
        TabularRowReaderCsv inMemoryReader = newReader(STATION, COUNTRY, TEMPERATURE, HUMIDITY);
        RowConsumerCreateTable rowConsumer = RowConsumerCreateTable.create(TableName.of("ParsedSummary"), null,
                plan.getColumnTypes());
        TabularRowReader.Result readResult = inMemoryReader.read(inMemorySource, rowConsumer);
        assertEquals(TabularRowReader.Status.SUCCESS, readResult.getStatus());
        TableColumnar parsedTable = rowConsumer.build();
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

    private static TabularRowReaderCsv newReader(ColumnName station, ColumnName country, ColumnName temperature,
            ColumnName humidity)
    {
        return new TabularRowReaderCsv().withSeparator(',');
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

        RowSourceCsv rowSource = RowSourceCsv.builder().withStreamSource(StreamSources.fromString(csv, "summary.csv"))
                .withColumnType(TEMPERATURE, app.babylon.table.column.ColumnTypes.DOUBLE).build();
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

}
