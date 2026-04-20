package app.babylon.table.plans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.babylon.io.StreamSource;
import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableException;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.aggregation.AccumulatorDouble;
import app.babylon.table.aggregation.Aggregate;
import app.babylon.table.column.Column;
import app.babylon.table.column.Column.Builder;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnLong;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.Columns;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.grouping.GroupBy;
import app.babylon.table.grouping.GroupKey;
import app.babylon.table.io.Row;
import app.babylon.table.io.RowConsumer;
import app.babylon.table.io.RowKey;
import app.babylon.table.io.RowSource;
import app.babylon.table.io.RowCursor;
import app.babylon.table.io.TabularRowReader;

public class TablePlanAggregate extends TablePlanCommon<TablePlanAggregate>
{
    private static final class RowConsumerGroupAggregate implements RowConsumer
    {
        private static final class GroupAccumulators
        {
            private final AccumulatorDouble[] accumulators;

            private GroupAccumulators(int size)
            {
                this.accumulators = new AccumulatorDouble[size];
                for (int i = 0; i < size; ++i)
                {
                    this.accumulators[i] = new AccumulatorDouble();
                }
            }
        }

        private final TablePlanAggregate plan;
        private int[] groupByPositions;
        private int maxGroupByPosition;
        private int[] aggregatePositions;
        private int maxAggregatePosition;
        private final Map<RowKey, GroupAccumulators> accumulatorsByGroup;

        private RowConsumerGroupAggregate(TablePlanAggregate plan)
        {
            this.plan = plan;
            this.groupByPositions = null;
            this.maxGroupByPosition = -1;
            this.aggregatePositions = null;
            this.maxAggregatePosition = -1;
            this.accumulatorsByGroup = new LinkedHashMap<>();
        }

        @Override
        public void start(ColumnName[] columnNames)
        {
            this.groupByPositions = new int[this.plan.groupByColumns.size()];
            Arrays.fill(this.groupByPositions, -1);
            this.aggregatePositions = new int[this.plan.aggregateSpecs.size()];
            Arrays.fill(this.aggregatePositions, -1);
            for (int i = 0; i < columnNames.length; ++i)
            {
                ColumnName columnName = columnNames[i];
                for (int j = 0; j < this.plan.groupByColumns.size(); ++j)
                {
                    if (this.plan.groupByColumns.get(j).equals(columnName))
                    {
                        this.groupByPositions[j] = i;
                    }
                }
                for (int j = 0; j < this.plan.aggregateSpecs.size(); ++j)
                {
                    if (this.plan.aggregateSpecs.get(j).sourceColumnName().equals(columnName))
                    {
                        this.aggregatePositions[j] = i;
                    }
                }
            }
            for (int i = 0; i < this.groupByPositions.length; ++i)
            {
                if (this.groupByPositions[i] < 0)
                {
                    throw new IllegalArgumentException(
                            "Group-by column not present in selected headers: " + this.plan.groupByColumns.get(i));
                }
            }
            for (int i = 0; i < this.aggregatePositions.length; ++i)
            {
                if (this.aggregatePositions[i] < 0)
                {
                    throw new IllegalArgumentException("Aggregate source column not present in selected headers: "
                            + this.plan.aggregateSpecs.get(i).sourceColumnName());
                }
            }
            this.maxGroupByPosition = max(this.groupByPositions);
            this.maxAggregatePosition = max(this.aggregatePositions);
        }

        @Override
        public void accept(Row row)
        {
            if (row.size() <= Math.max(this.maxGroupByPosition, this.maxAggregatePosition))
            {
                return;
            }

            RowKey groupKey = row.keyOf(this.groupByPositions);
            GroupAccumulators accumulators = this.accumulatorsByGroup.computeIfAbsent(groupKey,
                    k -> new GroupAccumulators(this.aggregatePositions.length));
            for (int i = 0; i < this.aggregatePositions.length; ++i)
            {
                int aggregatePosition = this.aggregatePositions[i];
                int length = row.length(aggregatePosition);
                if (length <= 0)
                {
                    continue;
                }
                accumulators.accumulators[i].accept(row, row.start(aggregatePosition), length);
            }
        }

        public TableColumnar build()
        {
            @SuppressWarnings("unchecked")
            ColumnObject.Builder<String>[] groupByBuilders = new ColumnObject.Builder[this.plan.groupByColumns.size()];
            for (int i = 0; i < groupByBuilders.length; ++i)
            {
                groupByBuilders[i] = ColumnObject.builder(this.plan.groupByColumns.get(i), ColumnTypes.STRING);
            }
            Column.Builder[] aggregateBuilders = newAggregateBuilders(this.plan);
            for (Map.Entry<RowKey, GroupAccumulators> entry : this.accumulatorsByGroup.entrySet())
            {
                RowKey groupKey = entry.getKey();
                for (int i = 0; i < groupByBuilders.length; ++i)
                {
                    groupByBuilders[i].add(groupKey.getString(i));
                }
                addAggregateValues(aggregateBuilders, this.plan, entry.getValue().accumulators);
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
            TableName tableName = this.plan.getTableName();
            if (tableName == null)
            {
                tableName = TableName.of("Summary");
            }
            return Tables.newTable(tableName, this.plan.getTableDescription(), columns);
        }

    }

