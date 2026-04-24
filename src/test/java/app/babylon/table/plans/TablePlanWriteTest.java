package app.babylon.table.plans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.io.SinkStream;

class TablePlanWriteTest
{
    private static final ColumnName CATEGORY = ColumnName.of("Category");
    private static final ColumnName AMOUNT = ColumnName.of("Amount");

    @Test
    void shouldWriteCsvUsingSelectedColumnsAndHeaders()
    {
        TableColumnar table = sampleTable();
        InMemorySink sink = new InMemorySink("cashflows.csv");

        new TablePlanWrite().withSink(sink).withSelectedColumns(CATEGORY, AMOUNT).execute(table);

        assertEquals("""
                Category,Amount
                Pay,1000000
                Receive,1250000.5
                """, sink.getText());
    }

    @Test
    void shouldWriteCsvWithoutHeaders()
    {
        TableColumnar table = sampleTable();
        InMemorySink sink = new InMemorySink("cashflows.csv");

        new TablePlanWrite().withSink(sink).withIncludeHeaders(false).withSelectedColumns(AMOUNT).execute(table);

        assertEquals("""
                1000000
                1250000.5
                """, sink.getText());
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

    private static final class InMemorySink implements SinkStream
    {
        private final String name;
        private final ByteArrayOutputStream outputStream;

        private InMemorySink(String name)
        {
            this.name = name;
            this.outputStream = new ByteArrayOutputStream();
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public OutputStream openStream() throws IOException
        {
            this.outputStream.reset();
            return this.outputStream;
        }

        private String getText()
        {
            return this.outputStream.toString(StandardCharsets.UTF_8);
        }
    }
}
