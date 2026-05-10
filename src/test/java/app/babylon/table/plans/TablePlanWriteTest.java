package app.babylon.table.plans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.io.TableSink;
import app.babylon.table.io.TableSinkCsv;

class TablePlanWriteTest
{
    private static final ColumnName CATEGORY = ColumnName.of("Category");
    private static final ColumnName AMOUNT = ColumnName.of("Amount");

    @Test
    void shouldWriteCsvUsingSelectedColumnsAndHeaders()
    {
        TableColumnar table = sampleTable();
        StringWriter writer = new StringWriter();
        TableSinkCsv sink = TableSinkCsv.toWriter("cashflows.csv", writer).build();

        new TablePlanWrite().withSink(sink).withSelectedColumns(CATEGORY, AMOUNT).execute(table);

        assertEquals("""
                Category,Amount\r
                Pay,1000000\r
                Receive,1250000.5\r
                """, writer.toString());
    }

    @Test
    void shouldExposeConfiguredSink()
    {
        StringWriter writer = new StringWriter();
        TableSinkCsv sink = TableSinkCsv.toWriter("cashflows.csv", writer).build();

        TablePlanWrite plan = new TablePlanWrite().withSink(sink);

        assertSame(sink, plan.getSink());
    }

    @Test
    void shouldDelegateSelectedTableToSink()
    {
        CapturingSink sink = new CapturingSink();

        new TablePlanWrite().withSink(sink).withSelectedColumns(AMOUNT).execute(sampleTable());

        assertEquals(1, sink.table.getColumnCount());
        assertEquals(AMOUNT, sink.table.getColumnNames()[0]);
    }

    private static TableColumnar sampleTable()
    {
        ColumnObject.Builder<String> categories = ColumnObject.builder(CATEGORY);
        categories.add("Pay");
        categories.add("Receive");

        ColumnObject.Builder<BigDecimal> amounts = ColumnObject.builderDecimal(AMOUNT);
        amounts.add(new BigDecimal("1000000"));
        amounts.add(new BigDecimal("1250000.50"));

        return Tables.newTable(TableName.of("Cashflows"), new TableDescription("Cashflow rows"), categories.build(),
                amounts.build());
    }

    private static final class CapturingSink implements TableSink
    {
        private TableColumnar table;

        @Override
        public String getName()
        {
            return "capturing";
        }

        @Override
        public void write(TableColumnar table) throws IOException
        {
            this.table = table;
        }
    }
}
