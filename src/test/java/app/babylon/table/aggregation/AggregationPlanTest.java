package app.babylon.table.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import app.babylon.io.DataSource;
import app.babylon.io.DataSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.io.Csv;
import app.babylon.table.io.HeaderStrategyAuto;
import app.babylon.table.io.RowConsumerTableCreator;

class AggregationPlanTest
{
    @Test
    void shouldCaptureOutputNameGroupBysAndAggregatesInOrder()
    {
        final ColumnName STATION = ColumnName.of("station");
        final ColumnName OBSERVATION = ColumnName.of("observation");
        final ColumnName MEAN_OBSERVATION = ColumnName.of("mean_observation");

        AggregationPlan plan = new AggregationPlan().withOutputTableName(TableName.of("summary")).withGroupBy(STATION)
                .withAggregate(OBSERVATION, MEAN_OBSERVATION, Aggregate.MEAN);

        assertEquals(TableName.of("summary"), plan.getOutputTableName());
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

        AggregationPlan plan = new AggregationPlan().withColumnType(STATION, String.class).withColumnType(OBSERVATION,
                double.class);

        assertEquals(Column.Type.of(String.class), plan.getColumnType(STATION));
        assertEquals(Column.Type.of(double.class), plan.getColumnType(OBSERVATION));
        assertEquals(2, plan.getColumnTypes().size());
    }

    @Test
    void executeShouldGroupAndAggregateSingleDoubleColumn()
    {
        final ColumnName COLUMN_1 = ColumnName.of("Column1");
        final ColumnName COLUMN_2 = ColumnName.of("Column2");
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName TEMPERATURE_COUNT = ColumnName.of("Count");
        final ColumnName TEMPERATURE_SUM = ColumnName.of("Sum");
        final ColumnName TEMPERATURE_MIN = ColumnName.of("Min");
        final ColumnName TEMPERATURE_MEAN = ColumnName.of("Mean");
        final ColumnName TEMPERATURE_MAX = ColumnName.of("Max");

        AggregationPlan plan = new AggregationPlan().withColumnType(STATION, String.class)
                .withColumnType(TEMPERATURE, double.class).withOutputTableName(TableName.of("StationSummary"))
                .withGroupBy(STATION).withAggregate(TEMPERATURE, TEMPERATURE_COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, TEMPERATURE_SUM, Aggregate.SUM)
                .withAggregate(TEMPERATURE, TEMPERATURE_MIN, Aggregate.MIN)
                .withAggregate(TEMPERATURE, TEMPERATURE_MEAN, Aggregate.MEAN)
                .withAggregate(TEMPERATURE, TEMPERATURE_MAX, Aggregate.MAX);

        Csv.ReadSettings readSettings = new Csv.ReadSettings().withSeparator(';').withColumnRename(COLUMN_1, STATION)
                .withColumnRename(COLUMN_2, TEMPERATURE);

        TableColumnar table = plan.execute(
                DataSources.fromString("Amsterdam;10.0\nAmsterdam;14.0\nLondon;7.0\n", "1brc.csv"), readSettings,
                new app.babylon.table.io.HeaderStrategyNoHeaders(10));

        assertEquals(TableName.of("StationSummary"), table.getName());
        assertEquals(2, table.getRowCount());
        assertEquals("Amsterdam", table.getString(STATION).get(0));
        assertEquals(2L, table.getLong(TEMPERATURE_COUNT).get(0));
        assertEquals(24.0d, table.getDouble(TEMPERATURE_SUM).get(0));
        assertEquals(10.0d, table.getDouble(TEMPERATURE_MIN).get(0));
        assertEquals(12.0d, table.getDouble(TEMPERATURE_MEAN).get(0));
        assertEquals(14.0d, table.getDouble(TEMPERATURE_MAX).get(0));
        assertEquals("London", table.getString(STATION).get(1));
        assertEquals(1L, table.getLong(TEMPERATURE_COUNT).get(1));
        assertEquals(7.0d, table.getDouble(TEMPERATURE_SUM).get(1));
        assertEquals(7.0d, table.getDouble(TEMPERATURE_MIN).get(1));
        assertEquals(7.0d, table.getDouble(TEMPERATURE_MEAN).get(1));
        assertEquals(7.0d, table.getDouble(TEMPERATURE_MAX).get(1));
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

        AggregationPlan plan = new AggregationPlan().withColumnType(STATION, String.class)
                .withColumnType(TEMPERATURE, double.class).withColumnType(HUMIDITY, double.class).withGroupBy(STATION)
                .withAggregate(TEMPERATURE, MIN_TEMPERATURE, Aggregate.MIN)
                .withAggregate(HUMIDITY, MAX_HUMIDITY, Aggregate.MAX);

        Csv.ReadSettings readSettings = new Csv.ReadSettings().withSeparator(';').withColumnRename(COLUMN_1, STATION)
                .withColumnRename(COLUMN_2, TEMPERATURE).withColumnRename(COLUMN_3, HUMIDITY);

        TableColumnar table = plan.execute(
                DataSources.fromString("Amsterdam;10.0;85.0\nAmsterdam;12.0;82.0\nLondon;7.0;91.0\n", "1brc.csv"),
                readSettings, new app.babylon.table.io.HeaderStrategyNoHeaders(10));

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
        final ColumnName TEMPERATURE_COUNT = ColumnName.of("Count");
        final ColumnName TEMPERATURE_MEAN = ColumnName.of("Mean");

        AggregationPlan plan = new AggregationPlan().withColumnType(STATION, String.class)
                .withColumnType(COUNTRY, String.class).withColumnType(TEMPERATURE, double.class)
                .withOutputTableName(TableName.of("StationCountrySummary")).withGroupBy(STATION, COUNTRY)
                .withAggregate(TEMPERATURE, TEMPERATURE_COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, TEMPERATURE_MEAN, Aggregate.MEAN);

        Csv.ReadSettings readSettings = new Csv.ReadSettings().withSeparator(';').withColumnRename(COLUMN_1, STATION)
                .withColumnRename(COLUMN_2, COUNTRY).withColumnRename(COLUMN_3, TEMPERATURE);

        TableColumnar table = plan
                .execute(
                        DataSources.fromString(
                                "Amsterdam;NL;10.0\nAmsterdam;NL;14.0\nAmsterdam;US;30.0\nLondon;UK;7.0\n", "1brc.csv"),
                        readSettings, new app.babylon.table.io.HeaderStrategyNoHeaders(10));

        assertEquals(3, table.getRowCount());
        assertEquals("Amsterdam", table.getString(STATION).get(0));
        assertEquals("NL", table.getString(COUNTRY).get(0));
        assertEquals(2L, table.getLong(TEMPERATURE_COUNT).get(0));
        assertEquals(12.0d, table.getDouble(TEMPERATURE_MEAN).get(0));
        assertEquals("Amsterdam", table.getString(STATION).get(1));
        assertEquals("US", table.getString(COUNTRY).get(1));
        assertEquals(1L, table.getLong(TEMPERATURE_COUNT).get(1));
        assertEquals(30.0d, table.getDouble(TEMPERATURE_MEAN).get(1));
        assertEquals("London", table.getString(STATION).get(2));
        assertEquals("UK", table.getString(COUNTRY).get(2));
        assertEquals(1L, table.getLong(TEMPERATURE_COUNT).get(2));
        assertEquals(7.0d, table.getDouble(TEMPERATURE_MEAN).get(2));
    }

