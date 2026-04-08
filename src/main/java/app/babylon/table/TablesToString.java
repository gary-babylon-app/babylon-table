/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

public class TablesToString
{

    private static final int MAX_NUMBER_COLUMNS = 8;
    private static final int MAX_NUMBER_ROWS = 24;
    private static final char SPACE = ' ';

    public static String printSmallTable(TableColumnar table)
    {
        return printSmallTable(table, ToStringSettings.standard());
    }
    public static String printSmallTable(TableColumnar table, ToStringSettings settings)
    {
        if (table.getColumnCount() <= MAX_NUMBER_COLUMNS && table.getRowCount() <= MAX_NUMBER_ROWS)
        {
            return printFullTable(table);
        }

        Column[] columns = getColumnsToPrint(table, MAX_NUMBER_COLUMNS);

        int[] widths = computeWidthsSmallTable(columns, settings);

        StringBuilder builder = new StringBuilder();
        builder.append(table.getName()).append("\n");
        builder.append(table.getDescription()).append("\n");

        boolean padMiddle = table.getColumnCount() > columns.length;

        plusHyphenLine(columns, widths, builder, padMiddle);
        headerValues(columns, widths, builder, padMiddle);
        plusHyphenLine(columns, widths, builder, padMiddle);

        int maxNumberRows = Math.min(MAX_NUMBER_ROWS, table.getRowCount());
        for (int j = 0; j < maxNumberRows / 2 + 1; ++j)
        {
            rowValues(settings, j, columns, widths, builder, padMiddle);
        }

        boolean padRows = table.getRowCount() > MAX_NUMBER_ROWS;

        if (padRows)
        {
            builder.append("...\n");
            builder.append("...\n");
            builder.append("...\n");
        }

        for (int j = Math.max(maxNumberRows / 2 + 1, table.getRowCount() - maxNumberRows / 2 + 1); j < table
                .getRowCount(); ++j)
        {
            rowValues(settings, j, columns, widths, builder, padMiddle);
        }

        plusHyphenLine(columns, widths, builder, padMiddle);

        return builder.toString();
    }

    private static Column[] getColumnsToPrint(TableColumnar table, int MAX_NUMBER_COLUMNS)
    {
        int numberofColumns = Math.min(MAX_NUMBER_COLUMNS, table.getColumnCount());

        Column[] columns = table.getColumns();
        Column[] columns2Print = new Column[numberofColumns];
        for (int i = 0; i < numberofColumns / 2; ++i)
        {
            columns2Print[i] = columns[i];
        }
        for (int i = numberofColumns / 2; i < numberofColumns; ++i)
        {
            int colIndex = table.getColumnCount() - numberofColumns + i;
            columns2Print[i] = columns[colIndex];
        }
        return columns2Print;
    }

    private static int[] computeWidthsSmallTable(Column[] columns, ToStringSettings settings)
    {
        int[] widths = new int[columns.length];

        for (int i = 0; i < columns.length; ++i)
        {
            Column column = columns[i];
            widths[i] = column.getName().getValue().length();
            int maxNumberRows = Math.min(MAX_NUMBER_ROWS, column.size());
            for (int j = 0; j < maxNumberRows / 2 + 1; ++j)
            {
                widths[i] = Math.max(column.toString(j, settings).length(), widths[i]);
            }
            for (int j = Math.max(maxNumberRows / 2 + 1, column.size() - maxNumberRows / 2 + 1); j < column.size(); ++j)
            {
                widths[i] = Math.max(column.toString(j, settings).length(), widths[i]);
            }
        }
        return widths;
    }
    private static void headerValues(Column[] columns, int[] widths, StringBuilder builder, boolean padMiddle)
    {
        builder.append("|");
        for (int i = 0; i < columns.length / 2; ++i)
        {
            ColumnName columnName = columns[i].getName();
            String v = padValue(i, columnName.getValue(), widths[i]);
            builder.append(v).append("|");
        }
        if (padMiddle)
        {
            builder.append(" ... |");
        }
        for (int i = columns.length / 2; i < columns.length; ++i)
        {
            ColumnName columnName = columns[i].getName();
            String v = padValue(i, columnName.getValue(), widths[i]);
            builder.append(v).append("|");
        }
        builder.append("\n");
    }

