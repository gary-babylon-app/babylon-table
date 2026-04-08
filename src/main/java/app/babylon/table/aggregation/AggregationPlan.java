package app.babylon.table.aggregation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.babylon.io.DataSource;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnBuilder;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.grouping.GroupBy;
import app.babylon.table.grouping.GroupKey;
import app.babylon.table.io.Csv;
import app.babylon.table.io.ReadSettingsCSV;
import app.babylon.table.io.Row;
import app.babylon.table.io.RowConsumerFactory;
import app.babylon.table.io.RowConsumerResult;
import app.babylon.table.io.RowKey;
import app.babylon.table.transform.Transform;

public class AggregationPlan
{
    private static final class RowConsumerGroupAggregatePlan implements RowConsumerResult<TableColumnar>
    {
        private final AggregationPlan plan;
        private final int[] groupByPositions;
        private final int maxGroupByPosition;
        private final int aggregatePosition;
        private final Map<RowKey, AccumulatorDouble> accumulatorsByGroup;

        private RowConsumerGroupAggregatePlan(AggregationPlan plan, int[] groupByPositions, int aggregatePosition)
        {
            this.plan = plan;
            this.groupByPositions = Arrays.copyOf(groupByPositions, groupByPositions.length);
            this.maxGroupByPosition = max(this.groupByPositions);
            this.aggregatePosition = aggregatePosition;
            this.accumulatorsByGroup = new LinkedHashMap<>();
        }

        @Override
        public void accept(Row row)
        {
            if (row.fieldCount() <= Math.max(this.maxGroupByPosition, this.aggregatePosition))
            {
                return;
            }

            char[] chars = row.chars();
            RowKey groupKey = RowKey.copyOf(row, this.groupByPositions);
            AccumulatorDouble accumulator = this.accumulatorsByGroup.computeIfAbsent(groupKey,
                    k -> new AccumulatorDouble());
            accumulator.accept(chars, row.start(this.aggregatePosition), row.length(this.aggregatePosition));
        }

        @Override
        public TableColumnar buildResult(DataSource dataSource)
        {
            @SuppressWarnings("unchecked")
            ColumnObject.Builder<String>[] groupByBuilders = new ColumnObject.Builder[this.plan.groupByColumns.size()];
            for (int i = 0; i < groupByBuilders.length; ++i)
            {
                groupByBuilders[i] = ColumnObject.builder(this.plan.groupByColumns.get(i), String.class);
            }
            ColumnBuilder[] aggregateBuilders = newAggregateBuilders(this.plan);
            for (Map.Entry<RowKey, AccumulatorDouble> entry : this.accumulatorsByGroup.entrySet())
            {
                RowKey groupKey = entry.getKey();
                for (int i = 0; i < groupByBuilders.length; ++i)
                {
                    groupByBuilders[i].add(groupKey.getString(i));
                }
                addAggregateValues(aggregateBuilders, this.plan, entry.getValue());
            }

            Column[] columns = new Column[aggregateBuilders.length + groupByBuilders.length];
            for (int i = 0; i < groupByBuilders.length; ++i)
            {
                columns[i] = groupByBuilders[i].build();
            }
            for (int i = 0; i < aggregateBuilders.length; ++i)
            {
                columns[i + groupByBuilders.length] = aggregateBuilders[i].build();
            }
            TableName tableName = this.plan.outputTableName == null
                    ? TableName.of("Summary")
                    : this.plan.outputTableName;
            return Tables.newTable(tableName, columns);
        }

    }

    public static final class AggregateSpec
    {
        private final ColumnName sourceColumnName;
        private final ColumnName outputColumnName;
        private final Aggregate aggregate;

        public AggregateSpec(ColumnName sourceColumnName, ColumnName outputColumnName, Aggregate aggregate)
        {
            this.sourceColumnName = app.babylon.lang.ArgumentCheck.nonNull(sourceColumnName);
            this.outputColumnName = app.babylon.lang.ArgumentCheck.nonNull(outputColumnName);
            this.aggregate = app.babylon.lang.ArgumentCheck.nonNull(aggregate);
        }

        public ColumnName getSourceColumnName()
        {
            return this.sourceColumnName;
        }

        public ColumnName getOutputColumnName()
        {
            return this.outputColumnName;
        }

        public Aggregate getAggregate()
        {
            return this.aggregate;
        }
    }

