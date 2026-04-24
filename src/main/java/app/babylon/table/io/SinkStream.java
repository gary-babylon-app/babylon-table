package app.babylon.table.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Named output stream for exported table content.
 */
public interface SinkStream
{
    String getName();

    OutputStream openStream() throws IOException;
}
