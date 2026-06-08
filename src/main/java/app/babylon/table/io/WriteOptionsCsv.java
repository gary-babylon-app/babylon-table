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

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.ToStringSettings;

public record WriteOptionsCsv(Charset charset, ToStringSettings toStringSettings, boolean includeHeaders,
        char separator, String lineSeparator)
{
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final char DEFAULT_SEPARATOR = ',';
    public static final String DEFAULT_LINE_SEPARATOR = "\r\n";

    public WriteOptionsCsv
    {
        charset = ArgumentCheck.nonNull(charset);
        toStringSettings = ArgumentCheck.nonNull(toStringSettings);
        lineSeparator = ArgumentCheck.nonNull(lineSeparator);
    }

    public static WriteOptionsCsv standard()
    {
        return builder().build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private Charset charset;
        private ToStringSettings toStringSettings;
        private boolean includeHeaders;
        private char separator;
        private String lineSeparator;

        private Builder()
        {
            this.charset = DEFAULT_CHARSET;
            this.toStringSettings = ToStringSettings.standard();
            this.includeHeaders = true;
            this.separator = DEFAULT_SEPARATOR;
            this.lineSeparator = DEFAULT_LINE_SEPARATOR;
        }

        public Builder withCharset(Charset charset)
        {
            this.charset = ArgumentCheck.nonNull(charset);
            return this;
        }

        public Builder withToStringSettings(ToStringSettings toStringSettings)
        {
            this.toStringSettings = ArgumentCheck.nonNull(toStringSettings);
            return this;
        }

        public Builder withIncludeHeaders(boolean includeHeaders)
        {
            this.includeHeaders = includeHeaders;
            return this;
        }

        public Builder withSeparator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Builder withLineSeparator(String lineSeparator)
        {
            this.lineSeparator = ArgumentCheck.nonNull(lineSeparator);
            return this;
        }

        public WriteOptionsCsv build()
        {
            return new WriteOptionsCsv(this.charset, this.toStringSettings, this.includeHeaders, this.separator,
                    this.lineSeparator);
        }
    }
}
