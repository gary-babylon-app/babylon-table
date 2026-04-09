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

import app.babylon.io.DataSource;
import app.babylon.io.DataSourceProbe;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LineReaderFactoryCSV implements LineReaderFactory
{
    @Override
    public LineReader create(DataSource dataSource, Csv.Settings readSettings) throws IOException
    {
        BufferedInputStream bufferedStream = toBufferedStream(dataSource.openStream());
        DataSourceProbe probe = DataSourceProbe.of(bufferedStream, dataSource.getName());

        if (probe.isXlsx() || probe.isXls() || probe.isPdf() || probe.isZip())
        {
            throw new IllegalArgumentException();
        }

        if (readSettings instanceof Csv.Settings readSettingCSV)
        {
            Charset charset = resolveCharset(readSettingCSV, probe);
            int bomLength = resolveBomLength(readSettingCSV, probe);
            if (readSettingCSV.isFixedWidths())
            {
                return new LineReaderCSVFixedWidth(bufferedStream, readSettingCSV, charset, bomLength);
            }
            return new LineReaderCSV(bufferedStream, readSettingCSV, charset, bomLength);
        } else
        {
            throw new IllegalArgumentException();
        }
    }

    protected static BufferedInputStream toBufferedStream(InputStream instream)
    {
        if (!(instream instanceof BufferedInputStream))
        {
            return new BufferedInputStream(instream);
        }
        return (BufferedInputStream) instream;
    }

    protected static Charset resolveCharset(Csv.Settings options, DataSourceProbe probe)
    {
        if (options == null || probe == null)
        {
            return StandardCharsets.UTF_8;
        }
        if (options.isAutoDetectEncoding())
        {
            Charset detected = probe.detectedCharset();
            return detected == null ? StandardCharsets.UTF_8 : detected;
        }
        return options.hasCharset() ? options.getCharset() : StandardCharsets.UTF_8;
    }

    protected static int resolveBomLength(Csv.Settings options, DataSourceProbe probe)
    {
        if (options == null || probe == null)
        {
            return 0;
        }
        return options.isAutoDetectEncoding() ? probe.bomLengthBytes() : 0;
    }
}