    @Test
    void streamingAndInMemoryExecutionShouldProduceMatchingSummaries()
    {
        final ColumnName STATION = ColumnName.of("Station");
        final ColumnName COUNTRY = ColumnName.of("Country");
        final ColumnName TEMPERATURE = ColumnName.of("Temperature");
        final ColumnName HUMIDITY = ColumnName.of("Humidity");
        final ColumnName TEMPERATURE_COUNT = ColumnName.of("Count");
        final ColumnName TEMPERATURE_SUM = ColumnName.of("Sum");
        final ColumnName TEMPERATURE_MIN = ColumnName.of("Min");
        final ColumnName TEMPERATURE_MEAN = ColumnName.of("Mean");
        final ColumnName TEMPERATURE_MAX = ColumnName.of("Max");
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

        AggregationPlan plan = new AggregationPlan().withOutputTableName(TableName.of("StationCountrySummary"))
                .withColumnType(STATION, String.class).withColumnType(COUNTRY, String.class)
                .withColumnType(TEMPERATURE, double.class).withColumnType(HUMIDITY, double.class)
                .withGroupBy(STATION, COUNTRY).withAggregate(TEMPERATURE, TEMPERATURE_COUNT, Aggregate.COUNT)
                .withAggregate(TEMPERATURE, TEMPERATURE_SUM, Aggregate.SUM)
                .withAggregate(TEMPERATURE, TEMPERATURE_MIN, Aggregate.MIN)
                .withAggregate(TEMPERATURE, TEMPERATURE_MEAN, Aggregate.MEAN)
                .withAggregate(TEMPERATURE, TEMPERATURE_MAX, Aggregate.MAX)
                .withAggregate(HUMIDITY, HUMIDITY_MAX, Aggregate.MAX);

        DataSource streamingSource = DataSources.fromString(csv, "summary.csv");
        DataSource inMemorySource = DataSources.fromString(csv, "summary.csv");

        TableColumnar streamingResult = plan.execute(streamingSource,
                newReadSettings(STATION, COUNTRY, TEMPERATURE, HUMIDITY));
        TableColumnar parsedTable = Csv.read(inMemorySource, newReadSettings(STATION, COUNTRY, TEMPERATURE, HUMIDITY),
                new HeaderStrategyAuto(), RowConsumerTableCreator
                        .create(newReadSettings(STATION, COUNTRY, TEMPERATURE, HUMIDITY), plan.getColumnTypes()));
        TableColumnar inMemoryResult = plan.execute(parsedTable);

        assertEquals(streamingResult.getName(), inMemoryResult.getName());
        assertEquals(streamingResult.getRowCount(), inMemoryResult.getRowCount());

        Map<String, SummaryRow> streamingRows = toSummaryRows(streamingResult, STATION, COUNTRY, TEMPERATURE_COUNT,
                TEMPERATURE_SUM, TEMPERATURE_MIN, TEMPERATURE_MEAN, TEMPERATURE_MAX, HUMIDITY_MAX);
        Map<String, SummaryRow> inMemoryRows = toSummaryRows(inMemoryResult, STATION, COUNTRY, TEMPERATURE_COUNT,
                TEMPERATURE_SUM, TEMPERATURE_MIN, TEMPERATURE_MEAN, TEMPERATURE_MAX, HUMIDITY_MAX);

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

    private static Csv.ReadSettings newReadSettings(ColumnName station, ColumnName country, ColumnName temperature,
            ColumnName humidity)
    {
        return new Csv.ReadSettings().withSeparator(',');
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

    private static record SummaryRow(long count, double sum, double min, double mean, double max, double humidityMax)
    {
    }

}
