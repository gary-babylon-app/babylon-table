package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class TransformToTypeTest
{
    @Test
    public void shouldCreateCategoricalColumnFromPlainStringInput()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM);
        strings.add("1");
        strings.add("2");
        strings.add("1");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformToType<>(ColumnTypes.DECIMAL, ColumnObject.Mode.CATEGORICAL, FROM, TO));

        assertTrue(transformed.get(TO) instanceof ColumnCategorical<?>);
        ColumnCategorical<BigDecimal> decimals = transformed.getCategorical(TO, ColumnTypes.DECIMAL);
        assertEquals(new BigDecimal("1"), decimals.get(0));
        assertEquals(new BigDecimal("2"), decimals.get(1));
        assertEquals(new BigDecimal("1"), decimals.get(2));
    }

    @Test
    public void shouldUseFirstInParseModeWhenRequested()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, ColumnTypes.STRING);
        strings.add("ignore 1 here");
        strings.add("missing");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(TransformToType.of(ColumnTypes.DECIMAL, "From", "To", "CATEGORICAL", "FIRST_IN"));

        ColumnCategorical<BigDecimal> decimals = transformed.getCategorical(TO, ColumnTypes.DECIMAL);
        assertEquals(new BigDecimal("1"), decimals.get(0));
        assertFalse(decimals.isSet(1));
    }

    @Test
    public void shouldUseLastInParseModeWithExplicitArrayMode()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, ColumnTypes.STRING);
        strings.add("1 ignore 2");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(TransformToType.of(ColumnTypes.DECIMAL, "From", "To", "ARRAY", "LAST_IN"));

        ColumnObject<BigDecimal> decimals = transformed.getObject(TO, ColumnTypes.DECIMAL);
        assertEquals(new BigDecimal("2"), decimals.get(0));
    }

    @Test
    public void shouldUseOnlyOneInParseMode()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, ColumnTypes.STRING);
        strings.add("ignore 1 here");
        strings.add("1 and 2");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(TransformToType.of(ColumnTypes.DECIMAL, "From", "To", "CATEGORICAL", "ONLY_ONE_IN"));

        ColumnCategorical<BigDecimal> decimals = transformed.getCategorical(TO, ColumnTypes.DECIMAL);
        assertEquals(new BigDecimal("1"), decimals.get(0));
        assertFalse(decimals.isSet(1));
    }

    @Test
    public void shouldUseVarargsConstructorWithDefaultModeAndExactParsing()
    {
        final ColumnName FROM = ColumnName.of("From");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM);
        strings.add("1");
        strings.add("");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToType<>(ColumnTypes.DECIMAL, FROM));

        ColumnObject<BigDecimal> decimals = transformed.getObject(FROM, ColumnTypes.DECIMAL);
        assertEquals(new BigDecimal("1"), decimals.get(0));
        assertFalse(decimals.isSet(1));
    }

    @Test
    public void shouldUseVarargsConstructorWithExplicitArrayMode()
    {
        final ColumnName FROM = ColumnName.of("From");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, ColumnTypes.STRING);
        strings.add("1");
        strings.add("2");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformToType<>(ColumnTypes.DECIMAL, ColumnObject.Mode.ARRAY, FROM));

        assertFalse(transformed.get(FROM) instanceof ColumnCategorical<?>);
        ColumnObject<BigDecimal> decimals = transformed.getObject(FROM, ColumnTypes.DECIMAL);
        assertEquals(new BigDecimal("2"), decimals.get(1));
    }

    @Test
    public void shouldUseVarargsConstructorWithExplicitParseMode()
    {
        final ColumnName FROM = ColumnName.of("From");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, ColumnTypes.STRING);
        strings.add("1 ignore 2");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(
                new TransformToType<>(ColumnTypes.DECIMAL, ColumnObject.Mode.AUTO, TransformParseMode.LAST_IN, FROM));

        ColumnObject<BigDecimal> decimals = transformed.getObject(FROM, ColumnTypes.DECIMAL);
        assertEquals(new BigDecimal("2"), decimals.get(0));
    }

    @Test
    public void shouldUseSingleColumnConstructorWithExplicitMode()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, ColumnTypes.STRING);
        strings.add("1");
        strings.add("2");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformToType<>(ColumnTypes.DECIMAL, ColumnObject.Mode.ARRAY, FROM, TO));

        assertFalse(transformed.get(TO) instanceof ColumnCategorical<?>);
        assertEquals(new BigDecimal("1"), transformed.getObject(TO, ColumnTypes.DECIMAL).get(0));
        assertEquals(new BigDecimal("2"), transformed.getObject(TO, ColumnTypes.DECIMAL).get(1));
    }

    @Test
    public void shouldUseSingleColumnConstructorWithExplicitModeAndParseMode()
    {
        final ColumnName FROM = ColumnName.of("From");
        final ColumnName TO = ColumnName.of("To");

        ColumnObject.Builder<String> strings = ColumnObject.builder(FROM, ColumnTypes.STRING);
        strings.add("ignore 1 here");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table.apply(new TransformToType<>(ColumnTypes.DECIMAL,
                ColumnObject.Mode.CATEGORICAL, TransformParseMode.FIRST_IN, FROM, TO));

        assertEquals(new BigDecimal("1"), transformed.getCategorical(TO, ColumnTypes.DECIMAL).get(0));
    }

    @Test
    public void shouldReturnNullFromFactoryWhenParamsAreMissing()
    {
        assertNull(TransformToType.of(ColumnTypes.DECIMAL, "From"));
    }
}
