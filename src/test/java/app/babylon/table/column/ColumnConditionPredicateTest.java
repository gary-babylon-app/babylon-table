package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.selection.RowPredicate;
import app.babylon.table.dsl.Conditions;

class ColumnConditionPredicateTest
{
    @Test
    void byteColumnsShouldBuildPredicatesFromComparisonConditions()
    {
        ColumnName name = ColumnName.of("Byte");
        Column column = ColumnByte.builder(name).add((byte) 1).add((byte) 2).addNull().build();

        RowPredicate predicate = column.predicate(Conditions.column(name).gte("2"));

        assertFalse(predicate.test(0));
        assertTrue(predicate.test(1));
        assertFalse(predicate.test(2));
    }

    @Test
    void intColumnsShouldBuildPredicatesFromComparisonConditions()
    {
        ColumnName name = ColumnName.of("Int");
        Column column = ColumnInt.builder(name).add(1).add(2).addNull().build();

        RowPredicate predicate = column.predicate(Conditions.column(name).gte("2"));

        assertFalse(predicate.test(0));
        assertTrue(predicate.test(1));
        assertFalse(predicate.test(2));
    }

    @Test
    void longColumnsShouldBuildPredicatesFromComparisonConditions()
    {
        ColumnName name = ColumnName.of("Long");
        Column column = ColumnLong.builder(name).add(1L).add(2L).addNull().build();

        RowPredicate predicate = column.predicate(Conditions.column(name).gte("2"));

        assertFalse(predicate.test(0));
        assertTrue(predicate.test(1));
        assertFalse(predicate.test(2));
    }

    @Test
    void doubleColumnsShouldBuildPredicatesFromComparisonConditions()
    {
        ColumnName name = ColumnName.of("Double");
        Column column = ColumnDouble.builder(name).add(1.5d).add(2.5d).addNull().build();

        RowPredicate predicate = column.predicate(Conditions.column(name).gt("2.0"));

        assertFalse(predicate.test(0));
        assertTrue(predicate.test(1));
        assertFalse(predicate.test(2));
    }

    @Test
    void booleanColumnsShouldBuildPredicatesFromComparisonConditions()
    {
        ColumnName name = ColumnName.of("Boolean");
        Column column = ColumnBoolean.builder(name).add(true).add(false).addNull().build();

        RowPredicate predicate = column.predicate(Conditions.column(name).is("true"));

        assertTrue(predicate.test(0));
        assertFalse(predicate.test(1));
        assertFalse(predicate.test(2));
    }

    @Test
    void objectColumnsShouldBuildPredicatesFromComparisonConditions()
    {
        ColumnName name = ColumnName.of("Amount");
        ColumnObject.Builder<BigDecimal> builder = ColumnObject.builderDecimal(name);
        builder.add(new BigDecimal("1.50"));
        builder.add(new BigDecimal("2.50"));
        builder.addNull();
        Column column = builder.build();

        RowPredicate predicate = column.predicate(Conditions.column(name).gt("2.00"));

        assertFalse(predicate.test(0));
        assertTrue(predicate.test(1));
        assertFalse(predicate.test(2));
    }

    @Test
    void categoricalColumnsShouldBuildPredicatesFromComparisonConditions()
    {
        ColumnName name = ColumnName.of("Side");
        Column column = ColumnCategorical.builder(name, ColumnTypes.STRING).add("Buy").add("Sell").addNull().build();

        RowPredicate predicate = column.predicate(Conditions.column(name).in("Buy"));

        assertTrue(predicate.test(0));
        assertFalse(predicate.test(1));
        assertFalse(predicate.test(2));
    }
}
