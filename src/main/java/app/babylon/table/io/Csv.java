/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.io;

import java.io.IOException;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableException;

public class Csv
{
    public static TableColumnar read(DataSource ds, ReadSettingsCSV options)
    {
        HeaderStrategy headerStrategy = options == null ? null : options.getHeaderStrategy();
        return read(ds, options, headerStrategy, RowConsumerTableBuilding.factory());
    }

    public static TableColumnar read(DataSource ds, ReadSettingsCSV options, HeaderStrategy headerStrategy)
    {
        return read(ds, options, headerStrategy, RowConsumerTableBuilding.factory());
    }

    public static <T> T read(
            DataSource ds,
            ReadSettingsCSV options,
            RowConsumerFactory<T> rowConsumerFactory)
    {
        HeaderStrategy headerStrategy = options == null ? null : options.getHeaderStrategy();
        return read(ds, options, headerStrategy, rowConsumerFactory);
    }

    public static <T> T read(
            DataSource ds,
            ReadSettingsCSV options,
            HeaderStrategy headerStrategy,
            RowConsumerFactory<T> rowConsumerFactory)
    {
        if (options == null)
        {
            options = new ReadSettingsCSV();
        }
        if (headerStrategy == null)
        {
            headerStrategy = new HeaderStrategyAuto();
        }
        if (rowConsumerFactory == null)
        {
            throw new IllegalArgumentException("rowConsumerFactory must not be null");
        }
        LineReaderFactory lineReaderFactory = options.getLineReaderFactory();
        if (lineReaderFactory == null)
        {
            lineReaderFactory = new LineReaderFactoryCSV();
        }
        try (LineReader lineReader = lineReaderFactory.create(ds, options))
        {
            RowStreamMarkable parsedRowStream = new RowStreamBuffered(lineReader);

            HeaderDetection headerDetection = headerStrategy.detect(parsedRowStream, options);

            RowConsumerResult<T> rowConsumer = rowConsumerFactory.create(options, headerDetection);
            RowProjected projectedRow = createRowProjected(options, headerDetection);

            parsedRowStream.reset();
            while (parsedRowStream.next())
            {
                rowConsumer.accept(projectedRow.with(parsedRowStream.current()));
            }
            return rowConsumer.buildResult(ds);
        }
        catch (TableException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new TableException("Failed to read table from data source '" + ds.getName() + "'.", e);
        }
    }

    private static RowProjected createRowProjected(ReadSettingsCSV options, HeaderDetection headerDetection)
    {
        RowProjected projectedRow = options.isStripping()
                ? new RowProjectedStripped(headerDetection.getSelectedPositions())
                        : new ProjectedRow(headerDetection.getSelectedPositions());
        return projectedRow;
    }
}
