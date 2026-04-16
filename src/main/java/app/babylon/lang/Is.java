/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.lang;

import java.util.Collection;
import java.util.Map;

public class Is
{
    public static <T> boolean empty(Collection<T> x)
    {
        return (x == null || x.size() == 0);
    }

    public static <U, V> boolean empty(Map<U, V> x)
    {
        return (x == null || x.size() == 0);
    }

    public static <T> boolean empty(T[] x)
    {
        return (x == null || x.length == 0);
    }
}
