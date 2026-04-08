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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import app.babylon.table.column.ColumnName;
import app.babylon.text.Strings;

final class CsvRowFilter
{
    private static final int CACHE_LIMIT_PER_COLUMN = 512;

    final Predicate<String>[] includeFilters;
    final Predicate<String>[] excludeFilters;
    final Set<String>[] includePass;
    final Set<String>[] includeFail;
    final Set<String>[] excludePass;
    final Set<String>[] excludeFail;

    @SuppressWarnings("unchecked")
    CsvRowFilter(ReadSettingsCSV settings, ColumnName[] selectedColumns)
    {
        this.includeFilters = (settings.hasRowIncludeFilters()) ? new Predicate[selectedColumns.length] : null;
        this.excludeFilters = (settings.hasRowExcludeFilters()) ? new Predicate[selectedColumns.length] : null;
        this.includePass = (settings.hasRowIncludeFilters()) ? new Set[selectedColumns.length] : null;
        this.includeFail = (settings.hasRowIncludeFilters()) ? new Set[selectedColumns.length] : null;
        this.excludePass = (settings.hasRowExcludeFilters()) ? new Set[selectedColumns.length] : null;
        this.excludeFail = (settings.hasRowExcludeFilters()) ? new Set[selectedColumns.length] : null;
        if (settings.hasRowIncludeFilters())
        {
            for (int i = 0; i < selectedColumns.length; ++i)
            {
                this.includeFilters[i] = settings.getRowIncludeFilter(selectedColumns[i]);
                if (this.includeFilters[i] != null)
                {
                    this.includePass[i] = new HashSet<>();
                    this.includeFail[i] = new HashSet<>();
                }
            }
        }
        if (settings.hasRowExcludeFilters())
        {
            for (int i = 0; i < selectedColumns.length; ++i)
            {
                this.excludeFilters[i] = settings.getRowExcludeFilter(selectedColumns[i]);
                if (this.excludeFilters[i] != null)
                {
                    this.excludePass[i] = new HashSet<>();
                    this.excludeFail[i] = new HashSet<>();
                }
            }
        }
    }

    boolean isInclude(List<String> row)
    {
        if (includeFilters == null)
        {
            return true;
        }
        if (row.size() != includeFilters.length)
        {
            return false;
        }

        for (int i = 0; i < row.size(); ++i)
        {
            if (includeFilters[i] != null)
            {
                String s = row.get(i);
                if (Strings.isEmpty(s))
                {
                    return false;
                }
                if (this.includePass[i].contains(s))
                {
                    continue;
                }
                if (this.includeFail[i].contains(s))
                {
                    return false;
                }
                if (includeFilters[i].test(s))
                {
                    addToCache(this.includePass[i], s);
                    continue;
                }
                addToCache(this.includeFail[i], s);
                return false;
            }
        }
        return true;
    }

    boolean isInclude(String[] row)
    {
        if (includeFilters == null)
        {
            return true;
        }
        if (row.length != includeFilters.length)
        {
            return false;
        }

        for (int i = 0; i < row.length; ++i)
        {
            if (includeFilters[i] != null)
            {
                String s = row[i];
                if (Strings.isEmpty(s))
                {
                    return false;
                }
                if (this.includePass[i].contains(s))
                {
                    continue;
                }
                if (this.includeFail[i].contains(s))
                {
                    return false;
                }
                if (includeFilters[i].test(s))
                {
                    addToCache(this.includePass[i], s);
                    continue;
                }
                addToCache(this.includeFail[i], s);
                return false;
            }
        }
        return true;
    }

    boolean isExclude(List<String> row)
    {
        if (excludeFilters == null)
        {
            return false;
        }
        if (row.size() != excludeFilters.length)
        {
            return false;
        }

        for (int i = 0; i < row.size(); ++i)
        {
            if (excludeFilters[i] != null)
            {
                String s = row.get(i);
                if (Strings.isEmpty(s))
                {
                    continue;
                }
                if (this.excludePass[i].contains(s))
                {
                    return true;
                }
                if (this.excludeFail[i].contains(s))
                {
                    continue;
                }
                if (excludeFilters[i].test(s))
                {
                    addToCache(this.excludePass[i], s);
                    return true;
                }
                addToCache(this.excludeFail[i], s);
            }
        }
        return false;
    }

    boolean isExclude(String[] row)
    {
        if (excludeFilters == null)
        {
            return false;
        }
        if (row.length != excludeFilters.length)
        {
            return false;
        }

        for (int i = 0; i < row.length; ++i)
        {
            if (excludeFilters[i] != null)
            {
                String s = row[i];
                if (Strings.isEmpty(s))
                {
                    continue;
                }
                if (this.excludePass[i].contains(s))
                {
                    return true;
                }
                if (this.excludeFail[i].contains(s))
                {
                    continue;
                }
                if (excludeFilters[i].test(s))
                {
                    addToCache(this.excludePass[i], s);
                    return true;
                }
                addToCache(this.excludeFail[i], s);
            }
        }
        return false;
    }

    private static void addToCache(Set<String> cache, String value)
    {
        if (cache != null && cache.size() < CACHE_LIMIT_PER_COLUMN)
        {
            cache.add(value);
        }
    }
}
