package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.babylon.table.ColumnCategorical;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import org.junit.jupiter.api.Test;

public class TransformToTypeTest
{
    @Test
    public void shouldCreateCategoricalColumnFromPlainStringInput()
    {
        ColumnName from = ColumnName.of("From");
        ColumnName to = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("1");
        strings.add("2");
        strings.add("1");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToType<>(Integer.class, from, Integer::parseInt, to));

        assertTrue(transformed.get(to) instanceof ColumnCategorical<?>);
        ColumnCategorical<Integer> ints = transformed.getCategorical(to, Integer.class);
        assertEquals(Integer.valueOf(1), ints.get(0));
        assertEquals(Integer.valueOf(2), ints.get(1));
        assertEquals(Integer.valueOf(1), ints.get(2));
    }

    @Test
    public void shouldUseFirstInParseModeWhenRequested()
    {
        ColumnName from = ColumnName.of("From");
        ColumnName to = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("ignore one here");
        strings.add("missing");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformToType.of(Integer.class, TransformToTypeTest::parseWordNumber,
                "From", "To", "CATEGORICAL", "FIRST_IN"));

        ColumnCategorical<Integer> ints = transformed.getCategorical(to, Integer.class);
        assertEquals(Integer.valueOf(1), ints.get(0));
        assertFalse(ints.isSet(1));
    }

    @Test
    public void shouldUseLastInParseModeWithExplicitArrayMode()
    {
        ColumnName from = ColumnName.of("From");
        ColumnName to = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("one ignore two");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformToType.of(Integer.class, TransformToTypeTest::parseWordNumber,
                "From", "To", "ARRAY", "LAST_IN"));

        ColumnObject<Integer> ints = transformed.getTyped(to, Integer.class);
        assertEquals(Integer.valueOf(2), ints.get(0));
    }

    @Test
    public void shouldUseOnlyOneInParseMode()
    {
        ColumnName from = ColumnName.of("From");
        ColumnName to = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(from, String.class);
        strings.add("ignore one here");
        strings.add("one and two");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(TransformToType.of(Integer.class, TransformToTypeTest::parseWordNumber,
                "From", "To", "CATEGORICAL", "ONLY_ONE_IN"));

        ColumnCategorical<Integer> ints = transformed.getCategorical(to, Integer.class);
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
