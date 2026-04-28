package app.babylon.table.io;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.ColumnName;

/**
 * Result of header detection, including the found headers and any selected
 * subset.
 */
public class HeaderDetection
{
    private final ColumnName[] headersFound;
    private final boolean syntheticHeaders;
    private final ColumnName[] selectedHeaders;
    private final int[] selectedPositions;

    /**
     * Creates a detection with the supplied headers and no synthetic flag.
     */
    public HeaderDetection(ColumnName[] headersFound)
    {
        this(headersFound, false);
    }

    /**
     * Creates a detection with the supplied headers.
     */
    public HeaderDetection(ColumnName[] headersFound, boolean syntheticHeaders)
    {
        this(headersFound, syntheticHeaders, headersFound, identityPositions(headersFound.length));
    }

    /**
     * Creates a full detection result.
     */
    public HeaderDetection(ColumnName[] headersFound, boolean syntheticHeaders, ColumnName[] selectedHeaders,
            int[] selectedPositions)
    {
        this.headersFound = ArgumentCheck.nonNull(headersFound);
        this.syntheticHeaders = syntheticHeaders;
        this.selectedHeaders = ArgumentCheck.nonNull(selectedHeaders);
        this.selectedPositions = ArgumentCheck.nonNull(selectedPositions);
    }

    /**
     * Returns the headers found in the source.
     */
    public ColumnName[] getHeadersFound()
    {
        return this.headersFound;
    }

    /**
     * Returns the selected output headers.
     */
    public ColumnName[] getSelectedHeaders()
    {
        return this.selectedHeaders;
    }

    /**
     * Returns the source positions of the selected headers.
     */
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

    /**
     * Returns whether the headers were synthesized rather than read from the
     * source.
     */
    public boolean isSyntheticHeaders()
    {
        return this.syntheticHeaders;
    }
}
