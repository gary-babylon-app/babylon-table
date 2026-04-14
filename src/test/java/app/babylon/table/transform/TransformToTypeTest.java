package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import org.junit.jupiter.api.Test;

public class TransformToTypeTest
{
    @Test
    public void shouldCreateCategoricalColumnFromPlainStringInput()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("1");
        strings.add("2");
        strings.add("1");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToType<>(Integer.class, FROM, Integer::parseInt, TO));

        assertTrue(transformed.get(TO) instanceof ColumnCategorical<?>);
        ColumnCategorical<Integer> ints = transformed.getCategorical(TO, Column.Type.of(Integer.class));
        assertEquals(Integer.valueOf(1), ints.get(0));
        assertEquals(Integer.valueOf(2), ints.get(1));
        assertEquals(Integer.valueOf(1), ints.get(2));
    }

    @Test
    public void shouldUseFirstInParseModeWhenRequested()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ignore one here");
        strings.add("missing");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformToType.of(Integer.class, TransformToTypeTest::parseWordNumber,
                "From", "To", "CATEGORICAL", "FIRST_IN"));

        ColumnCategorical<Integer> ints = transformed.getCategorical(TO, Column.Type.of(Integer.class));
        assertEquals(Integer.valueOf(1), ints.get(0));
        assertFalse(ints.isSet(1));
    }

    @Test
    public void shouldUseLastInParseModeWithExplicitArrayMode()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("one ignore two");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformToType.of(Integer.class, TransformToTypeTest::parseWordNumber,
                "From", "To", "ARRAY", "LAST_IN"));

        ColumnObject<Integer> ints = transformed.getObject(TO, Column.Type.of(Integer.class));
        assertEquals(Integer.valueOf(2), ints.get(0));
    }

    @Test
    public void shouldUseOnlyOneInParseMode()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, app.babylon.table.column.ColumnTypes.STRING);
        strings.add("ignore one here");
        strings.add("one and two");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformToType.of(Integer.class, TransformToTypeTest::parseWordNumber,
                "From", "To", "CATEGORICAL", "ONLY_ONE_IN"));

        ColumnCategorical<Integer> ints = transformed.getCategorical(TO, Column.Type.of(Integer.class));
        assertEquals(Integer.valueOf(1), ints.get(0));
        assertFalse(ints.isSet(1));
    }

    private static Integer parseWordNumber(String s)
    {
        return switch (s)
        {
            case "one" -> 1;
            case "two" -> 2;
            default -> null;
        };
    }
}