    public static record AggregateSpec(ColumnName sourceColumnName, ColumnName outputColumnName, Aggregate aggregate)
    {
        public AggregateSpec
        {
            sourceColumnName = ArgumentCheck.nonNull(sourceColumnName);
            outputColumnName = ArgumentCheck.nonNull(outputColumnName);
            aggregate = ArgumentCheck.nonNull(aggregate);
        }
    }

    private final List<ColumnName> groupByColumns;
    private final List<AggregateSpec> aggregateSpecs;
    private final Map<ColumnName, Column.Type> columnTypes;

    public TablePlanAggregate()
    {
        this.groupByColumns = new ArrayList<>();
        this.aggregateSpecs = new ArrayList<>();
        this.columnTypes = new LinkedHashMap<>();
    }

    public TablePlanAggregate withColumnType(ColumnName columnName, Column.Type columnType)
    {
        this.columnTypes.put(ArgumentCheck.nonNull(columnName), ArgumentCheck.nonNull(columnType));
        return this;
    }

    public Column.Type getColumnType(ColumnName columnName)
    {
        return this.columnTypes.get(columnName);
    }

    public Map<ColumnName, Column.Type> getColumnTypes()
    {
        return Collections.unmodifiableMap(this.columnTypes);
    }

    public TablePlanAggregate withGroupBy(ColumnName... columnNames)
    {
        if (columnNames != null)
        {
            this.groupByColumns.addAll(Arrays.asList(ArgumentCheck.nonNull(columnNames)));
        }
        return this;
    }

    public List<ColumnName> getGroupByColumns()
    {
        return Collections.unmodifiableList(this.groupByColumns);
    }

    public TablePlanAggregate withAggregate(ColumnName sourceColumnName, Aggregate aggregate)
    {
        return withAggregate(sourceColumnName, sourceColumnName, aggregate);
    }

    public TablePlanAggregate withAggregate(ColumnName sourceColumnName, ColumnName outputColumnName,
            Aggregate aggregate)
    {
        this.aggregateSpecs.add(new AggregateSpec(sourceColumnName, outputColumnName, aggregate));
        return this;
    }

    public List<AggregateSpec> getAggregateSpecs()
    {
        return Collections.unmodifiableList(this.aggregateSpecs);
    }

    @SuppressWarnings("unchecked")
    public TableColumnar execute(TableColumnar table)
    {
        validate();
        TableColumnar sourceTable = ArgumentCheck.nonNull(table);

        GroupBy grouped = sourceTable.groupBy(this.groupByColumns.toArray(new ColumnName[this.groupByColumns.size()]));
        Map<GroupKey, TableColumnar> groupedTables = grouped.getGroupedTables(new LinkedHashMap<>());

        ColumnObject.Builder<Object>[] groupByBuilders = new ColumnObject.Builder[this.groupByColumns.size()];
        for (int i = 0; i < groupByBuilders.length; ++i)
        {
            Column groupColumn = sourceTable.get(this.groupByColumns.get(i));
            groupByBuilders[i] = (ColumnObject.Builder<Object>) Columns.newBuilder(groupColumn.getName(),
                    groupColumn.getType());
        }

        Column.Builder[] aggregateBuilders = newAggregateBuilders(sourceTable, this);
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
        TableName tableName = getTableName() == null ? TableName.of("Summary") : getTableName();
        return Tables.newTable(tableName, getTableDescription(), columns);
    }

    @Override
    public TableColumnar execute(RowCursor rowCursor)
    {
        validate();
        RowCursor checkedRowCursor = ArgumentCheck.nonNull(rowCursor);
        RowConsumerGroupAggregate rowConsumer = new RowConsumerGroupAggregate(this);
        rowConsumer.start(toColumnNames(checkedRowCursor.columns()));
        while (checkedRowCursor.next())
        {
            rowConsumer.accept(checkedRowCursor.current());
        }
        return rowConsumer.build();
    }

    @Override
    public TableColumnar execute(RowSource rowSource)
    {
        validate();
        RowSource checkedRowSource = ArgumentCheck.nonNull(rowSource);
        try (RowCursor rowCursor = checkedRowSource.openRows())
        {
            return execute(rowCursor);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TableException("Failed to aggregate rows from row source '" + checkedRowSource.getName() + "'.",
                    e);
        }
    }

