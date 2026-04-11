package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;

public class TransformCoalesceTest
{
    @Test
    public void shouldTakeFirstNonNullAcrossThreeColumns()
    {
        final ColumnName CHOSEN = ColumnName.of("Chosen");
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName SECOND = ColumnName.of("Second");
        final ColumnName THIRD = ColumnName.of("Third");

        ColumnObject.Builder<String> firstBuilder = ColumnObject.builder(FIRST, String.class);
        firstBuilder.addNull();
        firstBuilder.add("a");
        firstBuilder.addNull();
        firstBuilder.addNull();

        ColumnObject.Builder<String> secondBuilder = ColumnObject.builder(SECOND, String.class);
        secondBuilder.add("b");
        secondBuilder.add("bb");
        secondBuilder.addNull();
        secondBuilder.addNull();

        ColumnObject.Builder<String> thirdBuilder = ColumnObject.builder(THIRD, String.class);
        thirdBuilder.add("c");
        thirdBuilder.add("cc");
        thirdBuilder.add("d");
        thirdBuilder.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), firstBuilder.build(), secondBuilder.build(),
                thirdBuilder.build());

        TableColumnar transformed = table
                .apply(new TransformCoalesce(CHOSEN, ColumnObject.Mode.AUTO, FIRST, SECOND, THIRD));

        ColumnObject<String> chosen = transformed.getString(CHOSEN);
        assertEquals("b", chosen.get(0));
        assertEquals("a", chosen.get(1));
        assertEquals("d", chosen.get(2));
        assertFalse(chosen.isSet(3));
    }

    @Test
    public void shouldUseExplicitCategoricalModeFromRegistry()
    {
        final ColumnName CHOSEN = ColumnName.of("CHOSEN");
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName SECOND = ColumnName.of("Second");
        final ColumnName THIRD = ColumnName.of("Third");

        ColumnObject.Builder<String> firstBuilder = ColumnObject.builder(FIRST, String.class);
        firstBuilder.addNull();
        firstBuilder.add("x");

        ColumnObject.Builder<String> secondBuilder = ColumnObject.builder(SECOND, String.class);
        secondBuilder.add("y");
        secondBuilder.addNull();

        ColumnObject.Builder<String> thirdBuilder = ColumnObject.builder(THIRD, String.class);
        thirdBuilder.add("z");
        thirdBuilder.add("z");

        TableColumnar table = Tables.newTable(TableName.of("t"), firstBuilder.build(), secondBuilder.build(),
                thirdBuilder.build());

        Transform transform = Transforms.registry().create("Coalesce", "Chosen", "CATEGORICAL", "First", "Second",
                "Third");
        TableColumnar transformed = table.apply(transform);

        assertTrue(transformed.get(CHOSEN) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> chosen = transformed.getCategorical(CHOSEN);
        assertEquals("y", chosen.get(0));
        assertEquals("x", chosen.get(1));
    }

    @Test
    public void shouldDefaultRegistryModeToAutoWhenOmitted()
    {
        Transform transform = Transforms.registry().create("Coalesce", "Chosen", "First", "Second", "Third");

        assertTrue(transform instanceof TransformCoalesce);
    }

    @Test
    public void shouldCoalesceBigDecimalColumns()
    {
        final ColumnName AMOUNT = ColumnName.of("Amount");
        final ColumnName FIRST = ColumnName.of("First");
        final ColumnName SECOND = ColumnName.of("Second");
        final ColumnName THIRD = ColumnName.of("Third");

        ColumnObject.Builder<BigDecimal> firstBuilder = ColumnObject.builder(FIRST, BigDecimal.class);
        firstBuilder.addNull();
        firstBuilder.add(new BigDecimal("1.25"));
        firstBuilder.addNull();

        ColumnObject.Builder<BigDecimal> secondBuilder = ColumnObject.builder(SECOND, BigDecimal.class);
        secondBuilder.add(new BigDecimal("2.50"));
        secondBuilder.addNull();
        secondBuilder.addNull();

        ColumnObject.Builder<BigDecimal> thirdBuilder = ColumnObject.builder(THIRD, BigDecimal.class);
        thirdBuilder.add(new BigDecimal("3.75"));
        thirdBuilder.add(new BigDecimal("4.00"));
        thirdBuilder.add(new BigDecimal("5.50"));

        TableColumnar table = Tables.newTable(TableName.of("t"), firstBuilder.build(), secondBuilder.build(),
                thirdBuilder.build());

        TableColumnar transformed = table
                .apply(new TransformCoalesce(AMOUNT, ColumnObject.Mode.AUTO, FIRST, SECOND, THIRD));

        ColumnObject<BigDecimal> chosen = transformed.getDecimal(AMOUNT);
        assertEquals(0, new BigDecimal("2.50").compareTo(chosen.get(0)));
        assertEquals(0, new BigDecimal("1.25").compareTo(chosen.get(1)));
        assertEquals(0, new BigDecimal("5.50").compareTo(chosen.get(2)));
    }
}
