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

import java.util.Objects;

public class HeaderDetection
{
    private final String[] headersFound;
    private final boolean syntheticHeaders;
    private final String[] selectedHeaders;
    private final int[] selectedPositions;

    public HeaderDetection(String[] headersFound)
    {
        this(headersFound, false);
    }

    public HeaderDetection(String[] headersFound, boolean syntheticHeaders)
    {
        this(headersFound, syntheticHeaders, headersFound, identityPositions(headersFound.length));
    }

    public HeaderDetection(
        String[] headersFound,
        boolean syntheticHeaders,
        String[] selectedHeaders,
        int[] selectedPositions)
    {
        this.headersFound = Objects.requireNonNull(headersFound);
        this.syntheticHeaders = syntheticHeaders;
        this.selectedHeaders = Objects.requireNonNull(selectedHeaders);
        this.selectedPositions = Objects.requireNonNull(selectedPositions);
    }

    public String[] getHeadersFound()
    {
        return this.headersFound;
    }

    public String[] getSelectedHeaders()
    {
        return this.selectedHeaders;
    }

    public int[] getSelectedPositions()
    {
        return this.selectedPositions;
    }

    private static int[] identityPositions(int size)
    {
        int[] positions = new int[size];
        for (int i = 0; i < size; ++i)
        {
            positions[i] = i;
        }
        return positions;
    }

    public boolean isSyntheticHeaders()
    {
        return this.syntheticHeaders;
    }
}