    private static ColumnName[] toColumnNames(app.babylon.table.column.ColumnDefinition[] columnDefinitions)
    {
        ColumnName[] columnNames = new ColumnName[columnDefinitions.length];
        for (int i = 0; i < columnDefinitions.length; ++i)
        {
            columnNames[i] = columnDefinitions[i].name();
        }
        return columnNames;
    }

    private void validate()
    {
        if (this.groupByColumns.isEmpty())
        {
            throw new IllegalArgumentException(
                    "Current TablePlanAggregate.execute requires at least one group-by column.");
        }
        if (this.aggregateSpecs.isEmpty())
        {
            throw new IllegalArgumentException("Current TablePlanAggregate.execute requires at least one aggregate.");
        }

        for (AggregateSpec aggregateSpec : this.aggregateSpecs)
        {
            if (!isSupportedAggregate(aggregateSpec.aggregate()))
            {
                throw new IllegalArgumentException(
                        "Unsupported aggregate for current TablePlanAggregate.execute: " + aggregateSpec.aggregate());
            }
        }
    }

    private static boolean isSupportedAggregate(Aggregate aggregate)
    {
        return aggregate == Aggregate.COUNT || aggregate == Aggregate.MIN || aggregate == Aggregate.MAX
                || aggregate == Aggregate.SUM || aggregate == Aggregate.MEAN || aggregate == Aggregate.VARIANCE
                || aggregate == Aggregate.VARIANCE_SAMPLE;
    }

    private static Column.Builder[] newAggregateBuilders(TablePlanAggregate plan)
    {
        Column.Builder[] aggregateBuilders = new Column.Builder[plan.aggregateSpecs.size()];
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            AggregateSpec aggregateSpec = plan.aggregateSpecs.get(i);
            if (aggregateSpec.aggregate() == Aggregate.COUNT)
            {
                aggregateBuilders[i] = ColumnLong.builder(aggregateSpec.outputColumnName());
            }
            else
            {
                aggregateBuilders[i] = ColumnDouble.builder(aggregateSpec.outputColumnName());
            }
        }
        return aggregateBuilders;
    }

    private static Column.Builder[] newAggregateBuilders(TableColumnar table, TablePlanAggregate plan)
    {
        Column.Builder[] aggregateBuilders = new Column.Builder[plan.aggregateSpecs.size()];
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            AggregateSpec aggregateSpec = plan.aggregateSpecs.get(i);
            if (aggregateSpec.aggregate() == Aggregate.COUNT)
            {
                aggregateBuilders[i] = ColumnLong.builder(aggregateSpec.outputColumnName());
                continue;
            }

            Column sourceColumn = table.get(aggregateSpec.sourceColumnName());
            if (sourceColumn instanceof ColumnDouble)
            {
                aggregateBuilders[i] = ColumnDouble.builder(aggregateSpec.outputColumnName());
            }
            else if (sourceColumn instanceof ColumnObject<?>
                    && BigDecimal.class.equals(sourceColumn.getType().getValueClass()))
            {
                aggregateBuilders[i] = ColumnObject.builderDecimal(aggregateSpec.outputColumnName());
            }
            else
            {
                throw new IllegalArgumentException(
                        "Unsupported aggregate source column type for " + aggregateSpec.sourceColumnName() + ": "
                                + (sourceColumn == null ? "null" : sourceColumn.getClass().getSimpleName()));
            }
        }
        return aggregateBuilders;
    }

    private static void addAggregateValues(Column.Builder[] aggregateBuilders, TablePlanAggregate plan,
            AccumulatorDouble[] accumulators)
    {
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            Aggregate aggregate = plan.aggregateSpecs.get(i).aggregate();
            if (aggregate == Aggregate.COUNT)
            {
                ((ColumnLong.Builder) aggregateBuilders[i]).add(accumulators[i].getCount());
            }
            else
            {
                ((ColumnDouble.Builder) aggregateBuilders[i]).add(accumulators[i].get(aggregate));
            }
        }
    }

    private static void addAggregateValues(Column.Builder[] aggregateBuilders, TablePlanAggregate plan,
            TableColumnar table)
    {
        for (int i = 0; i < aggregateBuilders.length; ++i)
        {
            AggregateSpec aggregateSpec = plan.aggregateSpecs.get(i);
            Aggregate aggregate = aggregateSpec.aggregate();
            Column sourceColumn = table.get(aggregateSpec.sourceColumnName());
            Column.Builder builder = aggregateBuilders[i];

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
                    + aggregateSpec.sourceColumnName() + ": " + sourceColumn.getClass().getSimpleName());
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

    @Override
    protected TablePlanAggregate self()
    {
        return this;
    }
}
