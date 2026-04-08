package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnDouble;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.transform.Transform;

class TransformToDoubleTest
{
    @Test
    void applyShouldConvertPlainStringColumnAndLeaveUnparseableValuesUnset()
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        builder.add("1.25");
        builder.add("USD 12.50");
        builder.add("1 2 3");
        builder.add("abc");
        builder.addNull();
        builder.add("(1,234.50)");
        ColumnObject<String> source = builder.build();

        ColumnDouble transformed = new TransformToDouble(ColumnName.of("amount")).apply((Column) source);

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
        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(ColumnName.of("amount"), String.class);
        builder.add("10.5");
        builder.add("bad");
        builder.add("10.5");
        builder.addNull();
        ColumnObject<String> source = builder.build();

        ColumnDouble transformed = new TransformToDouble(ColumnName.of("amount")).apply((Column) source);

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
        ColumnObject.Builder<String> amountBuilder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        amountBuilder.add("1.25");
        amountBuilder.add("USD 12.50");

        ColumnObject.Builder<String> otherBuilder = ColumnObject.builder(ColumnName.of("other"), String.class);
        otherBuilder.add("x");
        otherBuilder.add("y");

        ColumnDouble.Builder existingDoubleBuilder = ColumnDouble.builder(ColumnName.of("existing"));
        existingDoubleBuilder.add(3.0d);
        existingDoubleBuilder.add(4.0d);
        ColumnDouble existingDouble = existingDoubleBuilder.build();

        TableColumnar table = Tables.newTable(TableName.of("t"), new TableDescription(""), amountBuilder.build(),
                otherBuilder.build(), existingDouble);

        TableColumnar transformed = table
                .apply(new TransformToDouble(ColumnName.of("amount"), ColumnName.of("parsed")));

        ColumnDouble parsed = transformed.getDouble(ColumnName.of("parsed"));
        assertEquals(1.25d, parsed.get(0), 1.0e-12);
        assertEquals(12.5d, parsed.get(1), 1.0e-12);

        assertEquals("x", transformed.getString(ColumnName.of("other")).get(0));
        assertSame(existingDouble, transformed.getDouble(ColumnName.of("existing")));
    }

    @Test
    void applyShouldCopyExistingDoubleColumnWhenAppliedDirectly()
    {
        ColumnDouble.Builder builder = ColumnDouble.builder(ColumnName.of("amount"));
        builder.add(1.0d);
        builder.addNull();
        builder.add(3.5d);
        ColumnDouble source = builder.build();

        ColumnDouble transformed = new TransformToDouble(ColumnName.of("amount"), ColumnName.of("renamed"))
                .apply((Column) source);

        assertNotSame(source, transformed);
        assertEquals(ColumnName.of("renamed"), transformed.getName());
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
        ColumnObject.Builder<String> builder = ColumnObject.builder(ColumnName.of("amount"), String.class);
        builder.add("1e6");
        builder.add("1E-6");
        ColumnObject<String> source = builder.build();

        ColumnDouble transformed = new TransformToDouble(ColumnName.of("amount")).apply((Column) source);

        assertEquals(1.0e6d, transformed.get(0), 1.0e-12);
        assertEquals(1.0e-6d, transformed.get(1), 1.0e-18);
    }
}
