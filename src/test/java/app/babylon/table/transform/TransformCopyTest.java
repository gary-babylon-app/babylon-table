package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.dsl.ComparisonCondition;

public class TransformCopyTest
{
    @Test
    public void shouldCopyColumnToNewName()
    {
        final ColumnName SYMBOL = ColumnName.of("Symbol");
        final ColumnName SYMBOL_COPY = ColumnName.of("SymbolCopy");

        ColumnCategorical.Builder<String> builder = ColumnCategorical.builder(SYMBOL, ColumnTypes.STRING);
        builder.add("VEVE");
        builder.add("SGLN");
        builder.add("VEVE");

        TableColumnar table = Tables.newTable(TableName.of("t"), builder.build());

        TableColumnar transformed = table.apply(new TransformCopy(SYMBOL, SYMBOL_COPY));

        assertTrue(transformed.get(SYMBOL_COPY) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> copied = transformed.getCategorical(SYMBOL_COPY);
        assertEquals("VEVE", copied.get(0));
        assertEquals("SGLN", copied.get(1));
        assertEquals("VEVE", copied.get(2));
        assertEquals(copied.getCategoryCode(0), copied.getCategoryCode(2));
        assertNotSame(transformed.get(SYMBOL), copied);
    }

    @Test
    public void shouldConditionallyCopyIntoUnsetTargetValues()
    {
        final ColumnName DEBIT_CREDIT = ColumnName.of("DebitCredit");
        final ColumnName TRANSACTION_TYPE = ColumnName.of("TransactionType");
        final ColumnName CONSIDERATION = ColumnName.of("Consideration");

        ColumnObject.Builder<BigDecimal> debitCredit = ColumnObject.builderDecimal(DEBIT_CREDIT);
        debitCredit.add(new BigDecimal("10"));
        debitCredit.add(new BigDecimal("20"));
        debitCredit.add(new BigDecimal("30"));
        debitCredit.add(new BigDecimal("40"));
        debitCredit.addNull();

        ColumnCategorical.Builder<String> transactionType = ColumnCategorical.builder(TRANSACTION_TYPE,
                ColumnTypes.STRING);
        transactionType.add("Buy");
        transactionType.add("Sell");
        transactionType.add("Dividend");
        transactionType.add("Buy");
        transactionType.add("Sell");

        ColumnObject.Builder<BigDecimal> consideration = ColumnObject.builderDecimal(CONSIDERATION);
        consideration.add(new BigDecimal("99"));
        consideration.addNull();
        consideration.addNull();
        consideration.addNull();
        consideration.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), debitCredit.build(), transactionType.build(),
                consideration.build());

        TableColumnar transformed = table.apply(TransformCopy.of(DEBIT_CREDIT, CONSIDERATION,
                new ComparisonCondition(TRANSACTION_TYPE, Column.Operator.IN, "Buy", "Sell")));

        ColumnObject<BigDecimal> copied = transformed.getDecimal(CONSIDERATION);
        assertEquals(new BigDecimal("99"), copied.get(0));
        assertEquals(new BigDecimal("20"), copied.get(1));
        assertFalse(copied.isSet(2));
        assertEquals(new BigDecimal("40"), copied.get(3));
        assertFalse(copied.isSet(4));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Copy", "Symbol", "SymbolCopy");

        assertTrue(transform instanceof TransformCopy);
    }
}
