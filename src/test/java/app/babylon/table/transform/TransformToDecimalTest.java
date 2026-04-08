package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.Column;
import app.babylon.table.ColumnCategorical;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.ViewIndex;

class TransformToDecimalTest
{
    private static void assertDecimalEquals(String expected, BigDecimal actual)
    {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        builder.add("1.25");
        builder.add("1.2 3");
        builder.add("abc");
        builder.addNull();
        builder.add("-2.50");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformed = new TransformToDecimal(ColumnName.of("amount")).apply((Column) source);

        assertEquals(5, transformed.size());
        assertDecimalEquals("1.25", transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertFalse(transformed.isSet(2));
        assertFalse(transformed.isSet(3));
        assertDecimalEquals("-2.50", transformed.get(4));
    }

    @Test
    void applyShouldPreserveCategoricalShapeAndMapInvalidValuesToCategoryZero()
    {
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("amount"), String.class);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.add("bad");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformedBase = new TransformToDecimal(ColumnName.of("amount"))
                .apply((Column) source);
        assertTrue(transformedBase instanceof ColumnCategorical<?>);

        ColumnCategorical<BigDecimal> transformed = (ColumnCategorical<BigDecimal>) transformedBase;

        assertEquals(5, transformed.size());
        assertTrue(transformed.isSet(0));
        assertDecimalEquals("10", transformed.get(0));
        assertEquals(transformed.getCategoryCode(0), transformed.getCategoryCode(2));

        assertFalse(transformed.isSet(1));
        assertFalse(transformed.isSet(3));
        assertFalse(transformed.isSet(4));
        assertEquals(0, transformed.getCategoryCode(1));
        assertEquals(0, transformed.getCategoryCode(3));
        assertEquals(0, transformed.getCategoryCode(4));
        assertArrayEquals(new int[]
        {transformed.getCategoryCode(0)}, transformed.getCategoryCodes(null));
    }

    @Test
    void applyShouldReturnAllInvalidCategoricalValuesAsNullCategoryZero()
    {
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("amount"), String.class);
        builder.add("bad");
        builder.add("worse");
        builder.add("bad");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformedBase = new TransformToDecimal(ColumnName.of("amount"))
                .apply((Column) source);
        assertTrue(transformedBase instanceof ColumnCategorical<?>);

        ColumnCategorical<BigDecimal> transformed = (ColumnCategorical<BigDecimal>) transformedBase;

        assertFalse(transformed.isSet(0));
        assertFalse(transformed.isSet(1));
        assertFalse(transformed.isSet(2));
        assertEquals(0, transformed.getCategoryCode(0));
        assertEquals(0, transformed.getCategoryCode(1));
        assertEquals(0, transformed.getCategoryCode(2));
        assertArrayEquals(new int[0], transformed.getCategoryCodes(null));
    }

    @Test
    void viewOnDecimalTransformShouldExposeOnlyLiveDecimalCodes()
    {
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("amount"), String.class);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.add("bad");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformedBase = new TransformToDecimal(ColumnName.of("amount"))
                .apply((Column) source);
        ColumnCategorical<BigDecimal> transformed = (ColumnCategorical<BigDecimal>) transformedBase;

        ViewIndex.Builder rowIndexBuilder = ViewIndex.builder();
        rowIndexBuilder.add(0);
        rowIndexBuilder.add(2);
        rowIndexBuilder.add(0);
        ColumnCategorical<BigDecimal> view = transformed.view(rowIndexBuilder.build());

        assertEquals(3, view.size());
        assertTrue(view.isSet(0));
        assertTrue(view.isSet(1));
        assertTrue(view.isSet(2));
        assertDecimalEquals("10", view.get(0));
        assertEquals(view.getCategoryCode(0), view.getCategoryCode(1));
        assertEquals(view.getCategoryCode(0), view.getCategoryCode(2));
        assertArrayEquals(new int[]
        {view.getCategoryCode(0)}, view.getCategoryCodes(null));
    }

    @Test
    void applyTableShouldReplaceSelectedColumnsAndKeepDecimalColumnsAsIs()
    {
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        amountBuilder.add("1");
        amountBuilder.add("2");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(ColumnName.of("other"), String.class);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnObject.Builder<BigDecimal> existingDecimalBuilder = ColumnObject
                .builderDecimal(ColumnName.of("existing"));
        existingDecimalBuilder.add(new BigDecimal("3"));
        existingDecimalBuilder.add(new BigDecimal("4"));
        ColumnObject<BigDecimal> existingDecimal = existingDecimalBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingDecimal);

        TableColumnar transformed = table
                .apply(new TransformToDecimal(ColumnName.of("amount"), ColumnName.of("existing")));

        ColumnObject<BigDecimal> amount = transformed.getDecimal(ColumnName.of("amount"));
        assertDecimalEquals("1", amount.get(0));
        assertDecimalEquals("2", amount.get(1));

        assertEquals("x", transformed.getString(ColumnName.of("other")).get(0));
        assertSame(existingDecimal, transformed.getDecimal(ColumnName.of("existing")));
    }

    @Test
    void applyShouldAcceptScientificNotation()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        builder.add("1e6");
        builder.add("1E-6");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformed = new TransformToDecimal(ColumnName.of("amount")).apply((Column) source);

        assertDecimalEquals("1000000", transformed.get(0));
        assertDecimalEquals("0.000001", transformed.get(1));
    }
}
