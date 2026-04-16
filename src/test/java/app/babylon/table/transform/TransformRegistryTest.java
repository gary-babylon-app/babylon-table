package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

class TransformRegistryTest
{
    @Test
    void shouldRegisterCreateAndApplyTransforms()
    {
        final ColumnName CODE = ColumnName.of("Code");
        final ColumnName OUT = ColumnName.of("Out");

        TransformRegistry registry = TransformRegistry.builder()
                .register("Copy", params -> new TransformCopy(ColumnName.of(params[0]), ColumnName.of(params[1])))
                .build();

        assertTrue(registry.create("Copy", "Code", "Out") instanceof TransformCopy);
        assertEquals(null, registry.create("Missing", "Code"));
        assertEquals(null, registry.create(" ", "Code"));

        ColumnObject.Builder<String> code = ColumnObject.builder(CODE, app.babylon.table.column.ColumnTypes.STRING);
        code.add("A");
        TableColumnar table = Tables.newTable(TableName.of("t"), code.build());

        TableColumnar transformSet = Tables.newTable(TableName.of("transforms"),
                constantColumn(TransformSetSchema.TRANSFORM, "Copy"), constantColumn(TransformSetSchema.PARAM1, "Code"),
                constantColumn(TransformSetSchema.PARAM2, "Out"), emptyColumn(TransformSetSchema.PARAM3),
                emptyColumn(TransformSetSchema.PARAM4), emptyColumn(TransformSetSchema.PARAM5));

        TableColumnar transformed = registry.apply(table, transformSet);
        assertEquals("A", transformed.getString(OUT).get(0));
    }

    @Test
    void toBuilderShouldCarryRegistrationsForward()
    {
        TransformRegistry base = TransformRegistry.builder()
                .register("Copy", params -> new TransformCopy(ColumnName.of(params[0]), ColumnName.of(params[1])))
                .build();

        TransformRegistry copy = base.toBuilder().registerAll(base).build();

        assertTrue(copy.create("Copy", "Code", "Out") instanceof TransformCopy);
    }

    private static ColumnObject<String> constantColumn(ColumnName name, String value)
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(name, app.babylon.table.column.ColumnTypes.STRING);
        builder.add(value);
        return builder.build();
    }

    private static ColumnObject<String> emptyColumn(ColumnName name)
    {
        ColumnObject.Builder<String> builder = ColumnObject.builder(name, app.babylon.table.column.ColumnTypes.STRING);
        builder.addNull();
        return builder.build();
    }
}
