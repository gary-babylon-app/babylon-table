package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformToDoubleTest
{
    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("1.25");
        builder.add("USD 12.50");
        builder.add("1 2 3");
        builder.add("abc");
        builder.addNull();
        builder.add("(1,234.50)");
        ColumnObject<String> source = builder.build();

        ColumnDouble transformed = new TransformToDouble(AMOUNT).apply((Column) source);

        assertEquals(6, transformed.size());
        assertEquals(1.25d, transformed.get(0), 1.0e-12);
        assertEquals(12.5d, transformed.get(1), 1.0e-12);
        assertFalse(transformed.isSet(2));
        assertFalse(transformed.isSet(3));
        assertFalse(transformed.isSet(4));
        assertEquals(-1234.5d, transformed.get(5), 1.0e-12);
    }

    @Test
    void applyShouldConvertCategoricalStringColumnToPrimitiveDoubleColumn()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("10.5");
        builder.add("bad");
        builder.add("10.5");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnDouble transformed = new TransformToDouble(AMOUNT).apply((Column) source);

        assertEquals(4, transformed.size());
        assertEquals(10.5d, transformed.get(0), 1.0e-12);
        assertFalse(transformed.isSet(1));
        assertEquals(10.5d, transformed.get(2), 1.0e-12);
        assertFalse(transformed.isSet(3));
        assertTrue(!(transformed instanceof ColumnCategorical<?>));
    }

    @Test
    void applyTableShouldAddParsedDoubleColumnAndKeepExistingDoubleColumnsAsIs()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName OTHER = ColumnName.of("OTHER");
        final ColumnName EXISTING = ColumnName.of("EXISTING");
        final ColumnName PARSED = ColumnName.of("PARSED");
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        amountBuilder.add("1.25");
        amountBuilder.add("USD 12.50");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(OTHER, ColumnTypes.STRING);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnDouble.Builder existingDoubleBuilder = ColumnDouble.builder(EXISTING);
        existingDoubleBuilder.add(3.0d);
        existingDoubleBuilder.add(4.0d);
        ColumnDouble existingDouble = existingDoubleBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingDouble);

        TableColumnar transformed = table.apply(new TransformToDouble(AMOUNT, PARSED));

        ColumnDouble parsed = transformed.getDouble(PARSED);
        assertEquals(1.25d, parsed.get(0), 1.0e-12);
        assertEquals(12.5d, parsed.get(1), 1.0e-12);

        assertEquals("x", transformed.getString(OTHER).get(0));
        assertSame(existingDouble, transformed.getDouble(EXISTING));
    }

    @Test
    void applyShouldCopyExistingDoubleColumnWhenAppliedDirectly()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        final ColumnName RENAMED = ColumnName.of("RENAMED");
        ColumnDouble.Builder builder = ColumnDouble.builder(AMOUNT);
        builder.add(1.0d);
        builder.addNull();
        builder.add(3.5d);
        ColumnDouble source = builder.build();

        ColumnDouble transformed = new TransformToDouble(AMOUNT, RENAMED).apply((Column) source);

        assertNotSame(source, transformed);
        assertEquals(RENAMED, transformed.getName());
        assertEquals(1.0d, transformed.get(0), 1.0e-12);
        assertFalse(transformed.isSet(1));
        assertEquals(3.5d, transformed.get(2), 1.0e-12);
    }

    @Test
    void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("ToDouble", "amount", "parsed");

        assertTrue(transform instanceof TransformToDouble);
    }

    @Test
    void applyShouldAcceptScientificNotation()
    {
        final ColumnName AMOUNT = ColumnName.of("AMOUNT");
        ColumnObject.Builder<String> builder = ColumnObject.builder(AMOUNT, ColumnTypes.STRING);
        builder.add("1e6");
        builder.add("1E-6");
        ColumnObject<String> source = builder.build();

        ColumnDouble transformed = new TransformToDouble(AMOUNT).apply((Column) source);

        assertEquals(1.0e6d, transformed.get(0), 1.0e-12);
        assertEquals(1.0e-6d, transformed.get(1), 1.0e-18);
    }
}