    private static void rowValues(ToStringSettings settings, int j, Column[] columns, int[] widths,
            StringBuilder builder, boolean padMiddle)
    {
        builder.append("|");
        for (int i = 0; i < columns.length / 2; ++i)
        {
            String s = columns[i].toString(j, settings);
            String v = padValue(i, s, widths[i]);
            builder.append(v).append("|");
        }
        if (padMiddle)
        {
            builder.append(" ... |");
        }
        for (int i = columns.length / 2; i < columns.length; ++i)
        {
            String s = columns[i].toString(j, settings);
            String v = padValue(i, s, widths[i]);
            builder.append(v).append("|");
        }
        builder.append("\n");
    }
    private static void plusHyphenLine(Column[] columns, int[] widths, StringBuilder builder, boolean padMiddle)
    {
        for (int i = 0; i < columns.length / 2; ++i)
        {
            builder.append("+").append(Strings.rightPad("", widths[i], '-'));
        }

        if (padMiddle)
        {
            builder.append("+ ... ");
        }

        for (int i = columns.length / 2; i < columns.length; ++i)
        {
            builder.append("+").append(Strings.rightPad("", widths[i], '-'));
        }

        builder.append("+\n");
    }

    private static String padValue(int columnIndex, String v, int width)
    {
        if (columnIndex == 0)
        {
            return Strings.rightPad(v, width, SPACE);
        } else
        {
            return Strings.leftPad(v, width, SPACE);
        }
    }

    public static String printFullTable(TableColumnar table)
    {
        return printFullTable(table, new ToStringSettings(), table.getRowCount());
    }

    public static String printFullTable(TableColumnar table, ToStringSettings settings, int numRows)
    {
        Column[] columns = table.getColumns();
        int[] widths = new int[columns.length];

        for (int i = 0; i < columns.length; ++i)
        {
            Column column = columns[i];
            widths[i] = column.getName().getValue().length();
            for (int j = 0; j < column.size(); ++j)
            {
                widths[i] = Math.max(column.toString(j, settings).length(), widths[i]);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append(table.getName()).append("\n");
        builder.append(table.getDescription()).append("\n");

        for (int i = 0; i < columns.length; ++i)
        {
            builder.append("+").append(Strings.rightPad("", widths[i], '-'));
        }
        builder.append("+\n");

        for (int i = 0; i < columns.length; ++i)
        {
            ColumnName columnName = columns[i].getName();
            if (i == 0)
            {
                builder.append("|").append(Strings.rightPad(columnName.getValue(), widths[i], SPACE));
            } else
            {
                builder.append("|").append(Strings.leftPad(columnName.getValue(), widths[i], SPACE));
            }
        }
        builder.append("|\n");

        for (int i = 0; i < columns.length; ++i)
        {
            builder.append("+").append(Strings.rightPad("", widths[i], '-'));
        }
        builder.append("+\n");

        int rowCount = Math.min(numRows, table.getRowCount());
        for (int j = 0; j < rowCount; ++j)
        {
            for (int i = 0; i < columns.length; ++i)
            {
                Column column = columns[i];
                if (i == 0)
                {
                    builder.append("|").append(Strings.rightPad(column.toString(j, settings), widths[i], SPACE));
                } else
                {
                    builder.append("|").append(Strings.leftPad(column.toString(j, settings), widths[i], SPACE));
                }
            }
            builder.append("|\n");
        }
        for (int i = 0; i < columns.length; ++i)
        {
            builder.append("+").append(Strings.rightPad("", widths[i], '-'));
        }
        builder.append("+\n");
        return builder.toString();
    }
}
