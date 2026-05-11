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

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableException;

abstract class RowCursorLineReaderCommon implements RowCursor
{
    private final LineReader lineReader;

    protected RowCursorLineReaderCommon(LineReader lineReader)
    {
        this.lineReader = ArgumentCheck.nonNull(lineReader);
    }

    @Override
    public boolean next()
    {
        try
        {
            return this.lineReader.next();
        }
        catch (IOException e)
        {
            throw new TableException("Failed to read row.", e);
        }
    }

    @Override
    public Row current()
    {
        return this.lineReader.current();
    }

    @Override
    public void close() throws IOException
    {
        this.lineReader.close();
    }

}
