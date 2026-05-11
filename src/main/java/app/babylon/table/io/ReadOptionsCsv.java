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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.babylon.lang.ArgumentCheck;

public record ReadOptionsCsv(char separator, char quote, int[] fixedWidths, Charset charset, boolean autoDetectEncoding)
{
    public ReadOptionsCsv
    {
        fixedWidths = fixedWidths == null ? null : Arrays.copyOf(fixedWidths, fixedWidths.length);
        charset = ArgumentCheck.nonNull(charset);
    }

    public static ReadOptionsCsv standard()
    {
        return builder().build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public int[] fixedWidths()
    {
        return this.fixedWidths == null ? null : Arrays.copyOf(this.fixedWidths, this.fixedWidths.length);
    }

    boolean isFixedWidths()
    {
        return this.fixedWidths != null && this.fixedWidths.length > 0;
    }

    public static final class Builder
    {
        private char separator;
        private char quote;
        private int[] fixedWidths;
        private Charset charset;
        private boolean autoDetectEncoding;

        private Builder()
        {
            this.separator = ',';
            this.quote = '"';
            this.fixedWidths = null;
            this.charset = StandardCharsets.UTF_8;
            this.autoDetectEncoding = true;
        }

        public Builder withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Builder withQuote(char quote)
        {
            this.quote = quote;
            return this;
        }

        public Builder withFixedWidths(int[] fixedWidths)
        {
            this.fixedWidths = fixedWidths == null ? null : Arrays.copyOf(fixedWidths, fixedWidths.length);
            return this;
        }

        public Builder withCharset(Charset charset)
        {
            this.charset = ArgumentCheck.nonNull(charset);
            return this;
        }

        public Builder withAutoDetectEncoding(boolean autoDetectEncoding)
        {
            this.autoDetectEncoding = autoDetectEncoding;
            return this;
        }

        public ReadOptionsCsv build()
        {
            return new ReadOptionsCsv(this.separator, this.quote, this.fixedWidths, this.charset,
                    this.autoDetectEncoding);
        }
    }
}
