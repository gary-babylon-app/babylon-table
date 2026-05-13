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

public record ReadOptionsCsv(char separator, char quote, int[] fixedWidths, Charset charset, boolean autoDetectOptions)
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

    /**
     * Returns whether CSV input options should be detected from the source sample.
     * <p>
     * This is a compatibility alias for callers using the earlier name, when the
     * detection only covered encoding.
     *
     * @return whether automatic CSV option detection is enabled
     */
    @Deprecated
    public boolean autoDetectEncoding()
    {
        return this.autoDetectOptions;
    }

    public static final class Builder
    {
        private char separator;
        private char quote;
        private int[] fixedWidths;
        private Charset charset;
        private boolean autoDetectOptions;

        private Builder()
        {
            this.separator = ',';
            this.quote = '"';
            this.fixedWidths = null;
            this.charset = StandardCharsets.UTF_8;
            this.autoDetectOptions = true;
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

        public Builder withAutoDetectOptions(boolean autoDetectOptions)
        {
            this.autoDetectOptions = autoDetectOptions;
            return this;
        }

        /**
         * Configures automatic CSV option detection.
         * <p>
         * Kept as a compatibility alias for callers using the earlier name, when the
         * detection only covered encoding.
         *
         * @param autoDetectEncoding
         *            whether automatic CSV option detection is enabled
         * @return this builder
         */
        @Deprecated
        public Builder withAutoDetectEncoding(boolean autoDetectEncoding)
        {
            return withAutoDetectOptions(autoDetectEncoding);
        }

        public ReadOptionsCsv build()
        {
            return new ReadOptionsCsv(this.separator, this.quote, this.fixedWidths, this.charset,
                    this.autoDetectOptions);
        }
    }
}
