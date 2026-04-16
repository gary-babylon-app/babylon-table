package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.ViewIndex;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformToDecimalTest
{
    private static void assertDecimalEquals(String expected, BigDecimal actual)
    {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("1.25");
        builder.add("1.2 3");
        builder.add("abc");
        builder.addNull();
        builder.add("-2.50");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformed = TransformToDecimal.of(AMOUNT).apply((Column) source);

        assertEquals(5, transformed.size());
        assertDecimalEquals("1.25", transformed.get(0));
        assertFalse(transformed.isSet(1));
        assertFalse(transformed.isSet(2));
        assertFalse(transformed.isSet(3));
        assertDecimalEquals("-2.50", transformed.get(4));
    }

    @Test
    void constructorsAndFactoriesShouldCreateWorkingDecimalTransforms()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName PARSED = ColumnName.of("Parsed");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("-12.5");
        builder.add("$1,234.50");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> parsedWithCustomName = TransformToDecimal.of("Amount", "Parsed")
                .apply((Column) source);
        ColumnObject<BigDecimal> parsedFromColumns = TransformToDecimal.of(AMOUNT).apply((Column) source);
        TransformToDecimal fromStringFactory = TransformToDecimal.of("ToDecimal(Amount)");
        TransformToDecimal fromParamsFactory = TransformToDecimal.of("Amount", "Parsed");

        assertEquals(PARSED, parsedWithCustomName.getName());
        assertDecimalEquals("-12.5", parsedWithCustomName.get(0));
        assertDecimalEquals("1234.50", parsedWithCustomName.get(1));

        assertDecimalEquals("-12.5", parsedFromColumns.get(0));
        assertDecimalEquals("1234.50", parsedFromColumns.get(1));

        assertEquals("ToDecimal(Amount)", fromStringFactory.toString());
        assertDecimalEquals("-12.5", fromStringFactory.apply((Column) source).get(0));

        assertTrue(fromParamsFactory instanceof TransformToDecimal);
        ColumnObject<BigDecimal> parsedFromParams = fromParamsFactory.apply((Column) source);
        assertEquals(PARSED, parsedFromParams.getName());
        assertDecimalEquals("-12.5", parsedFromParams.get(0));
        assertDecimalEquals("1234.50", parsedFromParams.get(1));

        assertNull(TransformToDecimal.of(new String[]
        {"Amount"}));
        assertNull(TransformToDecimal.of((ColumnName) null));
    }

    @Test
    void applyShouldPreserveCategoricalShapeAndMapInvalidValuesToCategoryZero()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.add("bad");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformedBase = TransformToDecimal.of(AMOUNT).apply((Column) source);
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
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("bad");
        builder.add("worse");
        builder.add("bad");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformedBase = TransformToDecimal.of(AMOUNT).apply((Column) source);
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
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("10");
        builder.add("bad");
        builder.add("10");
        builder.add("bad");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformedBase = TransformToDecimal.of(AMOUNT).apply((Column) source);
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
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName OTHER = ColumnName.of("OTHER");
        final ColumnName EXISTING = ColumnName.of("EXISTING");
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        amountBuilder.add("1");
        amountBuilder.add("2");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(OTHER,
                app.babylon.table.column.ColumnTypes.STRING);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnObject.Builder<BigDecimal> existingDecimalBuilder = ColumnObject.builderDecimal(EXISTING);
        existingDecimalBuilder.add(new BigDecimal("3"));
        existingDecimalBuilder.add(new BigDecimal("4"));
        ColumnObject<BigDecimal> existingDecimal = existingDecimalBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingDecimal);

        TableColumnar transformed = table.apply(TransformToDecimal.of("AMOUNT", "EXISTING"));

        assertEquals("1", transformed.getString(AMOUNT).get(0));
        assertEquals("2", transformed.getString(AMOUNT).get(1));

        assertEquals("x", transformed.getString(OTHER).get(0));
        ColumnObject<BigDecimal> parsed = transformed.getDecimal(EXISTING);
        assertDecimalEquals("1", parsed.get(0));
        assertDecimalEquals("2", parsed.get(1));
        assertTrue(parsed != existingDecimal);
    }

    @Test
    void applyShouldAcceptScientificNotation()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT,
                app.babylon.table.column.ColumnTypes.STRING);
        builder.add("1e6");
        builder.add("1E-6");
        ColumnObject<String> source = builder.build();

        ColumnObject<BigDecimal> transformed = TransformToDecimal.of(AMOUNT).apply((Column) source);

        assertDecimalEquals("1000000", transformed.get(0));
        assertDecimalEquals("0.000001", transformed.get(1));
    }
}
