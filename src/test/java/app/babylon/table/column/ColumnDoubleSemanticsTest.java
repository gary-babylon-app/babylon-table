/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.column;

import app.babylon.table.plans.TablePlanAggregate;
import app.babylon.table.aggregation.Aggregate;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.ViewIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ColumnDoubleSemanticsTest
{
    @Test
    public void mutableColumnTracksNullsAndSupportsRegularValues()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnDouble.Builder columnBuilder = (ColumnDouble.Builder) ColumnDouble.builder(VALUES);
        columnBuilder.add(42.0);
        columnBuilder.addNull();
        columnBuilder.add(-123.5);
        ColumnDouble column = columnBuilder.build();

        assertEquals(3, column.size());
        assertTrue(column.isSet(0), "regular values must remain set");
        assertFalse(column.isSet(1), "addNull should create an unset entry");
        assertTrue(column.isSet(2), "regular values after null should remain set");
        assertEquals("42.0", column.toString(0));
        assertEquals("", column.toString(1));
        assertEquals("-123.5", column.toString(2));
    }

    @Test
    public void mutableCopyPreservesValuesAndNullMarkers()
    {
        final ColumnName ORIGINAL = ColumnName.of("original");
        ColumnDouble.Builder builder = (ColumnDouble.Builder) ColumnDouble.builder(ORIGINAL);
        builder.add(10.25);
        builder.addNull();
        builder.add(-3.0);
        ColumnDouble original = builder.build();

        ColumnDouble copy = original.copy(original.getName());

        assertEquals(original.size(), copy.size());
        assertEquals(original.getName(), copy.getName());
        assertTrue(copy.isSet(0));
        assertFalse(copy.isSet(1));
        assertTrue(copy.isSet(2));
        assertEquals("10.25", copy.toString(0));
        assertEquals("", copy.toString(1));
        assertEquals("-3.0", copy.toString(2));
    }

    @Test
    public void copyWithNewNamePreservesData()
    {
        final ColumnName SOURCE = ColumnName.of("source");
        final ColumnName TARGET = ColumnName.of("target");
        ColumnDouble.Builder builder = (ColumnDouble.Builder) ColumnDouble.builder(SOURCE);
        builder.add(7.5);
        builder.addNull();
        ColumnDouble original = builder.build();

        Column renamed = original.copy(TARGET);
        assertTrue(renamed instanceof ColumnDouble);
        ColumnDouble renamedDouble = (ColumnDouble) renamed;

        assertEquals(TARGET, renamedDouble.getName());
        assertEquals(2, renamedDouble.size());
        assertTrue(renamedDouble.isSet(0));
        assertFalse(renamedDouble.isSet(1));
    }

    @Test
    public void getAsColumnReturnsSingleRowConstantColumn()
    {
        final ColumnName SOURCE = ColumnName.of("source");
        ColumnDouble.Builder sourceBuilder = (ColumnDouble.Builder) ColumnDouble.builder(SOURCE);
        sourceBuilder.add(11.0);
        sourceBuilder.add(22.0);
        ColumnDouble source = sourceBuilder.build();

        Column single = source.getAsColumn(1);
        assertTrue(single instanceof ColumnDouble);
        assertEquals(1, single.size());
        assertEquals(SOURCE, single.getName());
        assertEquals("22.0", single.toString(0));
        assertTrue(single.isSet(0));
    }

    @Test
    public void constantColumnWithSentinelValueRepresentsNulls()
    {
        final ColumnName CONSTANT_NULL = ColumnName.of("constantNull");
        ColumnDouble constantNull = new ColumnDoubleConstant(CONSTANT_NULL, 0.0, 3, false);

        assertEquals(3, constantNull.size());
        assertFalse(constantNull.isSet(0));
        assertFalse(constantNull.isSet(1));
        assertFalse(constantNull.isSet(2));
        assertEquals("", constantNull.toString(0));
    }

    @Test
    public void constantViewReturnsResizedConstant()
    {
        final ColumnName C = ColumnName.of("c");
        ColumnDouble constant = new ColumnDoubleConstant(C, 7.5, 10, true);
        ViewIndex.Builder indexBuilder = ViewIndex.builder();
        indexBuilder.add(7);
        indexBuilder.add(2);
        indexBuilder.add(1);

        ColumnDouble view = (ColumnDouble) constant.view(indexBuilder.build());

        assertEquals(3, view.size());
        assertEquals(C, view.getName());
        assertTrue(view.isSet(0));
        assertTrue(view.isSet(1));
        assertTrue(view.isSet(2));
        assertEquals("7.5", view.toString(0));
        assertEquals("7.5", view.toString(1));
        assertEquals("7.5", view.toString(2));
    }

    @Test
    public void aggregateSupportsDoubleColumns()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnDouble.Builder builder = (ColumnDouble.Builder) ColumnDouble.builder(VALUES);
        builder.add(1.5);
        builder.addNull();
        builder.add(3.0);
        builder.add(-2.0);
        ColumnDouble column = builder.build();

        assertEquals(2.5, Columns.aggregate(column, Aggregate.SUM), 1e-12);
        assertEquals(-2.0, Columns.aggregate(column, Aggregate.MIN), 1e-12);
        assertEquals(3.0, Columns.aggregate(column, Aggregate.MAX), 1e-12);
        assertEquals(2.5 / 3.0, Columns.aggregate(column, Aggregate.MEAN), 1e-12);
    }

    @Test
    public void builderShouldAcceptCharSequenceValues()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnDouble.Builder builder = (ColumnDouble.Builder) ColumnDouble.builder(VALUES);
        builder.add("1.25");
        builder.add("12.50");
        builder.add("1 2 3");
        builder.add("abc");
        builder.add((CharSequence) null);
        builder.add("(1,234.50)");
        ColumnDouble column = builder.build();

        assertEquals(6, column.size());
        assertEquals(1.25d, column.get(0), 1.0e-12);
        assertEquals(12.5d, column.get(1), 1.0e-12);
        assertFalse(column.isSet(2));
        assertFalse(column.isSet(3));
        assertFalse(column.isSet(4));
        assertFalse(column.isSet(5));
    }

    @Test
    public void builderShouldAcceptCharArraySlices()
    {
        final ColumnName VALUES = ColumnName.of("values");
        ColumnDouble.Builder builder = (ColumnDouble.Builder) ColumnDouble.builder(VALUES);
        char[] chars = "xx12.75yy".toCharArray();
        builder.add(chars, 2, 5);
        builder.add(chars, 0, 2);
        ColumnDouble column = builder.build();

        assertEquals(2, column.size());
        assertEquals(12.75d, column.get(0), 1.0e-12);
        assertFalse(column.isSet(1));
    }

    @Test
    public void aggregationPlanExecuteTableSupportsDoubleColumns()
    {
        final ColumnName STATION = ColumnName.of("station");
        final ColumnName TEMPERATURE = ColumnName.of("temperature");
        ColumnCategorical.Builder<String> stationBuilder = ColumnCategorical.builder(STATION,
                app.babylon.table.column.ColumnTypes.STRING);
        stationBuilder.add("A");
        stationBuilder.add("A");
        stationBuilder.add("B");
        stationBuilder.add("B");

        ColumnDouble.Builder temperatureBuilder = (ColumnDouble.Builder) ColumnDouble.builder(TEMPERATURE);
        temperatureBuilder.add(10.0);
        temperatureBuilder.add(14.0);
        temperatureBuilder.add(-1.0);
        temperatureBuilder.add(3.0);

        TableColumnar table = Tables.newTable(TableName.of("measurements"), new TableDescription(""),
                stationBuilder.build(), temperatureBuilder.build());

        TableColumnar summary = new TablePlanAggregate().withTableName(TableName.of("summary")).withGroupBy(STATION)
                .withAggregate(TEMPERATURE, Aggregate.MEAN).execute(table);

        ColumnDouble means = summary.getDouble(TEMPERATURE);
        Assertions.assertNotNull(means);
        assertEquals(2, summary.getRowCount());
        for (int i = 0; i < summary.getRowCount(); ++i)
        {
            String station = summary.getString(STATION).get(i);
            if ("A".equals(station))
            {
                assertEquals(12.0, means.get(i), 1e-12);
            }
            else if ("B".equals(station))
            {
                assertEquals(1.0, means.get(i), 1e-12);
            }
        }
    }
}
