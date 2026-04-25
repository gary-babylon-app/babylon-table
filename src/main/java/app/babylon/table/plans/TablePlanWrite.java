package app.babylon.table.plans;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;
import app.babylon.table.ToStringSettings;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.io.SinkStream;
import app.babylon.text.Strings;

/**
 * Builder-style plan for exporting a {@link TableColumnar} to a configured
 * format and destination.
 */
public class TablePlanWrite
{
    private static final char DEFAULT_SEPARATOR = ',';
    private static final String DEFAULT_LINE_SEPARATOR = "\n";
    private static final char DOUBLE_QUOTE = '"';
    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';

    private final Set<ColumnName> selectedColumns;
    private SinkStream sink;
    private ToStringSettings toStringSettings;
    private boolean includeHeaders;
    private char separator;
    private String lineSeparator;
    private Charset charset;

    public TablePlanWrite()
    {
        this.selectedColumns = new LinkedHashSet<>();
        this.sink = null;
        this.toStringSettings = ToStringSettings.standard();
        this.includeHeaders = true;
        this.separator = DEFAULT_SEPARATOR;
        this.lineSeparator = DEFAULT_LINE_SEPARATOR;
        this.charset = StandardCharsets.UTF_8;
    }

    public TablePlanWrite withSink(SinkStream sink)
    {
        this.sink = ArgumentCheck.nonNull(sink);
        return this;
    }

    public SinkStream getSink()
    {
        return this.sink;
    }

    public TablePlanWrite withSelectedColumn(ColumnName columnName)
    {
        this.selectedColumns.add(ArgumentCheck.nonNull(columnName));
        return this;
    }

    public TablePlanWrite withSelectedColumns(ColumnName... columnNames)
    {
        if (columnNames != null)
        {
            this.selectedColumns.addAll(Arrays.asList(columnNames));
        }
        return this;
    }

    public TablePlanWrite withSelectedColumns(Collection<ColumnName> columnNames)
    {
        if (columnNames != null)
        {
            this.selectedColumns.addAll(columnNames);
        }
        return this;
    }

    public ColumnName[] getSelectedColumns()
    {
        return this.selectedColumns.toArray(new ColumnName[0]);
    }

    public TablePlanWrite withToStringSettings(ToStringSettings toStringSettings)
    {
        this.toStringSettings = ArgumentCheck.nonNull(toStringSettings);
        return this;
    }

    public ToStringSettings getToStringSettings()
    {
        return this.toStringSettings;
    }

    public TablePlanWrite withIncludeHeaders(boolean includeHeaders)
    {
        this.includeHeaders = includeHeaders;
        return this;
    }

    public boolean isIncludeHeaders()
    {
        return this.includeHeaders;
    }

    public TablePlanWrite withSeparator(char separator)
    {
        this.separator = separator;
        return this;
    }

    public char getSeparator()
    {
        return this.separator;
    }

    public TablePlanWrite withLineSeparator(String lineSeparator)
    {
        this.lineSeparator = ArgumentCheck.nonNull(lineSeparator);
        return this;
    }

    public String getLineSeparator()
    {
        return this.lineSeparator;
    }

    public TablePlanWrite withCharset(Charset charset)
    {
        this.charset = ArgumentCheck.nonNull(charset);
        return this;
    }

    public Charset getCharset()
    {
        return this.charset;
    }

    public void execute(TableColumnar table)
    {
        TableColumnar checkedTable = ArgumentCheck.nonNull(table);
        SinkStream checkedSink = ArgumentCheck.nonNull(this.sink, "sink");

        TableColumnar selectedTable = selectColumns(checkedTable);
        try (OutputStream outputStream = checkedSink.openStream())
        {
            writeCsv(selectedTable, outputStream);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to write table to '" + checkedSink.getName() + "'.", e);
        }
    }

    private TableColumnar selectColumns(TableColumnar table)
    {
        return this.selectedColumns.isEmpty() ? table : table.select(getSelectedColumns());
    }

    private void writeCsv(TableColumnar table, OutputStream outputStream) throws IOException
    {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, this.charset)))
        {
            if (this.includeHeaders)
            {
                writeHeader(table, writer);
            }
            for (int rowIndex = 0; rowIndex < table.getRowCount(); ++rowIndex)
            {
                writeRow(table, writer, rowIndex);
            }
            writer.flush();
        }
    }

    private void writeHeader(TableColumnar table, BufferedWriter writer) throws IOException
    {
        ColumnName[] columnNames = table.getColumnNames();
        for (int i = 0; i < columnNames.length; ++i)
        {
            if (i > 0)
            {
                writer.write(this.separator);
            }
            writeEscaped(columnNames[i].toString(), writer);
        }
        writer.write(this.lineSeparator);
    }

    private void writeRow(TableColumnar table, BufferedWriter writer, int rowIndex) throws IOException
    {
        StringBuilder cell = new StringBuilder();
        int i = 0;
        for (Column column : table.columns())
        {
            if (i > 0)
            {
                writer.write(this.separator);
            }
            cell.setLength(0);
            column.appendTo(rowIndex, cell, this.toStringSettings);
            writeEscaped(cell, writer);
            ++i;
        }
        writer.write(this.lineSeparator);
    }

    private void writeEscaped(CharSequence value, BufferedWriter writer) throws IOException
    {
        CharSequence text = value == null ? "" : value;
        boolean mustQuote = Strings.indexOfAny(text, this.separator, DOUBLE_QUOTE, LINE_FEED, CARRIAGE_RETURN) >= 0;
        if (!mustQuote)
        {
            writer.append(text);
            return;
        }
        writer.write(DOUBLE_QUOTE);
        for (int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            if (c == DOUBLE_QUOTE)
            {
                writer.write(DOUBLE_QUOTE);
            }
            writer.write(c);
        }
        writer.write(DOUBLE_QUOTE);
    }
}
