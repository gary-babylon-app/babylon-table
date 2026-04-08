package app.babylon.table.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.io.DataSources;
import app.babylon.table.io.ReadSettingsCSV;
import app.babylon.table.transform.TransformToDouble;

class AggregationPlanTest
{
    @Test
    void shouldCaptureTransformsGroupBysAndAggregatesInOrder()
    {
        final ColumnName STATION = ColumnName.of("station");
        final ColumnName OBSERVATION = ColumnName.of("observation");
        final ColumnName MEAN_OBSERVATION = ColumnName.of("mean_observation");

        AggregationPlan plan = new AggregationPlan().withOutputTableName(TableName.of("summary"))
                .withTransform(new TransformToDouble(OBSERVATION)).withGroupBy(STATION)
                .withAggregate(OBSERVATION, MEAN_OBSERVATION, Aggregate.MEAN);

        assertEquals(TableName.of("summary"), plan.getOutputTableName());
        assertEquals(1, plan.getTransforms().size());
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

        ReadSettingsCSV readSettings = new ReadSettingsCSV().withSeparator(';')
                .withHeaderStrategy(new app.babylon.table.io.HeaderStrategyNoHeaders(10))
                .withColumnRename(COLUMN_1, STATION).withColumnRename(COLUMN_2, TEMPERATURE);

        TableColumnar table = plan.execute(
                DataSources.fromString("Amsterdam;10.0\nAmsterdam;14.0\nLondon;7.0\n", "1brc.csv"), readSettings);

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
        assertEquals(Column.Type.of(double.class), readSettings.getColumnType(TEMPERATURE));
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

        ReadSettingsCSV readSettings = new ReadSettingsCSV().withSeparator(';')
                .withHeaderStrategy(new app.babylon.table.io.HeaderStrategyNoHeaders(10))
                .withColumnRename(COLUMN_1, STATION).withColumnRename(COLUMN_2, TEMPERATURE)
                .withColumnRename(COLUMN_3, HUMIDITY);

        TableColumnar table = plan.execute(
                DataSources.fromString("Amsterdam;10.0;85.0\nAmsterdam;12.0;82.0\nLondon;7.0;91.0\n", "1brc.csv"),
                readSettings);

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

        ReadSettingsCSV readSettings = new ReadSettingsCSV().withSeparator(';')
                .withHeaderStrategy(new app.babylon.table.io.HeaderStrategyNoHeaders(10))
                .withColumnRename(COLUMN_1, STATION).withColumnRename(COLUMN_2, COUNTRY)
                .withColumnRename(COLUMN_3, TEMPERATURE);

        TableColumnar table = plan.execute(DataSources.fromString(
                "Amsterdam;NL;10.0\nAmsterdam;NL;14.0\nAmsterdam;US;30.0\nLondon;UK;7.0\n", "1brc.csv"), readSettings);

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

}