    private final List<Transform> transforms;
    private final List<ColumnName> groupByColumns;
    private final List<AggregateSpec> aggregateSpecs;
    private final Map<ColumnName, Column.Type> columnTypes;
    private TableName outputTableName;

    public AggregationPlan()
    {
        this.transforms = new ArrayList<>();
        this.groupByColumns = new ArrayList<>();
        this.aggregateSpecs = new ArrayList<>();
        this.columnTypes = new LinkedHashMap<>();
        this.outputTableName = null;
    }

    public AggregationPlan withOutputTableName(TableName outputTableName)
    {
        this.outputTableName = outputTableName;
        return this;
    }

    public TableName getOutputTableName()
    {
        return this.outputTableName;
    }

    public AggregationPlan withTransform(Transform transform)
    {
        if (transform != null)
        {
            this.transforms.add(transform);
        }
        return this;
    }

    public AggregationPlan withTransforms(Transform... transforms)
    {
        if (transforms != null)
        {
            for (Transform transform : transforms)
            {
                withTransform(transform);
            }
        }
        return this;
    }

    public List<Transform> getTransforms()
    {
        return Collections.unmodifiableList(this.transforms);
    }

    public AggregationPlan withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(app.babylon.lang.ArgumentCheck.nonNull(columnName),
                app.babylon.lang.ArgumentCheck.nonNull(columnType));
        return this;
    }

    public AggregationPlan withColumnType(ColumnName columnName, Class<?> valueClass)
    {
        return withColumnType(columnName, Column.Type.of(app.babylon.lang.ArgumentCheck.nonNull(valueClass)));
    }

    public Column.Type getColumnType(ColumnName columnName)
    {
        return this.columnTypes.get(columnName);
    }

    public Map<ColumnName, Column.Type> getColumnTypes()
    {
        return Collections.unmodifiableMap(this.columnTypes);
    }

    public AggregationPlan withGroupBy(ColumnName... columnNames)
    {
        if (columnNames != null)
        {
            this.groupByColumns.addAll(Arrays.asList(app.babylon.lang.ArgumentCheck.nonNull(columnNames)));
        }
        return this;
    }

    public List<ColumnName> getGroupByColumns()
    {
        return Collections.unmodifiableList(this.groupByColumns);
    }

    public AggregationPlan withAggregate(ColumnName sourceColumnName, Aggregate aggregate)
    {
        return withAggregate(sourceColumnName, sourceColumnName, aggregate);
    }

    public AggregationPlan withAggregate(ColumnName sourceColumnName, ColumnName outputColumnName, Aggregate aggregate)
    {
        this.aggregateSpecs.add(new AggregateSpec(sourceColumnName, outputColumnName, aggregate));
        return this;
    }

    public List<AggregateSpec> getAggregateSpecs()
    {
        return Collections.unmodifiableList(this.aggregateSpecs);
    }

    public TableColumnar execute(TableColumnar table)
    {
        validateForCurrentImplementation();
        TableColumnar sourceTable = app.babylon.lang.ArgumentCheck.nonNull(table);
        if (!this.transforms.isEmpty())
        {
            sourceTable = sourceTable.apply(this.transforms);
        }

        GroupBy grouped = sourceTable.groupBy(this.groupByColumns.toArray(new ColumnName[this.groupByColumns.size()]));
        Map<GroupKey, TableColumnar> groupedTables = grouped.getGroupedTables(new LinkedHashMap<>());

        @SuppressWarnings("unchecked")
        ColumnObject.Builder<Object>[] groupByBuilders = new ColumnObject.Builder[this.groupByColumns.size()];
        for (int i = 0; i < groupByBuilders.length; ++i)
        {
            Column groupColumn = sourceTable.get(this.groupByColumns.get(i));
            groupByBuilders[i] = (ColumnObject.Builder<Object>) Columns.newColumn(groupColumn.getName(),
                    groupColumn.getType());
        }

        ColumnBuilder[] aggregateBuilders = newAggregateBuilders(sourceTable, this);
        for (Map.Entry<GroupKey, TableColumnar> entry : groupedTables.entrySet())
        {
            GroupKey groupKey = entry.getKey();
            TableColumnar groupTable = entry.getValue();
            for (int i = 0; i < groupByBuilders.length; ++i)
            {
                groupByBuilders[i].add(groupKey.getComponent(i));
            }
            addAggregateValues(aggregateBuilders, this, groupTable);
        }

        Column[] columns = new Column[groupByBuilders.length + aggregateBuilders.length];
        for (int i = 0; i < groupByBuilders.length; ++i)
        {
            columns[i] = groupByBuilders[i].build();
        }
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            columns[i + groupByBuilders.length] = aggregateBuilders[i].build();
        }
        TableName tableName = this.outputTableName == null ? TableName.of("Summary") : this.outputTableName;
        return Tables.newTable(tableName, columns);
    }

    public TableColumnar execute(DataSource dataSource, ReadSettingsCSV readSettings)
    {
        validateForCurrentImplementation();
        ReadSettingsCSV effectiveReadSettings = readSettings == null ? new ReadSettingsCSV() : readSettings;
        for (Map.Entry<ColumnName, Column.Type> entry : this.columnTypes.entrySet())
        {
            effectiveReadSettings.withColumnType(entry.getKey(), entry.getValue());
        }
        RowConsumerFactory<TableColumnar> rowConsumerFactory = (options, headerDetection) -> {
            String[] selectedHeaders = headerDetection.getSelectedHeaders();
            int[] groupByPositions = new int[this.groupByColumns.size()];
            Arrays.fill(groupByPositions, -1);
            int aggregatePosition = -1;
            ColumnName aggregateColumnName = this.aggregateSpecs.get(0).getSourceColumnName();
            for (int i = 0; i < selectedHeaders.length; ++i)
            {
                ColumnName columnName = options.getRenameColumnName(selectedHeaders[i]);
                for (int j = 0; j < this.groupByColumns.size(); ++j)
                {
                    if (this.groupByColumns.get(j).equals(columnName))
                    {
                        groupByPositions[j] = i;
                    }
                }
                if (aggregateColumnName.equals(columnName))
                {
                    aggregatePosition = i;
                }
            }
            for (int i = 0; i < groupByPositions.length; ++i)
            {
                if (groupByPositions[i] < 0)
                {
                    throw new IllegalArgumentException(
                            "Group-by column not present in selected headers: " + this.groupByColumns.get(i));
                }
            }
            if (aggregatePosition < 0)
            {
                throw new IllegalArgumentException(
                        "Aggregate source column not present in selected headers: " + aggregateColumnName);
            }
            return new RowConsumerGroupAggregatePlan(this, groupByPositions, aggregatePosition);
        };
        return Csv.read(dataSource, effectiveReadSettings, rowConsumerFactory);
    }

    private void validateForCurrentImplementation()
    {
        if (this.groupByColumns.isEmpty())
        {
            throw new IllegalArgumentException(
                    "Current AggregationPlan.execute requires at least one group-by column.");
        }
        if (this.aggregateSpecs.isEmpty())
        {
            throw new IllegalArgumentException("Current AggregationPlan.execute requires at least one aggregate.");
        }

        ColumnName aggregateColumnName = this.aggregateSpecs.get(0).getSourceColumnName();
        for (AggregateSpec aggregateSpec : this.aggregateSpecs)
        {
            if (!aggregateColumnName.equals(aggregateSpec.getSourceColumnName()))
            {
                throw new IllegalArgumentException(
                        "Current AggregationPlan.execute supports exactly one aggregate source column.");
            }
            if (!isSupportedAggregate(aggregateSpec.getAggregate()))
            {
                throw new IllegalArgumentException(
                        "Unsupported aggregate for current AggregationPlan.execute: " + aggregateSpec.getAggregate());
            }
        }
    }

    private static boolean isSupportedAggregate(Aggregate aggregate)
    {
        return aggregate == Aggregate.COUNT || aggregate == Aggregate.MIN || aggregate == Aggregate.MAX
                || aggregate == Aggregate.SUM || aggregate == Aggregate.MEAN;
    }

    private static ColumnBuilder[] newAggregateBuilders(AggregationPlan plan)
    {
        ColumnBuilder[] aggregateBuilders = new ColumnBuilder[plan.aggregateSpecs.size()];
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            AggregateSpec aggregateSpec = plan.aggregateSpecs.get(i);
            if (aggregateSpec.getAggregate() == Aggregate.COUNT)
            {
                aggregateBuilders[i] = ColumnLong.builder(aggregateSpec.getOutputColumnName());
            } else
            {
                aggregateBuilders[i] = ColumnDouble.builder(aggregateSpec.getOutputColumnName());
            }
        }
        return aggregateBuilders;
    }

    private static ColumnBuilder[] newAggregateBuilders(TableColumnar table, AggregationPlan plan)
    {
        ColumnBuilder[] aggregateBuilders = new ColumnBuilder[plan.aggregateSpecs.size()];
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            AggregateSpec aggregateSpec = plan.aggregateSpecs.get(i);
            if (aggregateSpec.getAggregate() == Aggregate.COUNT)
            {
                aggregateBuilders[i] = ColumnLong.builder(aggregateSpec.getOutputColumnName());
                continue;
            }

            Column sourceColumn = table.get(aggregateSpec.getSourceColumnName());
            if (sourceColumn instanceof ColumnDouble)
            {
                aggregateBuilders[i] = ColumnDouble.builder(aggregateSpec.getOutputColumnName());
            } else if (sourceColumn instanceof ColumnObject<?>
                    && BigDecimal.class.equals(sourceColumn.getType().getValueClass()))
            {
                aggregateBuilders[i] = ColumnObject.builderDecimal(aggregateSpec.getOutputColumnName());
            } else
            {
                throw new IllegalArgumentException(
                        "Unsupported aggregate source column type for " + aggregateSpec.getSourceColumnName() + ": "
                                + (sourceColumn == null ? "null" : sourceColumn.getClass().getSimpleName()));
            }
        }
        return aggregateBuilders;
    }

    private static void addAggregateValues(ColumnBuilder[] aggregateBuilders, AggregationPlan plan,
            AccumulatorDouble accumulator)
    {
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            Aggregate aggregate = plan.aggregateSpecs.get(i).getAggregate();
            if (aggregate == Aggregate.COUNT)
            {
                ((ColumnLong.Builder) aggregateBuilders[i]).add(accumulator.getCount());
            } else
            {
                ((ColumnDouble.Builder) aggregateBuilders[i]).add(valueOf(accumulator, aggregate));
            }
        }
    }

    private static void addAggregateValues(ColumnBuilder[] aggregateBuilders, AggregationPlan plan, TableColumnar table)
    {
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            AggregateSpec aggregateSpec = plan.aggregateSpecs.get(i);
            Aggregate aggregate = aggregateSpec.getAggregate();
            Column sourceColumn = table.get(aggregateSpec.getSourceColumnName());
            ColumnBuilder builder = aggregateBuilders[i];

            if (aggregate == Aggregate.COUNT)
            {
                ((ColumnLong.Builder) builder).add(countSetValues(sourceColumn));
                continue;
            }
            if (builder instanceof ColumnDouble.Builder doubleBuilder
                    && sourceColumn instanceof ColumnDouble sourceDouble)
            {
                doubleBuilder.add(Columns.aggregate(sourceDouble, aggregate));
                continue;
            }
            if (builder instanceof ColumnObject.Builder<?> objectBuilder
                    && sourceColumn instanceof ColumnObject<?> sourceObject
                    && BigDecimal.class.equals(sourceColumn.getType().getValueClass()))
            {
                @SuppressWarnings("unchecked")
                ColumnObject.Builder<BigDecimal> decimalBuilder = (ColumnObject.Builder<BigDecimal>) objectBuilder;
                @SuppressWarnings("unchecked")
                ColumnObject<BigDecimal> decimalColumn = (ColumnObject<BigDecimal>) sourceObject;
                decimalBuilder.add(Columns.aggregate(decimalColumn, aggregate));
                continue;
            }
            throw new IllegalArgumentException("Unsupported aggregate source column type for "
                    + aggregateSpec.getSourceColumnName() + ": " + sourceColumn.getClass().getSimpleName());
        }
    }

    private static long countSetValues(Column column)
    {
        long count = 0L;
        for (int i = 0; i < column.size(); ++i)
        {
            if (column.isSet(i))
            {
                ++count;
            }
        }
        return count;
    }

    private static double valueOf(AccumulatorDouble accumulator, Aggregate aggregate)
    {
        return switch (aggregate)
        {
            case COUNT -> throw new IllegalArgumentException("COUNT is not a double aggregate");
            case MIN -> accumulator.getMin();
            case MAX -> accumulator.getMax();
            case SUM -> accumulator.getSum();
            case MEAN -> accumulator.getMean();
        };
    }

    private static int max(int[] values)
    {
        int max = values[0];
        for (int i = 1; i < values.length; ++i)
        {
            if (values[i] > max)
            {
                max = values[i];
            }
        }
        return max;
    }
}
