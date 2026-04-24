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

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.column.type.TypeWriter;

public class ToStringSettings
{

    public static final DecimalFormat STANDARD_DECIMAL_FORMAT = new DecimalFormat("#.##");

    private DateTimeFormatter dateFormatter;
    private DecimalFormat decimalFormat;
    private boolean stripTrailingZeros;
    private final Map<Column.Type, TypeWriter<?>> typeWriters;

    public ToStringSettings()
    {
        this.dateFormatter = DateTimeFormatter.ISO_DATE;
        this.decimalFormat = STANDARD_DECIMAL_FORMAT;
        this.stripTrailingZeros = true;
        this.typeWriters = new LinkedHashMap<>();
    }

    public ToStringSettings withDateFormatter(DateTimeFormatter f)
    {
        this.dateFormatter = f;
        return this;
    }

    public ToStringSettings withStripTrailingZeros(boolean b)
    {
        this.stripTrailingZeros = b;
        return this;
    }
    public ToStringSettings withDecimalFormatter(DecimalFormat f)
    {
        this.decimalFormat = f;
        return this;
    }

    public ToStringSettings withTypeWriter(Column.Type type, TypeWriter<?> writer)
    {
        this.typeWriters.put(ArgumentCheck.nonNull(type), ArgumentCheck.nonNull(writer));
        return this;
    }

    public boolean isStripTrailingZeros()
    {
        return this.stripTrailingZeros;
    }

    public DateTimeFormatter getDateFormatter(DateTimeFormatter valueIfNull)
    {
        return (this.dateFormatter == null) ? valueIfNull : this.dateFormatter;
    }

    public DecimalFormat getDecimalFormatter(DecimalFormat valueIfNull)
    {
        return (this.decimalFormat == null) ? valueIfNull : this.decimalFormat;
    }

    public Optional<TypeWriter<?>> getTypeWriter(Column.Type type)
    {
        return Optional.ofNullable(this.typeWriters.get(type));
    }

    public static ToStringSettings standard()
    {
        return new ToStringSettings();
    }
}
