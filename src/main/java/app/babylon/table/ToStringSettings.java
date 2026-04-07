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

public class ToStringSettings 
{
    
    public static final DecimalFormat STANDARD_DECIMAL_FORMAT = new DecimalFormat("#.##");
    
    private DateTimeFormatter dateFormatter;
    private DecimalFormat decimalFormat;
    private boolean stripTrailingZeros;
    
    public ToStringSettings()
    {
        this.dateFormatter = DateTimeFormatter.ISO_DATE;
        this.decimalFormat = STANDARD_DECIMAL_FORMAT;
        this.stripTrailingZeros = true;
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
    
    public boolean isStripTrailingZeros()
    {
        return this.stripTrailingZeros;
    }
    
    public DateTimeFormatter getDateFormatter(DateTimeFormatter valueIfNull)
    {
        return (this.dateFormatter==null)?valueIfNull:this.dateFormatter;
    }
    
    public DecimalFormat getDecimalFormatter(DecimalFormat valueIfNull)
    {
        return (this.decimalFormat==null)?valueIfNull:this.decimalFormat;
    }
    
    public static ToStringSettings standard()
    {
        return new ToStringSettings();
    }
}
